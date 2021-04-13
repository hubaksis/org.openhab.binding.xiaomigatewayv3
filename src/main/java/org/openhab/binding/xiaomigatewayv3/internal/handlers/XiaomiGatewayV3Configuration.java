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


/**
 * The {@link XiaomiGatewayV3Configuration} class contains fields mapping thing configuration parameters.
 *
 * @author hubaksis - Initial contribution
 * 
 * Code copied from miio binding
 */

@SuppressWarnings("null")
public class XiaomiGatewayV3Configuration {
    public String host;
    public String token;
    public String deviceId;    
    public String model; 
    public String communication; 
    public int refreshInterval;
    public int timeout; 
    public String cloudServer; 
}
