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

import static org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;

import org.openhab.binding.xiaomigatewayv3.internal.handlers.XiaomiGatewayV3BridgeHandler;
import org.openhab.binding.xiaomigatewayv3.internal.handlers.DoorWindowSensorThingHandler;
import org.openhab.binding.xiaomigatewayv3.internal.handlers.MotionSensorWithLuxThingHandler;

import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.CloudConnector;
import org.osgi.service.component.annotations.Reference;
import java.util.concurrent.ScheduledExecutorService;
import org.openhab.core.common.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import java.util.Map;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

/**
 * The {@link XiaomiGatewayV3HandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author hubaksis - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.xiaomigatewayv3", service = ThingHandlerFactory.class)
public class XiaomiGatewayV3HandlerFactory extends BaseThingHandlerFactory {

    //private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String THING_HANDLER_THREADPOOL_NAME = "thingHandler";
    protected final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(THING_HANDLER_THREADPOOL_NAME);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_DOOR_SENSOR, THING_TYPE_MOTION_SENSOR_WITH_LUX);

    private CloudConnector cloudConnector;

    @Activate
    public XiaomiGatewayV3HandlerFactory(@Reference CloudConnector cloudConnector, Map<String, Object> properties) {
        this.cloudConnector = cloudConnector;
        @Nullable
        String username = (String) properties.get("username");
        @Nullable
        String password = (String) properties.get("password");
        @Nullable
        String country = (String) properties.get("country");
        cloudConnector.setCredentials(username, password, country);
        scheduler.submit(() -> cloudConnector.isConnected());
        // this.channelTypeRegistry = channelTypeRegistry;
        // this.basicChannelTypeProvider = basicChannelTypeProvider;
    }


    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID); 
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();                

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) { 
            return new XiaomiGatewayV3BridgeHandler((Bridge) thing, cloudConnector);
        } else if (thingTypeUID.equals(THING_TYPE_DOOR_SENSOR)) {
            return new DoorWindowSensorThingHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_MOTION_SENSOR_WITH_LUX)) {
            return new MotionSensorWithLuxThingHandler(thing);
        }

        return null;
    }
}
