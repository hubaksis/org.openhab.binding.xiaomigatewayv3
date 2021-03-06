package org.openhab.binding.xiaomigatewayv3.internal.miio;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants;
import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.CloudConnector;
import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.MiCloudException;


/**
 * The {@link MiIoAsyncCommunication} is responsible for communications with the Mi IO devices
 *
 * @author Marcel Verpaalen - Initial contribution
 */
@NonNullByDefault
public class MiIoAsyncCommunication {

    private static final int MSG_BUFFER_SIZE = 2048;

    private final Logger logger = LoggerFactory.getLogger(MiIoAsyncCommunication.class);

    private final String ip;
    private final byte[] token;
    private byte[] deviceId;
    private @Nullable DatagramSocket socket;

    private List<MiIoMessageListener> listeners = new CopyOnWriteArrayList<>();

    private AtomicInteger id = new AtomicInteger(-1);
    private int timeDelta;
    private int timeStamp;
    private final JsonParser parser;
    private @Nullable MessageSenderThread senderThread;
    private boolean connected;
    private ThingStatusDetail status = ThingStatusDetail.NONE;
    private int errorCounter;
    private int timeout;
    private boolean needPing = true;
    private static final int MAX_ERRORS = 3;
    private static final int MAX_ID = 15000;
    private final CloudConnector cloudConnector;

    private ConcurrentLinkedQueue<MiIoSendCommand> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();

    public MiIoAsyncCommunication(String ip, byte[] token, byte[] did, int id, int timeout,
            CloudConnector cloudConnector) {
        this.ip = ip;
        this.token = token;
        this.deviceId = did;
        this.timeout = timeout;
        this.cloudConnector = cloudConnector;
        setId(id);
        parser = new JsonParser();
        startReceiver();
    }

    protected List<MiIoMessageListener> getListeners() {
        return listeners;
    }

    /**
     * Registers a {@link MiIoMessageListener} to be called back, when data is received.
     * If no {@link MessageSenderThread} exists, when the method is called, it is being set up.
     *
     * @param listener {@link MiIoMessageListener} to be called back
     */
    public synchronized void registerListener(MiIoMessageListener listener) {
        needPing = true;
        startReceiver();
        if (!getListeners().contains(listener)) {
            logger.trace("Adding socket listener {}", listener);
            getListeners().add(listener);
        }
    }

    /**
     * Unregisters a {@link MiIoMessageListener}. If there are no listeners left,
     * the {@link MessageSenderThread} is being closed.
     *
     * @param listener {@link MiIoMessageListener} to be unregistered
     */
    public synchronized void unregisterListener(MiIoMessageListener listener) {
        getListeners().remove(listener);
        if (getListeners().isEmpty()) {
            concurrentLinkedQueue.clear();
            close();
        }
    }

    public int queueCommand(MiIoCommand command, String cloudServer) throws MiIoCryptoException, IOException {
        return queueCommand(command, "[]", cloudServer);
    }

    public int queueCommand(MiIoCommand command, String params, String cloudServer)
            throws MiIoCryptoException, IOException {
        return queueCommand(command.getCommand(), params, cloudServer);
    }

    public int queueCommand(String command, String params, String cloudServer)
            throws MiIoCryptoException, IOException, JsonSyntaxException {
        try {
            JsonObject fullCommand = new JsonObject();
            int cmdId = id.incrementAndGet();
            if (cmdId > MAX_ID) {
                id.set(0);
            }
            fullCommand.addProperty("id", cmdId);
            fullCommand.addProperty("method", command);
            fullCommand.add("params", parser.parse(params));
            MiIoSendCommand sendCmd = new MiIoSendCommand(cmdId, MiIoCommand.getCommand(command), fullCommand,
                    cloudServer);
            concurrentLinkedQueue.add(sendCmd);
            if (logger.isDebugEnabled()) {
                // Obfuscate part of the token to allow sharing of the logfiles
                String tokenText = Utils.obfuscateToken(Utils.getHex(token));
                logger.debug("Command added to Queue {} -> {} (Device: {} token: {} Queue: {}).{}{}",
                        fullCommand.toString(), ip, Utils.getHex(deviceId), tokenText, concurrentLinkedQueue.size(),
                        cloudServer.isBlank() ? "" : " Send via cloudserver: ", cloudServer);
            }
            if (needPing && cloudServer.isBlank()) {
                sendPing(ip);
            }
            return cmdId;
        } catch (JsonSyntaxException e) {
            logger.warn("Send command '{}' with parameters {} -> {} (Device: {}) gave error {}", command, params, ip,
                    Utils.getHex(deviceId), e.getMessage());
            throw e;
        }
    }

