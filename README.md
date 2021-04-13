# XiaomiGatewayV3 Binding

This repository serves as host for the documentation and the issue tracker for Xiaomi Gateway V3 binding for [OpenHab](https://openhab.org).
This binding works with Xiaomi Gateway 3 (ZNDMWG03LM) device.

All ideas were taken from https://github.com/AlexxIT/XiaomiGateway3
Please check AlexxIT for explanation about firmware and how to enable passwordless telnet if you haven't done it yet.

The binding is connecting to the internal MQTT queue and parces messages.

When creating a binding you need to set three parameters: IP address, token, device ID.

Token and device ID can be seen in the Mi Home app. Note: deviceId should be HEX value. My app shows decimal value, so I had to convert it from decimal to hex.

While initializing - binding tries to enable telnet service on the device. See details [here](https://community.home-assistant.io/t/xiaomi-mijia-smart-multi-mode-gateway-zndmwg03lm-support/159586/61).

All code for communication via MiIO protocol was taken from miio openhab binding.


## Supported Things

- Xiaomi/Aquara Door Sensor (MCCGQ01LM)
- Aqara Motion Sensor (RTCGQ11LM)

## Discovery

The binding can find attached supported ZigBee devices now.
The binding cannot find gateway itself.
The binding cannot find bluetooth devices.

## Binding Configuration

1. Configure the gateway as discribed here: https://github.com/AlexxIT/XiaomiGateway3

2. Create a bridge, set up IP address, token and device ID. The binding automatically turns on telnet and remote MQTT access. 

Important checkpoint after this step: you should be able to connect to the gateway with a MQTT client (for example - [MQTT Explorer](http://mqtt-explorer.com/)) and see incoming messages in zigbee/send queue.

3. The gateway bridge should be online. 


## Thing Configuration

4. Connect a sensor to the gateway using MiHome app.

5. Create a Thing and set Device ID to the sensor ID value. For my sensors it looks like this: 'lumi.12345abcdef123'

As there is no auto-discovery yet, you should get it from the MQTT client (or turn on log level to DEBUG and check logs for incoming messages)

Trigger a sensor and check the message with 'cmd=report' (not heartbeat) value.  

6. Setup required channels.


## Channels

ToDO 

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/OH-INF/thing``` of your binding._

| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |


## Any custom content here!


To compile - I use the command: mvn clean install -D"spotless.check.skip"=true -DskipChecks

## To add a new device
1. XiaomiGatewayV3BindingConstants.java

    a. Add a new ThingTypeUID like THING_TYPE_DOOR_SENSOR

    b. Add this ThingTypeUID to the SUPPORTED_THING_TYPES_UIDS    

    c. Add device description to the ThingDescriptions list.

    d. Add specific channels to the Specific sensor channels section

2. thing-types.xml

    a. Add new channel-type to the bottom of the file with the same IDs as from step 1-d

    b. Add new thing-type to the file with the same ID as in the step 1-a

3. Create a new file YourNewSensorThingHandler.java based on DoorWindowSensorThingHandler.java in the handlers folder.

    a. Fill in LocalParamCollection with the properties from steps 1-d 

4. XiaomiGatewayV3HandlerFactory.java

    a. Add your new class to the createHandler method (based on ThingTypeUID from step 1-a)

