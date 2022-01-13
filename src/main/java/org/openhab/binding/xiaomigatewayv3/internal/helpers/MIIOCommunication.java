package org.openhab.binding.xiaomigatewayv3.internal.helpers;


import org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


import org.openhab.core.cache.ExpiringCache;


import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoAsyncCommunication;
import org.openhab.binding.xiaomigatewayv3.internal.miio.Utils;
import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.CloudConnector;
import org.openhab.binding.xiaomigatewayv3.internal.miio.Message;
import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoMessageListener;
import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoSendCommand;
import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoCryptoException;
import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoCrypto;
import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoCommand;

import org.openhab.binding.xiaomigatewayv3.internal.json.GatewayDevicesList;

import org.openhab.binding.xiaomigatewayv3.internal.handlers.XiaomiGatewayV3Configuration;
import org.openhab.binding.xiaomigatewayv3.internal.handlers.XiaomiGatewayV3BridgeHandler;


import java.io.IOException;
public class MIIOCommunication implements MiIoMessageListener{
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected @Nullable MiIoAsyncCommunication miioCom;
    
    protected byte[] token = new byte[0];
    protected CloudConnector cloudConnector;
    protected String cloudServer = "";
   
    protected int lastId;

    private @Nullable XiaomiGatewayV3Configuration configuration;
    private XiaomiGatewayV3BridgeHandler bridge;

    protected static final long CACHE_EXPIRY_NETWORK = TimeUnit.SECONDS.toMillis(60);
    protected final ExpiringCache<String> network = new ExpiringCache<>(CACHE_EXPIRY_NETWORK, () -> {
        int ret = sendCommand(MiIoCommand.MIIO_INFO);
        if (ret != 0) {
            return "id:" + ret;
        }
        return "failed";
    });;

    public MIIOCommunication(CloudConnector cloudConnector, XiaomiGatewayV3Configuration configuration, XiaomiGatewayV3BridgeHandler bridge) {
        this.cloudConnector = cloudConnector;
        this.configuration = configuration;
        this.bridge = bridge;
    }