    MiIoSendCommand sendMiIoSendCommand(MiIoSendCommand miIoSendCommand) {
        String errorMsg = "Unknown Error while sending command";
        String decryptedResponse = "";
        try {
            if (miIoSendCommand.getCloudServer().isBlank()) {
                decryptedResponse = sendCommand(miIoSendCommand.getCommandString(), token, ip, deviceId);
            } else {
                decryptedResponse = cloudConnector.sendRPCCommand(Utils.getHex(deviceId),
                        miIoSendCommand.getCloudServer(), miIoSendCommand);
                logger.debug("Command {} send via cloudserver {}", miIoSendCommand.getCommandString(),
                        miIoSendCommand.getCloudServer());
                updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
            }
            // hack due to avoid invalid json errors from some misbehaving device firmwares
            decryptedResponse = decryptedResponse.replace(",,", ",");
            JsonElement response;
            response = parser.parse(decryptedResponse);
            if (!response.isJsonObject()) {
                errorMsg = "Received message is not a JSON object ";
            } else {
                needPing = false;
                logger.trace("Received  JSON message {}", response.toString());
                JsonObject resJson = response.getAsJsonObject();
                if (resJson.has("id")) {
                    int id = resJson.get("id").getAsInt();
                    if (id == miIoSendCommand.getId()) {
                        miIoSendCommand.setResponse(response.getAsJsonObject());
                        return miIoSendCommand;
                    } else {
                        if (id < miIoSendCommand.getId()) {
                            errorMsg = String.format(
                                    "Received message out of sync, extend timeout time. Expected id: %d, received id: %d",
                                    miIoSendCommand.getId(), id);
                        } else {
                            errorMsg = String.format("Received message out of sync. Expected id: %d, received id: %d",
                                    miIoSendCommand.getId(), id);
                        }
                    }
                } else {
                    errorMsg = "Received message is without id";
                }

            }
            logger.debug("{}: {}", errorMsg, decryptedResponse);
        } catch (MiIoCryptoException | IOException e) {
            logger.debug("Send command '{}'  -> {} (Device: {}) gave error {}", miIoSendCommand.getCommandString(), ip,
                    Utils.getHex(deviceId), e.getMessage());
            errorMsg = e.getMessage();
        } catch (JsonSyntaxException e) {
            logger.warn("Could not parse '{}' <- {} (Device: {}) gave error {}", decryptedResponse,
                    miIoSendCommand.getCommandString(), Utils.getHex(deviceId), e.getMessage());
            errorMsg = "Received message is invalid JSON";
        } catch (MiCloudException e) {
            logger.debug("Send command '{}'  -> cloudserver '{}' (Device: {}) gave error {}",
                    miIoSendCommand.getCommandString(), miIoSendCommand.getCloudServer(), Utils.getHex(deviceId),
                    e.getMessage());
            errorMsg = e.getMessage();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
        JsonObject erroResp = new JsonObject();
        erroResp.addProperty("error", errorMsg);
        miIoSendCommand.setResponse(erroResp);
        return miIoSendCommand;
    }

    public synchronized void startReceiver() {
        MessageSenderThread senderThread = this.senderThread;
        if (senderThread == null || !senderThread.isAlive()) {
            senderThread = new MessageSenderThread();
            senderThread.start();
            this.senderThread = senderThread;
        }
    }

    /**
     * The {@link MessageSenderThread} is responsible for consuming messages from the queue and sending these to the
     * device
     *
     */
    private class MessageSenderThread extends Thread {
        public MessageSenderThread() {
            super("Mi IO MessageSenderThread");
            setDaemon(true);
        }

        @Override
        public void run() {
            logger.debug("Starting Mi IO MessageSenderThread");
            while (!interrupted()) {
                try {
                    if (concurrentLinkedQueue.isEmpty()) {
                        Thread.sleep(100);
                        continue;
                    }
                    MiIoSendCommand queuedMessage = concurrentLinkedQueue.remove();
                    MiIoSendCommand miIoSendCommand = sendMiIoSendCommand(queuedMessage);
                    for (MiIoMessageListener listener : listeners) {
                        logger.trace("inform listener {}, data {} from {}", listener, queuedMessage, miIoSendCommand);
                        try {
                            listener.onMessageReceived(miIoSendCommand);
                        } catch (Exception e) {
                            logger.debug("Could not inform listener {}: {}: ", listener, e.getMessage(), e);
                        }
                    }
                } catch (NoSuchElementException e) {
                    // ignore
                } catch (InterruptedException e) {
                    // That's our signal to stop
                    break;
                } catch (Exception e) {
                    logger.warn("Error while polling/sending message", e);
                }
            }
            closeSocket();
            logger.debug("Finished Mi IO MessageSenderThread");
        }
    }

    private String sendCommand(String command, byte[] token, String ip, byte[] deviceId)
            throws MiIoCryptoException, IOException {
        byte[] sendMsg = new byte[0];
        if (!command.isBlank()) {
            byte[] encr;
            encr = MiIoCrypto.encrypt(command.getBytes(StandardCharsets.UTF_8), token);
            timeStamp = (int) Instant.now().getEpochSecond();
            sendMsg = Message.createMsgData(encr, token, deviceId, timeStamp + timeDelta);
        }
        Message miIoResponseMsg = sendData(sendMsg, ip);
        if (miIoResponseMsg == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No response from device {} at {} for command {}.\r\n{}", Utils.getHex(deviceId), ip,
                        command, (new Message(sendMsg)).toSting());
            } else {
                logger.debug("No response from device {} at {} for command {}.", Utils.getHex(deviceId), ip, command);
            }
            errorCounter++;
            if (errorCounter > MAX_ERRORS) {
                status = ThingStatusDetail.CONFIGURATION_ERROR;
                sendPing(ip);
            }
            return "{\"error\":\"No Response\"}";
        }
        if (!miIoResponseMsg.isChecksumValid()) {
            return "{\"error\":\"Message has invalid checksum\"}";
        }
        if (errorCounter > 0) {
            errorCounter = 0;
            status = ThingStatusDetail.NONE;
            updateStatus(ThingStatus.ONLINE, status);
        }
        if (!connected) {
            pingSuccess();
        }
        String decryptedResponse = new String(MiIoCrypto.decrypt(miIoResponseMsg.getData(), token), "UTF-8").trim();
        logger.trace("Received response from {}: {}", ip, decryptedResponse);
        return decryptedResponse;
    }

    public @Nullable Message sendPing(String ip) throws IOException {
        for (int i = 0; i < 3; i++) {
            logger.debug("Sending Ping {} ({})", Utils.getHex(deviceId), ip);
            Message resp = sendData(XiaomiGatewayV3BindingConstants.DISCOVER_STRING, ip);
            if (resp != null) {
                pingSuccess();
                return resp;
            }
        }
        pingFail();
        return null;
    }

    private void pingFail() {
        logger.debug("Ping {} ({}) failed", Utils.getHex(deviceId), ip);
        connected = false;
        status = ThingStatusDetail.COMMUNICATION_ERROR;
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
    }

    private void pingSuccess() {
        logger.debug("Ping {} ({}) success", Utils.getHex(deviceId), ip);
        if (!connected) {
            connected = true;
            status = ThingStatusDetail.NONE;
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE);
        } else {
            if (ThingStatusDetail.CONFIGURATION_ERROR.equals(status)) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
            } else {
                status = ThingStatusDetail.NONE;
                updateStatus(ThingStatus.ONLINE, status);
            }
        }
    }

    private void updateStatus(ThingStatus status, ThingStatusDetail statusDetail) {
        for (MiIoMessageListener listener : listeners) {
            logger.trace("inform listener {}, data {} from {}", listener, status, statusDetail);
            try {
                listener.onStatusUpdated(status, statusDetail);
            } catch (Exception e) {
                logger.debug("Could not inform listener {}: {}", listener, e.getMessage(), e);
            }
        }
    }

    private @Nullable Message sendData(byte[] sendMsg, String ip) throws IOException {
        byte[] response = comms(sendMsg, ip);
        if (response.length >= 32) {
            Message miIoResponse = new Message(response);
            timeStamp = (int) TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTime().getTime());
            timeDelta = miIoResponse.getTimestampAsInt() - timeStamp;
            logger.trace("Message Details:{} ", miIoResponse.toSting());
            return miIoResponse;
        } else {
            logger.trace("Reponse length <32 : {}", response.length);
            return null;
        }
    }

    private synchronized byte[] comms(byte[] message, String ip) throws IOException {
        InetAddress ipAddress = InetAddress.getByName(ip);
        DatagramSocket clientSocket = getSocket();
        DatagramPacket receivePacket = new DatagramPacket(new byte[MSG_BUFFER_SIZE], MSG_BUFFER_SIZE);
        try {
            logger.trace("Connection {}:{}", ip, clientSocket.getLocalPort());
            if (message.length > 0) {
                byte[] sendData = new byte[MSG_BUFFER_SIZE];
                sendData = message;
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress,
                        XiaomiGatewayV3BindingConstants.PORT);
                clientSocket.send(sendPacket);
                sendPacket.setData(new byte[MSG_BUFFER_SIZE]);
            }
            clientSocket.receive(receivePacket);
            byte[] response = Arrays.copyOfRange(receivePacket.getData(), receivePacket.getOffset(),
                    receivePacket.getOffset() + receivePacket.getLength());
            return response;
        } catch (SocketTimeoutException e) {
            logger.debug("Communication error for Mi device at {}: {}", ip, e.getMessage());
            needPing = true;
            return new byte[0];
        }
    }

    private DatagramSocket getSocket() throws SocketException {
        @Nullable
        DatagramSocket socket = this.socket;
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeout);
            logger.debug("Opening socket on port: {} ", socket.getLocalPort());
            this.socket = socket;
            return socket;
        } else {
            return socket;
        }
    }

    public void close() {
        try {
            final MessageSenderThread senderThread = this.senderThread;
            if (senderThread != null) {
                senderThread.interrupt();
            }
        } catch (SecurityException e) {
            logger.debug("Error while closing: {} ", e.getMessage());
        }
        closeSocket();
    }

    public void closeSocket() {
        try {
            final DatagramSocket socket = this.socket;
            if (socket != null) {
                logger.debug("Closing socket for port: {} ", socket.getLocalPort());
                socket.close();
                this.socket = null;
            }
        } catch (SecurityException e) {
            logger.debug("Error while closing: {} ", e.getMessage());
        }
    }

    /**
     * @return the id
     */
    public int getId() {
        return id.incrementAndGet();
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id.set(id);
    }

    /**
     * Time delta between device time and server time
     *
     * @return delta
     */
    public int getTimeDelta() {
        return timeDelta;
    }

    public byte[] getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(byte[] deviceId) {
        this.deviceId = deviceId;
    }

    public int getQueueLength() {
        return concurrentLinkedQueue.size();
    }
}
