/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.xiaomigatewayv3.internal.handlers;

//import static org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants.*;

import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import javassist.expr.Instanceof;

import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
 import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;
 import org.openhab.core.io.transport.mqtt.MqttConnectionState;
 import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
 //import org.openhab.core.thing.ThingTypeUID;
 

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openhab.core.cache.ExpiringCache;

import org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessage;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageReport;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageHeartbeat;

import org.openhab.binding.xiaomigatewayv3.internal.miio.MiIoAsyncCommunication;
import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.CloudConnector;

import org.openhab.binding.xiaomigatewayv3.internal.json.GatewayDevicesList;
import java.util.Collection;
import java.util.Collections;

import org.openhab.binding.xiaomigatewayv3.internal.helpers.MIIOCommunication;

import org.openhab.binding.xiaomigatewayv3.internal.discovery.XiaomiGatewayV3Discovery;
import org.openhab.core.thing.binding.ThingHandlerService;

import static org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants.*;


/**
 * The {@link XiaomiGatewayV3BridgeHandler} bridgeHandler implementation
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public class XiaomiGatewayV3BridgeHandler extends BaseBridgeHandler 
                                    implements MqttConnectionObserver, MqttMessageSubscriber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Nullable XiaomiGatewayV3Configuration configuration;
    private @Nullable MqttBrokerConnection connection;
    private @Nullable XiaomiGatewayV3Discovery discoveryService;

    private String topicsToSubscribe = "#";

    private @Nullable String mqttUniqueTopic = null;    

    private int connectionMQTTAttempts = 0;
    private final int MAX_MQTT_CONNECTION_ATTEMPTS = 3;
    //miio
    protected @Nullable MiIoAsyncCommunication miioCom;
    protected byte[] token = new byte[0];
    protected CloudConnector cloudConnector;
    protected String cloudServer = "";
    protected int lastId;
    protected static final long CACHE_EXPIRY_NETWORK = TimeUnit.SECONDS.toMillis(60);
    
    protected @Nullable MIIOCommunication miioComm;


    protected final ExpiringCache<String> network = new ExpiringCache<>(CACHE_EXPIRY_NETWORK, () -> {
        if(miioComm != null)
        {
            int ret = miioComm.sentCommand_MIIO_INFO();
            if (ret != 0) {
                return "id:" + ret;
            }
        }
        return "failed";
    });


    public XiaomiGatewayV3BridgeHandler(Bridge bridge, CloudConnector cloudConnector) {
        super(bridge);

        this.cloudConnector = cloudConnector;        
    }

    String PAIRING_MQTT_MSG = "{\"commands\": [{\"command\": \"lumi open-with-cloud-install-code 0x3c\",\"postDelayMs\": 0}]}";

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        try {
            switch (channelUID.getId()) {
                case BRIDGE_PAIRING_MODE:
                    if(mqttUniqueTopic == null) {
                        logger.warn("Haven't received any heartbeat messages yet, cannot detect topic for pairing. Please try again in a couple of minutes.");                        
                    } else {
                        if(command.toString().equals("PAIRING_MODE")) {
                            logger.info("Turning on pairing mode");
                            if(connection != null){                            
                                connection.publish("gw/" + mqttUniqueTopic + "/commands", PAIRING_MQTT_MSG.getBytes(), 1, false);
                            }
                        }
                    }                    
                    break;                
            }
            //statusUpdated(ThingStatus.ONLINE);
        } catch(Exception ex) {
            // catch exceptions and handle it in your binding
            //statusUpdated(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, ex.getMessage());
            logger.error("Error handling the command: " + ex.toString());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing the bridge handler '{}' with thingType {}", getThing().getUID(),
                getThing().getThingTypeUID());

        final XiaomiGatewayV3Configuration configuration = getConfigAs(XiaomiGatewayV3Configuration.class);        
        this.configuration = configuration;
        
        if(miioComm == null)
            miioComm = new MIIOCommunication(cloudConnector, configuration, this);

        updateStatus(ThingStatus.UNKNOWN);

        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.

        if (configuration.host == null || configuration.host.isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "IP address required. Configure IP address");
            return;
        }
        if (!miioComm.tokenCheckPass(configuration.token)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Token required. Configure token");
            return;
        }

        scheduler.execute(() -> {                        
            logger.debug("IP address: {}", configuration.host); 

            try {
                setupXiaomiGateway();
                //start MQTT client despite the result of the configuration
                connectMQTT();

                //something connected with config changed - not needed for now
                // if(!gatewayIpAddress.equals(configuration.host)){
                //     gatewayIpAddress = configuration.host;
                //     connectMQTT();
                // }
            } catch (Exception e) {
                logger.warn("Error connecting to the queue. ", e);
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    /**
     * This method setups Xiaomi Gateway.
     * - enables telnet via miio protocol
     * - enables remote MQTT server connection
     */
    private void setupXiaomiGateway(){
        miioComm.sentCommand_TELNET_ENABLE();  
        miioComm.sentCommand_GET_DEVICE_LIST();
    }

    public void connectMQTT() {
        if(connection == null){
            MqttBrokerConnection localConnection = null;
            if(connection != null){
                connection.unsubscribe(topicsToSubscribe, this);
                connection.stop();
                connection = null;
            }

            logger.debug("Creating a new instance of MqttBrokerConnection"); 

            localConnection = new MqttBrokerConnection(configuration.host, 1883, false, "sgbinding1");

            if (localConnection != null) {
                localConnection.setKeepAliveInterval(20);
                localConnection.setQos(1);
                localConnection.setUnsubscribeOnStop(true);
                localConnection.addConnectionObserver(this);
                localConnection.start();            
                connection = localConnection;
                if (localConnection.connectionState().compareTo(MqttConnectionState.CONNECTED) == 0) {
                    subscribeToTopics();
                    logger.debug("Connection successfull, subscribing to topics");
                    updateStatus(ThingStatus.ONLINE);
                }
            }
        }
    }

    private void subscribeToTopics(){        
        logger.debug("Trying to subscribe to topics...");
        connection.subscribe(topicsToSubscribe, this);


        // Boolean result = false;
        // try {
        //     result = connection.subscribe(topicsToSubscribe, this).get();            
        // } catch (Exception e) {
        //     logger.warn("Cannot subscribe to topics{}", e.toString());
        // }

        // if(result){
        //     logger.debug("Successfully subscribed to topics");
        //     //updateStatus(ThingStatus.ONLINE);
        // }
        // else {
        //     logger.debug("Can't subscribe to topics");
        //     //updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Cannot subscribe to topics.");
        // }
        // return result;
    }


    /**
     * Will return filtered by type things list, connected to the bridge. 
     * Returns all things for now.
     * 
     * @return 
     */
    private List<Thing> getFilteredDevicesList(){
        //ThingTypeUID deviceUid = THING_TYPE_DOOR_SENSOR;
        List<Thing> allThingsFiltered = getThing().getThings();
        //List<Thing> allThingsFiltered = getThing().getThings().stream().filter(thing->thing.getThingTypeUID().equals(deviceUid)).collect(Collectors.toList());                        

        return allThingsFiltered;
    } 

    private void processMessageAsReport(String state){
        try {
            ZigbeeSendMessageReport message = new Gson().fromJson(state, ZigbeeSendMessageReport.class);
            
            if (message == null) {
                logger.warn("Can't parse json (null): {}", state);
            } else {
                if(message.params.size() != 0)
                    {
                        for (Thing thing : getFilteredDevicesList()) {                            
                            Object thingDId = thing.getConfiguration().get(XiaomiGatewayV3BindingConstants.THING_DEVICE_ID);
                            if(thingDId != null && thingDId.toString().equals(message.getDeviceId())){
                                logger.info("Found did: {}", thingDId);
                                if(thing.getHandler() != null)
                                   {
                                        logger.debug("Thing handler is NOT null - updating properties"); 
                                       ((AbstractXiaomiGatewayV3ThingHandler) thing.getHandler()).updateProperties(message.params);
                                   }
                                else
                                    logger.debug("Thing handler is null - should not happen");
                            }
                        }
                    }
            }
        } catch (Exception e) {
            logger.warn("Can't parse processMessageAsReport: {}", state, e);
        }
    }

    private void processMessageAsHeartbeat(String state){
        try {
            ZigbeeSendMessageHeartbeat message = new Gson().fromJson(state, ZigbeeSendMessageHeartbeat.class);
            
            if (message == null) {
                logger.warn("Can't parse json (null): {}", state);
            } else {
                if(message.getFirstParam() != null                                 
                                && message.getFirstParam().res_list != null
                                && message.getFirstParam().res_list.size() != 0)
                    {
                        for (Thing thing : getFilteredDevicesList()) {                            
                            Object thingDId = thing.getConfiguration().get(XiaomiGatewayV3BindingConstants.THING_DEVICE_ID);
                            if(thingDId != null && thingDId.toString().equals(message.getFirstParam().getDeviceId())){
                                //logger.info("Found did: {}", thingDId);
                                ((AbstractXiaomiGatewayV3ThingHandler) thing.getHandler()).updateProperties(message.getFirstParam().res_list);
                            }
                        }
                    }
            }
        } catch (Exception e) {
            logger.warn("Can't parse processMessageAsHeartbeat: {}", state, e);
        }
    }

    private void processThingMessage(String state){
        try {
            ZigbeeSendMessage message = new Gson().fromJson(state, ZigbeeSendMessage.class);
            if (message == null) {
                logger.warn("Can't parse json (null): {}", state);
            } else {
                if(message.getCmd().equals("report")){
                    processMessageAsReport(state);
                } else if (message.getCmd().equals("heartbeat")){
                    processMessageAsHeartbeat(state);
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warn("Can't parse json: {}", state);
        }
    }

    @Override
    public void processMessage(String topic, byte[] payload) {
        String state = new String(payload, StandardCharsets.UTF_8);
        logger.debug("Message received. Topic: {}, payload: {}", topic, state);
        
        if(topic.startsWith("gw/")){
            if(mqttUniqueTopic == null)
            {
                mqttUniqueTopic = topic.replace("gw/", "");
                mqttUniqueTopic = mqttUniqueTopic.substring(0, mqttUniqueTopic.indexOf("/"));
                logger.info("Received a message and saved the topic for future pairing commands: " + mqttUniqueTopic);
            }

            if(topic.endsWith("heartbeat"))
            {
                //update binding heartbeat
                //2021-03-31 19:17:32.221 [WARN ] [.core.thing.binding.BaseThingHandler] - Handler XiaomiGatewayV3BridgeHandler of thing xiaomigatewayv3:config:xiaomi_bridge tried updating channel bridge_heartbeat although the handler was already disposed.
                //updateState(new ChannelUID(getThing().getUID(), XiaomiGatewayV3BindingConstants.BRIDGE_HEARTBEAT), new StringType(state)); 
            }
        } else if(topic.equals("zigbee/send")) {
            processThingMessage(state);
        }
    }

    @Override
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error) {
        logger.debug("MQTT brokers state changed to:{}", state);
        switch (state) {
            case CONNECTED:
                if(!connection.hasSubscribers())
                    subscribeToTopics();
                else
                    logger.debug("Already subscribed to topics");

                updateStatus(ThingStatus.ONLINE);
                break;
            case CONNECTING:
                connectionMQTTAttempts++;
                if(connectionMQTTAttempts > MAX_MQTT_CONNECTION_ATTEMPTS){
                    connectionMQTTAttempts = 0;
                    setupXiaomiGateway();
                }
                break;
            case DISCONNECTED:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Bridge (broker) cannot connect to the MQTT gateway.");
                break;
        }
    }

    @Override
    public void dispose() {
        MqttBrokerConnection localConnection = connection;
        if (localConnection != null) {
            localConnection.unsubscribe(topicsToSubscribe, this);
            localConnection.stop();
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(XiaomiGatewayV3Discovery.class);
    }

    public void sendRequestForDeviceList(){
        miioComm.sentCommand_GET_DEVICE_LIST();
    }

    public void setDiscoveryService(XiaomiGatewayV3Discovery discoveryService) {
        this.discoveryService = discoveryService;
    }

    public void getDevicesListRequestCompleted(GatewayDevicesList deviceList){
        if(discoveryService != null)
            discoveryService.createDisvoceryResult(deviceList);
    }

    // //miio
    // protected synchronized @Nullable MiIoAsyncCommunication getConnection() {
    //     if (miioCom != null) {
    //         return miioCom;
    //     }
    //     final XiaomiGatewayV3Configuration configuration = getConfigAs(XiaomiGatewayV3Configuration.class);
    //     if (configuration.host == null || configuration.host.isEmpty()) {
    //         return null;
    //     }
    //     @Nullable
    //     String deviceId = configuration.deviceId;
    //     try {
    //         if (deviceId != null && deviceId.length() == 8 && tokenCheckPass(configuration.token)) {
    //             final MiIoAsyncCommunication miioCom = new MiIoAsyncCommunication(configuration.host, token,
    //                     Utils.hexStringToByteArray(deviceId), lastId, configuration.timeout, cloudConnector);
    //             if (getCloudServer().isBlank()) {
    //                 logger.debug("Ping Mi device {} at {}", deviceId, configuration.host);
    //                 Message miIoResponse = miioCom.sendPing(configuration.host);
    //                 if (miIoResponse != null) {
    //                     logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}",
    //                             Utils.getHex(miIoResponse.getDeviceId()), configuration.host,
    //                             miIoResponse.getTimestamp(), LocalDateTime.now(), miioCom.getTimeDelta());
    //                     miioCom.registerListener(this);
    //                     this.miioCom = miioCom;
    //                     return miioCom;
    //                 } else {
    //                     miioCom.close();
    //                 }
    //             } else {
    //                 miioCom.registerListener(this);
    //                 this.miioCom = miioCom;
    //                 return miioCom;
    //             }
    //         } else {
    //             logger.debug("No device ID defined. Retrieving Mi device ID");
    //             final MiIoAsyncCommunication miioCom = new MiIoAsyncCommunication(configuration.host, token,
    //                     new byte[0], lastId, configuration.timeout, cloudConnector);
    //             Message miIoResponse = miioCom.sendPing(configuration.host);
    //             if (miIoResponse != null) {
    //                 logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}",
    //                         Utils.getHex(miIoResponse.getDeviceId()), configuration.host, miIoResponse.getTimestamp(),
    //                         LocalDateTime.now(), miioCom.getTimeDelta());
    //                 deviceId = Utils.getHex(miIoResponse.getDeviceId());
    //                 logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}", deviceId,
    //                         configuration.host, miIoResponse.getTimestamp(), LocalDateTime.now(),
    //                         miioCom.getTimeDelta());
    //                 miioCom.setDeviceId(miIoResponse.getDeviceId());
    //                 logger.debug("Using retrieved Mi device ID: {}", deviceId);
    //                 updateDeviceIdConfig(deviceId);
    //                 miioCom.registerListener(this);
    //                 this.miioCom = miioCom;
    //                 return miioCom;
    //             } else {
    //                 miioCom.close();
    //             }
    //         }
    //         logger.debug("Ping response from device {} at {} FAILED", configuration.deviceId, configuration.host);
    //         disconnectedNoResponse();
    //         return null;
    //     } catch (IOException e) {
    //         logger.debug("Could not connect to {} at {}", getThing().getUID().toString(), configuration.host);
    //         disconnected(e.getMessage());
    //         return null;
    //     }
    // }

    // private void updateDeviceIdConfig(String deviceId) {
    //     if (!deviceId.isEmpty()) {
    //         updateProperty(Thing.PROPERTY_SERIAL_NUMBER, deviceId);
    //         Configuration config = editConfiguration();
    //         config.put(PROPERTY_DID, deviceId);
    //         updateConfiguration(config);
    //     } else {
    //         logger.debug("Could not update config with device ID: {}", deviceId);
    //     }
    // }

    // protected void disconnectedNoResponse() {
    //     disconnected("No Response from device");
    // }

    // protected void disconnected(@Nullable String message) {
    //     updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
    //             message != null ? message : "");
    //     final MiIoAsyncCommunication miioCom = this.miioCom;
    //     if (miioCom != null) {
    //         lastId = miioCom.getId();
    //         lastId += 10;
    //     }
    // }


    // String getCloudServer() {
    //     // This can be improved in the future with additional / more advanced options like e.g. directFirst which would
    //     // use direct communications and in case of failures fall back to cloud communication. For now we keep it
    //     // simple and only have the option for cloud or direct.
    //     //final XiaomiGatewayV3Configuration configuration = this.configuration;
    //     // if (configuration != null && configuration.communication != null) {
    //     //     return configuration.communication.equals("cloud") ? cloudServer : "";
    //     // }
    //     return "";
    // }

    // private boolean tokenCheckPass(@Nullable String tokenSting) {
    //     if (tokenSting == null) {
    //         return false;
    //     }
    //     switch (tokenSting.length()) {
    //         case 16:
    //             token = tokenSting.getBytes();
    //             return true;
    //         case 32:
    //             if (!IGNORED_TOKENS.contains(tokenSting)) {
    //                 token = Utils.hexStringToByteArray(tokenSting);
    //                 return true;
    //             }
    //             return false;
    //         case 96:
    //             try {
    //                 token = Utils.hexStringToByteArray(MiIoCrypto.decryptToken(Utils.hexStringToByteArray(tokenSting)));
    //                 logger.debug("IOS token decrypted to {}", Utils.getHex(token));
    //             } catch (MiIoCryptoException e) {
    //                 logger.warn("Could not decrypt token {}{}", tokenSting, e.getMessage());
    //                 return false;
    //             }
    //             return true;
    //         default:
    //             return false;
    //     }
    // }

    // protected int sendCommand(MiIoCommand command) {
    //     return sendCommand(command, "[]");
    // }

    // protected int sendCommand(MiIoCommand command, String params) {
    //     try {
    //         final MiIoAsyncCommunication connection = getConnection();
    //         return (connection != null) ? connection.queueCommand(command, params, getCloudServer()) : 0;
    //     } catch (MiIoCryptoException | IOException e) {
    //         logger.debug("Command {} for {} failed (type: {}): {}", command.toString(), getThing().getUID(),
    //                 getThing().getThingTypeUID(), e.getLocalizedMessage());
    //     }
    //     return 0;
    // }

    // protected int sendCommand(String commandString) {
    //     return sendCommand(commandString, getCloudServer());
    // }

    // /**
    //  * This is used to execute arbitrary commands by sending to the commands channel. Command parameters to be added
    //  * between
    //  * [] brackets. This to allow for unimplemented commands to be executed (e.g. get detailed historical cleaning
    //  * records)
    //  *
    //  * @param commandString command to be executed
    //  * @param cloud server to be used or empty string for direct sending to the device
    //  * @return vacuum response
    //  */
    // protected int sendCommand(String commandString, String cloudServer) {
    //     final MiIoAsyncCommunication connection = getConnection();
    //     try {
    //         String command = commandString.trim();
    //         String param = "[]";
    //         int sb = command.indexOf("[");
    //         int cb = command.indexOf("{");
    //         if (Math.max(sb, cb) > 0) {
    //             int loc = (Math.min(sb, cb) > 0 ? Math.min(sb, cb) : Math.max(sb, cb));
    //             param = command.substring(loc).trim();
    //             command = command.substring(0, loc).trim();
    //         }
    //         return (connection != null) ? connection.queueCommand(command, param, cloudServer) : 0;
    //     } catch (MiIoCryptoException | IOException e) {
    //         disconnected(e.getMessage());
    //     }
    //     return 0;
    // }


    // @Override
    // public void onStatusUpdated(ThingStatus status, ThingStatusDetail statusDetail) {
    //     updateStatus(status, statusDetail);
    // }

    // @Override
    // public void onMessageReceived(MiIoSendCommand response) {
    //     logger.debug("Received response for {} type: {}, result: {}, fullresponse: {}", getThing().getUID().getId(),
    //             response.getCommand(), response.getResult(), response.getResponse());
    //     if (response.isError()) {
    //         logger.debug("Error received: {}", response.getResponse().get("error"));
    //         if (MiIoCommand.MIIO_INFO.equals(response.getCommand())) {
    //             network.invalidateValue();
    //         }
    //         return;
    //     }
    //     try {
    //         switch (response.getCommand()) {
    //             // case MIIO_INFO:
    //             //     if (!isIdentified) {
    //             //         defineDeviceType(response.getResult().getAsJsonObject());
    //             //     }
    //             //     updateNetwork(response.getResult().getAsJsonObject());
    //             //     break;
    //             case TELNET_ENABLE:
    //                 var telnet = new TenletCommunication();
    //                 telnet.EnableMqttExternalServer(configuration.host);
    //                 break;

    //             default:
    //                 break;
    //         }
    //         // if (cmds.containsKey(response.getId())) {
    //         //     if (response.getCloudServer().isBlank()) {
    //         //         updateState(CHANNEL_COMMAND, new StringType(response.getResponse().toString()));
    //         //     } else {
    //         //         updateState(CHANNEL_RPC, new StringType(response.getResponse().toString()));
    //         //     }
    //         //     cmds.remove(response.getId());
    //         // }
    //     } catch (Exception e) {
    //         logger.debug("Error while handing message {}", response.getResponse(), e);
    //     }
    // }

}
