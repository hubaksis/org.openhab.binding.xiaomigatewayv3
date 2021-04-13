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
 * ThingHandler implementation for Xiaomi Light Sensor GZCGQ01LM
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
public class LightSensorThingHandler extends AbstractXiaomiGatewayV3ThingHandler {

    public LightSensorThingHandler(Thing thing) {
        super(thing);

        LocalParamCollection.add(new SimpleEntry<String, String>("2.1", LIGHT_SENSOR_ILLUMINANCE));
        LocalParamCollection.add(new SimpleEntry<String, String>("3.1", LIGHT_SENSOR_BATTERY));
    }    
}
