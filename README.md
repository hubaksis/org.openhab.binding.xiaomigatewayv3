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

Works with openhab 3.2.0

## Supported Things

- Xiaomi/Aquara Door Sensor (MCCGQ01LM)
- Aqara Motion Sensor (RTCGQ11LM)
- Xiaomi Light Sensor (GZCGQ01LM)
- Xaiomi Motion Sensor (RTCGQ01LM)
- Aqara Vibration Sensor (DJT11LM)
- Aqara Water Leak Sensor (SJCGQ11LM)
- Honeywell Smoke Sensor (JTYJ-GD-01LM/BW)

## Discovery

The binding can find attached supported ZigBee devices now.
The binding cannot find gateway itself.
The binding cannot find bluetooth devices.

## Binding Configuration

1. Configure the gateway as discribed here: https://github.com/AlexxIT/XiaomiGateway3

2. Create a bridge, set up IP address, token and device ID. The binding automatically turns on telnet and remote MQTT access. 

Important checkpoint after this step: you should be able to connect to the gateway with a MQTT client (for example - [MQTT Explorer](http://mqtt-explorer.com/)) and see incoming messages in zigbee/send queue.

3. The gateway bridge should be online. 


## Automatic Thing Configuration

Connect a sensor to the gateway using MiHome app.

Things -> (+) -> Select the gateway binding -> Press Scan

All supported devices should be added to your Inbox


## Channels

### Binding itself

| channel  | type   | description                  |
|----------|--------|------------------------------|
| Bridge heartbeat message  | String | (not used yet)  |
| Trigger pairing mode  | String | Activates gateway pairing mode  |


### Motion sensor
| channel  | type   | description                  |
|----------|--------|------------------------------|
| Sensor state  | Contact | With the motions sensor there is a hint. It sends only when the movement happened (will the state OPEN), however it doesn't send state CLOSED. So it's better to create an expiration timer (in metadata) on a item |

### Water leak sensor
| channel  | type   | description                  |
|----------|--------|------------------------------|
| Sensor state  | Contact | Sends OPEN when the contacts are closed, sends CLOSED when the contact are opened again |

### TODO everything else...


## Any custom content here!


To compile - I use the command: mvn clean install -D"spotless.check.skip"=true -DskipChecks

## (For developers) to add support of a new ZigBee device

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

