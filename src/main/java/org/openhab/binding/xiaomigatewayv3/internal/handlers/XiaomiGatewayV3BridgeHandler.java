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

import static org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants.*;

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

 import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
 import org.openhab.core.io.transport.mqtt.MqttConnectionObserver;
 import org.openhab.core.io.transport.mqtt.MqttConnectionState;
 import org.openhab.core.io.transport.mqtt.MqttMessageSubscriber;
 import org.openhab.core.thing.ThingTypeUID;
 
import org.openhab.core.library.types.StringType;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.stream.Collectors;

import org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessage;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageReport;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageHeartbeat;

/**
 * The {@link XiaomiGatewayV3BridgeHandler} bridgeHandler implementation
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public class XiaomiGatewayV3BridgeHandler extends BaseBridgeHandler implements MqttConnectionObserver, MqttMessageSubscriber {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    //private @Nullable XiaomiGatewayV3Configuration config;
    private @Nullable MqttBrokerConnection connection;

    private String topicsToSubscribe = "#";
    private String gatewayIpAddress = "";

    public XiaomiGatewayV3BridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        
    }

    @Override
    public void initialize() {
        //config = getConfigAs(XiaomiGatewayV3Configuration.class);

        logger.debug("Initializing the bridge"); 

        // TODO: Initialize the handler.
        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            //boolean thingReachable = true; // <background task with long running initialization here>
            
            Object ipAddressObject = thing.getConfiguration().get(XiaomiGatewayV3BindingConstants.GATEWAY_IP_ADDRESS);
            String ipAddress = (String) ipAddressObject;        

            logger.debug("IP address: {}", ipAddress); 
            //thingReachable = ipAddress.contains("123");

            try {
                if(!gatewayIpAddress.equals(ipAddress)){
                    gatewayIpAddress = ipAddress;
                    connectMQTT();
                }
                //updateStatus(ThingStatus.ONLINE);
            } catch (Exception e) {
                logger.warn("Error connecting to the queue: {}", e.toString());
                updateStatus(ThingStatus.OFFLINE);
            }

             

            // if (thingReachable) {
            //     updateStatus(ThingStatus.ONLINE);
            // } else {
            //     updateStatus(ThingStatus.OFFLINE);
            // }
        });


        // These logging types should be primarily used by bindings
        // logger.trace("Example trace message");
        // logger.debug("Example debug message");
        // logger.warn("Example warn message");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }


    public void connectMQTT() {
        MqttBrokerConnection localConnection = null;
        if(connection != null){
            connection.unsubscribe(topicsToSubscribe, this);
            connection.stop();
            connection = null;
        }

        logger.debug("Creating a new instance of MqttBrokerConnection"); 

        localConnection = new MqttBrokerConnection(gatewayIpAddress, 1883, false, "sgbinding1");

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
                //logger.info("Received params"); 
                if(message.params.size() != 0)
                    {
                        //logger.info("Received {}: {}", message.params.get(0).Name, message.params.get(0).Value.toString());                        
                        for (Thing thing : getFilteredDevicesList()) {                            
                            Object thingDId = thing.getConfiguration().get(XiaomiGatewayV3BindingConstants.THING_DEVICE_ID);
                            if(thingDId != null && thingDId.toString().equals(message.getDeviceId())){
                                logger.info("Found did: {}", thingDId);
                                ((AbstractXiaomiGatewayV3ThingHandler) thing.getHandler()).updateProperties(message.params);
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
                //logger.info("Received params"); 
                if(message.getFirstParam() != null                                 
                                && message.getFirstParam().res_list != null
                                && message.getFirstParam().res_list.size() != 0)
                    {
                        //logger.info("Received {}: {}", message.params.get(0).Name, message.params.get(0).Value.toString());
                        for (Thing thing : getFilteredDevicesList()) {                            
                            Object thingDId = thing.getConfiguration().get(XiaomiGatewayV3BindingConstants.THING_DEVICE_ID);
                            if(thingDId != null && thingDId.toString().equals(message.getFirstParam().getDeviceId())){
                                logger.info("Found did: {}", thingDId);
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
                //parsing the message
                if(message.getCmd().equals("report")){
                    //logger.info("Parsing a message like a report");
                    processMessageAsReport(state);
                } else if (message.getCmd().equals("heartbeat")){
                    //logger.info("Parsing a message like a heartbeat");
                    processMessageAsHeartbeat(state);
                }
            }
            //return new DeviceCollection(devices);
        } catch (JsonSyntaxException e) {
            logger.warn("Can't parse json: {}", state);
        }
    }

    @Override
    public void processMessage(String topic, byte[] payload) {
        String state = new String(payload, StandardCharsets.UTF_8);
        logger.debug("Message received. Topic: {}, payload: {}", topic, state);
        
        if(topic.startsWith("gw/") && topic.endsWith("heartbeat")){
            //update binding heartbeat
            updateState(new ChannelUID(getThing().getUID(), XiaomiGatewayV3BindingConstants.BRIDGE_HEARTBEAT), new StringType(state)); 
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
                //updateStatus(ThingStatus.UNKNOWN);
                //break;
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

}
