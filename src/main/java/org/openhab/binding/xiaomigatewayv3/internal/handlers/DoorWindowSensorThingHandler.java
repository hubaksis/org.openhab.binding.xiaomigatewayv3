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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;


import java.util.AbstractMap.SimpleEntry;

/**
 * ThingHandler implementation for Aquara door and window sensor (MCCGQ01LM)
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public class DoorWindowSensorThingHandler extends AbstractXiaomiGatewayV3ThingHandler {

    //private final Logger logger = LoggerFactory.getLogger(AbstractXiaomiGatewayV3Handler.class);

    public DoorWindowSensorThingHandler(Thing thing) {
        super(thing);

        LocalParamCollection.add(new SimpleEntry<String, String>("3.1.85", DOOR_SENSOR_STATE));
    }    

    // @Override
    // public void updatePropertiesAsReport(ZigbeeSendMessageReport message){
    //     if(message.OptionExists("3.1.85"))
    //         updateState(channel(DOOR_SENSOR_STATE), ChannelTypeUtil.intToState(message.GetValue("3.1.85")));

    //     updateState(channel("blablabla"), ChannelTypeUtil.intToState(message.GetValue("3.1.85")));
    // }

}
