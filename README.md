# XiaomiGatewayV3 Binding

This repository serves as host for the documentation and the issue tracker for Xiaomi Gateway V3 binding for [OpenHab](https://openhab.org).
This binding works with Xiaomi Gateway 3 (ZNDMWG03LM) device.

All ideas were taken from https://github.com/AlexxIT/XiaomiGateway3
Please check AlexxIT for explanation about firmware and how to enable internal MQTT server.

The binding is connecting to the internal MQTT queue and parces messages.


## Supported Things

- Xiaomi/Aquara Door Sensor (MCCGQ01LM)
- Aqara Motion Sensor (RTCGQ11LM)

## Discovery

No auto-discovery for now.

## Binding Configuration

1. Configure the gateway as discribed here: https://github.com/AlexxIT/XiaomiGateway3
Checkpoint: you should be able to connect to the gateway with a MQTT client (for example - [MQTT Explorer](http://mqtt-explorer.com/)) and see incoming messages in zigbee/send queue.

2. Create a bridge, setting IP address to the IP address of your gateway.

3. The gateway bridge should be online for now. 


## Thing Configuration

4. Connect a sensor to the gateway using MiHome app.

5. Create a Thing and set Device ID to the sensor ID value. For my sensors it looks like this: 'lumi.12345abcdef123'
As there is no auto-discovery yet, you should get it from the MQTT client. Trigger a sensor and check the message with 'cmd=report' (not heartbeat) value.   

6. Setup required channels.


## Channels

ToDO 

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |


## Any custom content here!


To compile: run mvn clean install -pl :org.openhab.binding.xiaomigatewayv3 -D"spotless.check.skip"=true

I use command: mvn clean install -D"spotless.check.skip"=true -DskipChecks