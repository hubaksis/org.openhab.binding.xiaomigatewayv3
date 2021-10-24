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

import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;

import java.util.List;

import java.util.Map.Entry;
import java.util.AbstractMap.SimpleEntry;

import org.openhab.binding.xiaomigatewayv3.internal.helpers.ChannelTypeUtil;
import org.openhab.binding.xiaomigatewayv3.internal.helpers.Helpers;
import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageReportParams;

import org.eclipse.jdt.annotation.Nullable;

/**
 * {@link AbstractXiaomiGatewayV3ThingHandler} is abstract base class for all xiaomi gateway v3 thing handlers .
 *
 * @author hubaksis - Initial contribution
 */

@NonNullByDefault
public class AbstractXiaomiGatewayV3ThingHandler extends BaseThingHandler {

    //private final Logger logger = LoggerFactory.getLogger(AbstractXiaomiGatewayV3Handler.class);

   //private @Nullable XiaomiGatewayV3Configuration config;

   //private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_DOOR_SENSOR);

   private static List<Entry<String, String>> GlobalParamsCollection = List.of(new SimpleEntry<String, String>("8.0.2001", GLOBAL_BATTERY_PERCENT),
                                                                        new SimpleEntry<String, String>("8.0.2002", GLOBAL_RESET_COUNT),
                                                                        new SimpleEntry<String, String>("8.0.2003", GLOBAL_SEND_ALL_COUNT),
                                                                        new SimpleEntry<String, String>("8.0.2004", GLOBAL_SEND_FAIL_COUNT),
                                                                        new SimpleEntry<String, String>("8.0.2005", GLOBAL_SEND_RETRY_COUNT),
                                                                        new SimpleEntry<String, String>("8.0.2006", GLOBAL_CHIP_TEMPERATURE),
                                                                        new SimpleEntry<String, String>("8.0.2008", GLOBAL_BATTERY_VOLTAGE),
                                                                        new SimpleEntry<String, String>("8.0.2010", GLOBAL_CUR_STATE),
                                                                        new SimpleEntry<String, String>("8.0.2011", GLOBAL_PREV_STATE),
                                                                        new SimpleEntry<String, String>("8.0.2013", GLOBAL_CCA)                                                                        
                                                                        );

                                                                            
    protected List<Entry<String, String>> LocalParamCollection = new java.util.ArrayList<>();


    public AbstractXiaomiGatewayV3ThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (DOOR_SENSOR_STATE.equals(channelUID.getId())) {
        //     if (command instanceof RefreshType) {
        //         // TODO: handle data refresh
        //     }

        //     // TODO: handle command

        //     // Note: if communication with thing fails for some reason,
        //     // indicate that by setting the status with detail information:
        //     // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        //     // "Could not control device at IP address x.x.x.x");
        // }

        // if (BATTERY_VOLTAGE.equals(channelUID.getId())) {
        //     if (command instanceof RefreshType) {
        //         // TODO: handle data refresh
        //     }

        //     // TODO: handle command

        //     // Note: if communication with thing fails for some reason,
        //     // indicate that by setting the status with detail information:
        //     // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        //     // "Could not control device at IP address x.x.x.x");
        // }

        // if (BATTERY_PERCENT.equals(channelUID.getId())) {
        //     if (command instanceof RefreshType) {
        //         // TODO: handle data refresh
        //     }

        //     // TODO: handle command

        //     // Note: if communication with thing fails for some reason,
        //     // indicate that by setting the status with detail information:
        //     // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
        //     // "Could not control device at IP address x.x.x.x");
        // }
    }

    @Override
    public void initialize() {
        //config = getConfigAs(XiaomiGatewayV3Configuration.class);

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
            boolean thingReachable = true; // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
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

    public void updateProperties(@Nullable List<ZigbeeSendMessageReportParams> params){
       fillParamsFromCollection(params, GlobalParamsCollection);

        if(LocalParamCollection != null && !LocalParamCollection.isEmpty())
            fillParamsFromCollection(params, LocalParamCollection);
    }

    private void fillParamsFromCollection(@Nullable List<ZigbeeSendMessageReportParams> params, List<Entry<String, String>> collection)
    {
        if(params == null)
            return;
            
        for (Entry<String,String> entry : collection) {
            if(Helpers.OptionExists(params, entry.getKey()))
                updateState(channel(entry.getValue()), ChannelTypeUtil.intToState(Helpers.GetValue(params, entry.getKey())));
        }
    }

    /**
     * Creates a {@link ChannelUID} from the given name.
     *
     * @param name channel name
     * @return {@link ChannelUID}
     */
    protected ChannelUID channel(String name) {
        return new ChannelUID(getThing().getUID(), name);
    }
}