    protected synchronized @Nullable MiIoAsyncCommunication getConnection() {
        if (miioCom != null) {
            return miioCom;
        }        
        
        String deviceId = configuration.deviceId;
    
        try {
            if (deviceId != null && deviceId.length() == 8 && tokenCheckPass(configuration.token)) {
                final MiIoAsyncCommunication miioCom = new MiIoAsyncCommunication(configuration.host, token,
                        Utils.hexStringToByteArray(deviceId), lastId, configuration.timeout, cloudConnector);
                if (getCloudServer().isBlank()) {
                    logger.debug("Ping Mi device {} at {}", deviceId, configuration.host);
                    Message miIoResponse = miioCom.sendPing(configuration.host);
                    if (miIoResponse != null) {
                        logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}",
                                Utils.getHex(miIoResponse.getDeviceId()), configuration.host,
                                miIoResponse.getTimestamp(), LocalDateTime.now(), miioCom.getTimeDelta());
                        miioCom.registerListener(this);
                        this.miioCom = miioCom;
                        return miioCom;
                    } else {
                        miioCom.close();
                    }
                } else {
                    miioCom.registerListener(this);
                    this.miioCom = miioCom;
                    return miioCom;
                }
            } else {
                logger.debug("No device ID defined. Retrieving Mi device ID");
                final MiIoAsyncCommunication miioCom = new MiIoAsyncCommunication(configuration.host, token,
                        new byte[0], lastId, configuration.timeout, cloudConnector);
                Message miIoResponse = miioCom.sendPing(configuration.host);
                if (miIoResponse != null) {
                    logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}",
                            Utils.getHex(miIoResponse.getDeviceId()), configuration.host, miIoResponse.getTimestamp(),
                            LocalDateTime.now(), miioCom.getTimeDelta());
                    deviceId = Utils.getHex(miIoResponse.getDeviceId());
                    logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}", deviceId,
                            configuration.host, miIoResponse.getTimestamp(), LocalDateTime.now(),
                            miioCom.getTimeDelta());
                    miioCom.setDeviceId(miIoResponse.getDeviceId());
                    logger.debug("Using retrieved Mi device ID: {}", deviceId);
                    updateDeviceIdConfig(deviceId);
                    miioCom.registerListener(this);
                    this.miioCom = miioCom;
                    return miioCom;
                } else {
                    miioCom.close();
                }
            }
            logger.debug("Ping response from device {} at {} FAILED", configuration.deviceId, configuration.host);
            disconnectedNoResponse();
            return null;
        } catch (IOException e) {
            //logger.debug("Could not connect to {} at {}", getThing().getUID().toString(), configuration.host);
            logger.debug("Could not connect to {}", configuration.host);
            disconnected(e.getMessage());
            return null;
        }
    }

    private void updateDeviceIdConfig(String deviceId) {
        // if (!deviceId.isEmpty()) {
        //     updateProperty(Thing.PROPERTY_SERIAL_NUMBER, deviceId);
        //     Configuration config = editConfiguration();
        //     config.put(PROPERTY_DID, deviceId);
        //     updateConfiguration(config);
        // } else {
        //     logger.debug("Could not update config with device ID: {}", deviceId);
        // }
    }

    protected void disconnectedNoResponse() {
        // disconnected("No Response from device");
    }

    protected void disconnected(@Nullable String message) {
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
        //         message != null ? message : "");
        // final MiIoAsyncCommunication miioCom = this.miioCom;
        // if (miioCom != null) {
        //     lastId = miioCom.getId();
        //     lastId += 10;
        // }
    }


    String getCloudServer() {
        // This can be improved in the future with additional / more advanced options like e.g. directFirst which would
        // use direct communications and in case of failures fall back to cloud communication. For now we keep it
        // simple and only have the option for cloud or direct.
        //final XiaomiGatewayV3Configuration configuration = this.configuration;
        // if (configuration != null && configuration.communication != null) {
        //     return configuration.communication.equals("cloud") ? cloudServer : "";
        // }
        return "";
    }

    public boolean tokenCheckPass(@Nullable String tokenSting) {
        if (tokenSting == null) {
            return false;
        }
        switch (tokenSting.length()) {
            case 16:
                token = tokenSting.getBytes();
                return true;
            case 32:
                if (!XiaomiGatewayV3BindingConstants.IGNORED_TOKENS.contains(tokenSting)) {
                    token = Utils.hexStringToByteArray(tokenSting);
                    return true;
                }
                return false;
            case 96:
                try {
                    token = Utils.hexStringToByteArray(MiIoCrypto.decryptToken(Utils.hexStringToByteArray(tokenSting)));
                    logger.debug("IOS token decrypted to {}", Utils.getHex(token));
                } catch (MiIoCryptoException e) {
                    logger.warn("Could not decrypt token {}{}", tokenSting, e.getMessage());
                    return false;
                }
                return true;
            default:
                return false;
        }
    }

    public int sentCommand_MIIO_INFO(){
        return sendCommand(MiIoCommand.MIIO_INFO);
    }

    public int sentCommand_TELNET_ENABLE(){
        return sendCommand(MiIoCommand.TELNET_ENABLE);
    }

    public int sentCommand_GET_DEVICE_LIST(){
        return sendCommand(MiIoCommand.GET_DEVICE_LIST);
    }

    protected int sendCommand(MiIoCommand command) {
        return sendCommand(command, "[]");
    }

    protected int sendCommand(MiIoCommand command, String params) {
        try {
            final MiIoAsyncCommunication connection = getConnection();
            return (connection != null) ? connection.queueCommand(command, params, getCloudServer()) : 0;
        } catch (MiIoCryptoException | IOException e) {
            //logger.debug("Command {} for {} failed (type: {}): {}", command.toString(), getThing().getUID(), getThing().getThingTypeUID(), e.getLocalizedMessage());
            logger.debug("Command {} failed: {}", command.toString(), e.getLocalizedMessage());
        }
        return 0;
    }

    protected int sendCommand(String commandString) {
        return sendCommand(commandString, getCloudServer());
    }

    /**
     * This is used to execute arbitrary commands by sending to the commands channel. Command parameters to be added
     * between
     * [] brackets. This to allow for unimplemented commands to be executed (e.g. get detailed historical cleaning
     * records)
     *
     * @param commandString command to be executed
     * @param cloud server to be used or empty string for direct sending to the device
     * @return vacuum response
     */
    protected int sendCommand(String commandString, String cloudServer) {
        final MiIoAsyncCommunication connection = getConnection();
        try {
            String command = commandString.trim();
            String param = "[]";
            int sb = command.indexOf("[");
            int cb = command.indexOf("{");
            if (Math.max(sb, cb) > 0) {
                int loc = (Math.min(sb, cb) > 0 ? Math.min(sb, cb) : Math.max(sb, cb));
                param = command.substring(loc).trim();
                command = command.substring(0, loc).trim();
            }
            return (connection != null) ? connection.queueCommand(command, param, cloudServer) : 0;
        } catch (MiIoCryptoException | IOException e) {
            disconnected(e.getMessage());
        }
        return 0;
    }

    @Override
    public void onMessageReceived(MiIoSendCommand response) {
        //logger.debug("Received response for {} type: {}, result: {}, fullresponse: {}", getThing().getUID().getId(),
        //        response.getCommand(), response.getResult(), response.getResponse());
        logger.debug("Received response for type: {}, result: {}, fullresponse: {}",
                response.getCommand(), response.getResult(), response.getResponse());

        if (response.isError()) {
            logger.debug("Error received: {}", response.getResponse().get("error"));
            if (MiIoCommand.MIIO_INFO.equals(response.getCommand())) {
                network.invalidateValue();
            }

            //ignoring telnet response issue and trying to connect anyway as per descussion https://community.openhab.org/t/xiaomi-gateway-3-binding-zndmwg03lm/117759/75?u=dexter 
            switch (response.getCommand()){
                case TELNET_ENABLE:
                    logger.debug("Error in response for TELNET_ENABLE command. Ignoring and continuing.");
                    break;
                default:
                    return;
            }
        }
        
        try {
            switch (response.getCommand()) {
                // case MIIO_INFO:
                //     if (!isIdentified) {
                //         defineDeviceType(response.getResult().getAsJsonObject());
                //     }
                //     updateNetwork(response.getResult().getAsJsonObject());
                //     break;
                case TELNET_ENABLE:
                    var telnet = new TenletCommunication();
                    telnet.EnableMqttExternalServer(configuration.host);
                    break;
                case GET_DEVICE_LIST:
                    parseDeviceList(response.getResponse().toString());
                    break;
                default:
                    break;
            }
            // if (cmds.containsKey(response.getId())) {
            //     if (response.getCloudServer().isBlank()) {
            //         updateState(CHANNEL_COMMAND, new StringType(response.getResponse().toString()));
            //     } else {
            //         updateState(CHANNEL_RPC, new StringType(response.getResponse().toString()));
            //     }
            //     cmds.remove(response.getId());
            // }
        } catch (Exception e) {
            logger.debug("Error while handing message {}", response.getResponse(), e);
        }
    }

    private void parseDeviceList(String str){
        GatewayDevicesList message = new Gson().fromJson(str, GatewayDevicesList.class);
        logger.info("Found devices count: {}", message.result.size());
        if(bridge != null)
            bridge.getDevicesListRequestCompleted(message);

    }

    @Override
    public void onStatusUpdated(ThingStatus status, ThingStatusDetail statusDetail) {
        //updateStatus(status, statusDetail);
    }

}
