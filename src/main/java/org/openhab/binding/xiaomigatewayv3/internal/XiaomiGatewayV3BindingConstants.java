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
package org.openhab.binding.xiaomigatewayv3.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.OpenHAB;
import java.io.File;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.List;
import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;
import org.openhab.binding.xiaomigatewayv3.internal.dto.ThingDescription;

/**
 * The {@link XiaomiGatewayV3BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public final class XiaomiGatewayV3BindingConstants {

    public static final String BINDING_ID = "xiaomigatewayv3";

    public static final String BRIDGE_TYPE_ID = "config";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_ID);

    // Aquara door and window sensor (MCCGQ01LM)
    public static final ThingTypeUID THING_TYPE_DOOR_SENSOR = new ThingTypeUID(BINDING_ID, "door_sensor");    
    // Motion sensor with illuminance (RTCGQ11LM)
    public static final ThingTypeUID THING_TYPE_MOTION_SENSOR_WITH_LUX = new ThingTypeUID(BINDING_ID, "motion_sensor_with_illuminance");    
    // Xiaomi light sendor GZCGQ01LM
    public static final ThingTypeUID THING_TYPE_LIGHT_SENSOR = new ThingTypeUID(BINDING_ID, "light_sensor");    

   // Motion sensor (RTCGQ01LM)
   public static final ThingTypeUID THING_TYPE_MOTION_SENSOR = new ThingTypeUID(BINDING_ID, "motion_sensor");

   public static final ThingTypeUID THING_TYPE_VIBRATION_SENSOR = new ThingTypeUID(BINDING_ID, "vibration_sensor");

   public static final ThingTypeUID THING_TYPE_WATER_LEAK_SENSOR = new ThingTypeUID(BINDING_ID, "water_leak_sensor");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_BRIDGE, THING_TYPE_DOOR_SENSOR, THING_TYPE_MOTION_SENSOR_WITH_LUX,
                                    THING_TYPE_LIGHT_SENSOR, THING_TYPE_MOTION_SENSOR, THING_TYPE_VIBRATION_SENSOR,
                                    THING_TYPE_WATER_LEAK_SENSOR)
                    .collect(Collectors.toSet()));
                    
    //# All lumi models:
    //#   https://github.com/rytilahti/python-miio/issues/699#issuecomment-643208618
    //# Zigbee Model: [Manufacturer, Device Name, Device Model, ThingTypeUID]
    public static final List<Entry<String, ThingDescription>> ThingDescriptions = List.of(
            //door window sensor
            new SimpleEntry<String, ThingDescription>("lumi.sensor_magnet", new ThingDescription("Xiaomi", "Door Sensor", "MCCGQ01LM", THING_TYPE_DOOR_SENSOR)),
            new SimpleEntry<String, ThingDescription>("lumi.sensor_magnet.v2", new ThingDescription("Xiaomi", "Door Sensor", "MCCGQ01LM", THING_TYPE_DOOR_SENSOR)),
            new SimpleEntry<String, ThingDescription>("lumi.sensor_magnet.aq2", new ThingDescription("Aqara", "Door Sensor", "MCCGQ11LM", THING_TYPE_DOOR_SENSOR)),            

            //motion sensor with illuminance
            new SimpleEntry<String, ThingDescription>("lumi.sensor_motion.aq2", new ThingDescription("Aqara", "Motion Sensor", "RTCGQ11LM", THING_TYPE_MOTION_SENSOR_WITH_LUX)),
            //
            new SimpleEntry<String, ThingDescription>("lumi.sen_ill.mgl01", new ThingDescription("Xiaomi", "Light Sensor", "GZCGQ01LM", THING_TYPE_LIGHT_SENSOR)), 

            new SimpleEntry<String, ThingDescription>("lumi.sensor_motion", new ThingDescription("Xiaomi", "Motion Sensor", "RTCGQ01LM", THING_TYPE_MOTION_SENSOR)), 
            
            //vibration sensor DJT11LM
            new SimpleEntry<String, ThingDescription>("lumi.vibration.aq1", new ThingDescription("Aqara", "Vibration Sensor", "DJT11LM", THING_TYPE_VIBRATION_SENSOR)),

            new SimpleEntry<String, ThingDescription>("lumi.sensor_wleak.aq1", new ThingDescription("Aqara", "Water Leak Sensor", "SJCGQ11LM", THING_TYPE_WATER_LEAK_SENSOR))
            );    

    
    //miio
    public static final String BINDING_USERDATA_PATH = OpenHAB.getUserDataFolder() + File.separator
    + XiaomiGatewayV3BindingConstants.BINDING_ID;
    public static final byte[] DISCOVER_STRING = org.openhab.binding.xiaomigatewayv3.internal.miio.Utils
            .hexStringToByteArray("21310020ffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
    public static final int PORT = 54321;
    public static final Set<String> IGNORED_TOKENS = Collections.unmodifiableSet(Stream
    .of("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", "00000000000000000000000000000000").collect(Collectors.toSet()));

    // Bringe config ids    
    public static final String PROPERTY_HOST_IP = "host";
    public static final String PROPERTY_DID = "deviceId";
    public static final String PROPERTY_TOKEN = "token";
    public static final String PROPERTY_MODEL = "model";
    public static final String PROPERTY_REFRESH_INTERVAL = "refreshInterval";
    public static final String PROPERTY_TIMEOUT = "timeout";
    public static final String PROPERTY_CLOUDSERVER = "cloudServer";


    // Thing config ids
    public static final String THING_DEVICE_ID = "deviceId";
    public static final String THING_MANUFACTURER = "manufacturer";
    public static final String THING_NAME = "deviceName";
    public static final String THING_MODEL = "deviceModel";
    public static final String THING_INTERNAL_MODEL = "internalModel";

    // List of all Channel ids
    //Bridge
    public static final String BRIDGE_HEARTBEAT = "bridge_heartbeat";
    public static final String BRIDGE_PAIRING_MODE = "bridge_pairing_mode_command";

    //Global Zigbee devices properties
    //source: https://github.com/AlexxIT/XiaomiGateway3/blob/master/custom_components/xiaomi_gateway3/core/utils.py
    public static final String GLOBAL_BATTERY_PERCENT = "global_battery_percent";   //8.0.2001
    public static final String GLOBAL_RESET_COUNT = "global_reset_cnt";             //8.0.2002
    public static final String GLOBAL_SEND_ALL_COUNT = "global_send_all_cnt";       //8.0.2003
    public static final String GLOBAL_SEND_FAIL_COUNT = "global_send_fail_cnt";     //8.0.2004
    public static final String GLOBAL_SEND_RETRY_COUNT = "global_send_retry_cnt";   //8.0.2005
    public static final String GLOBAL_CHIP_TEMPERATURE = "global_chip_temperature"; //8.0.2006
    public static final String GLOBAL_BATTERY_VOLTAGE = "global_battery_voltage";   //8.0.2008
    
    public static final String GLOBAL_CUR_STATE = "global_cur_state";               //8.0.2010
    public static final String GLOBAL_PREV_STATE = "global_pre_state";              //8.0.2011
    public static final String GLOBAL_CCA = "global_CCA";                           //8.0.2013


    //Specific sensor channels
    //for Aquara door and window sensor (MCCGQ01LM)
    public static final String DOOR_SENSOR_STATE = "door_sensor_state";             //3.1.85

    //for Motion sensor with illuminance (RTCGQ11LM)
    public static final String MOTION_SENSOR_STATE = "motion_sensor_state";         //3.1.85
    public static final String ILLUMINANCE_LUX = "illuminance_lux";                 //0.3.85
    public static final String ILLUMINANTION = "illumination";                      //0.4.85

    //for light sensor (GZCGQ01LM)
    public static final String LIGHT_SENSOR_ILLUMINANCE = "light_sensor_illuminance";       //2.1
    public static final String LIGHT_SENSOR_BATTERY = "light_sensor_battery";               //3.1


    //for vibration sensor (DJT11LM)
    public static final String VIBRATION_SENSOR_BED_ACTIVIRY = "vibration_sensor_bed_activity";              //0.1.85
    public static final String VIBRATION_SENSOR_TILT_ANGLE = "vibration_sensor_tilt_angle";                  //0.2.85
    public static final String VIBRATION_SENSOR_VIBRATE_INTENSITY = "vibration_sensor_vibrate_intensity";    //0.3.85
    public static final String VIBRATION_SENSOR_VIBRATION = "vibration_sensor_vibration";                    //13.1.85
    public static final String VIBRATION_SENSOR_VIBRATION_LEVEL = "vibration_sensor_vibration_level";        //14.1.85

    //for water leak sensor (SJCGQ11LM)
    public static final String WATER_LEAK_SENSOR_ALARM = "water_leak_sensor_alarm";                     // 3.1.85
}
