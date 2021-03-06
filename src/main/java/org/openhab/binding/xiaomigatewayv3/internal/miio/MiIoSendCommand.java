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
package org.openhab.binding.xiaomigatewayv3.internal.miio;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Commands to be send
 *
 * @author Marcel Verpaalen - Initial contribution
 */
@NonNullByDefault
public class MiIoSendCommand {

    private final int id;
    private final MiIoCommand command;
    private final JsonObject commandJson;
    private @Nullable JsonObject response;
    private String cloudServer = "";

    public void setResponse(JsonObject response) {
        this.response = response;
    }

    public MiIoSendCommand(int id, MiIoCommand command, JsonObject fullCommand) {
        this.id = id;
        this.command = command;
        this.commandJson = fullCommand;
    }

    public MiIoSendCommand(int id, MiIoCommand command, JsonObject fullCommand, String cloudServer) {
        this.id = id;
        this.command = command;
        this.commandJson = fullCommand;
        this.cloudServer = cloudServer;
    }

    public int getId() {
        return id;
    }

    public MiIoCommand getCommand() {
        return command;
    }

    public JsonObject getCommandJson() {
        return commandJson;
    }

    public String getCommandString() {
        return commandJson.toString();
    }

    public String getMethod() {
        return commandJson.has("method") ? commandJson.get("method").getAsString() : "";
    }

    public JsonElement getParams() {
        return commandJson.has("params") ? commandJson.get("params").getAsJsonArray() : new JsonArray();
    }

    public JsonObject getResponse() {
        final @Nullable JsonObject response = this.response;
        return response != null ? response : new JsonObject();
    }

    public boolean isError() {
        final @Nullable JsonObject response = this.response;
        if (response != null) {
            return response.has("error");
        }
        return true;
    }

    public JsonElement getResult() {
        final @Nullable JsonObject response = this.response;
        if (response != null && response.has("result")) {
            return response.get("result");
        }
        return new JsonObject();
    }

    public String getCloudServer() {
        return cloudServer;
    }

    public void setCloudServer(String cloudServer) {
        this.cloudServer = cloudServer;
    }
}
