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

 package org.openhab.binding.xiaomigatewayv3.internal.json;


import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * The {@link XiaomiGatewayV3Handler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author hubaksis - Initial contribution
 */


@NonNullByDefault
public class ZigbeeSendMessageHeartbeat extends ZigbeeSendMessageBase {
    /*
    {"cmd":"report","id":2000001433,"did":"lumi.158d000288cee1","dev_src":"0","time":1614293550982,"rssi":-79,"params":[{"res_name":"3.1.85","value":1}]}    
    */

    @SerializedName("params")
    @Nullable
    private List<ZigbeeSendMessageHeartbeatParams> params;
       
    @Nullable
    public ZigbeeSendMessageHeartbeatParams getFirstParam(){
        if(params == null || params.isEmpty())
            return null;

        return params.get(0);
    }

}
