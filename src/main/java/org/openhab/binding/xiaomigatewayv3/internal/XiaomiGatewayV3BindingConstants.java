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

/**
 * The {@link XiaomiGatewayV3BindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public class XiaomiGatewayV3BindingConstants {

    private static final String BINDING_ID = "xiaomigatewayv3";

    public static final String BRIDGE_TYPE_ID = "config";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, BRIDGE_TYPE_ID);

    // Aquara door and window sensor (MCCGQ01LM)
    public static final ThingTypeUID THING_TYPE_DOOR_SENSOR = new ThingTypeUID(BINDING_ID, "door_sensor");    

    // Motion sensor with illuminance (RTCGQ11LM)
    public static final ThingTypeUID THING_TYPE_MOTION_SENSOR_WITH_LUX = new ThingTypeUID(BINDING_ID, "motion_sensor_with_illuminamce");    




    // Bringe config ids
    public static final String GATEWAY_IP_ADDRESS = "gateway_ip_address";

    // Thing config ids
    public static final String THING_DEVICE_ID = "deviceId";

    // List of all Channel ids
    //Bridge
    public static final String BRIDGE_HEARTBEAT = "bridge_heartbeat";

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


    //Sensor channels
    public static final String DOOR_SENSOR_STATE = "door_sensor_state";

    
    public static final String MOTION_SENSOR_STATE = "motion_sensor_state";
    public static final String ILLUMINANCE_LUX = "illuminance_lux";
    public static final String ILLUMINANTION = "illumination";
}
