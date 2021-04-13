package org.openhab.binding.xiaomigatewayv3.internal.dto;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.thing.ThingTypeUID;

public class ThingDescription {
    @NonNull
    public String Manufacturer;

    @NonNull
    public String DeviceName;

    @NonNull
    public String DeviceModel;

    @NonNull
    public ThingTypeUID ThingTypeUid;

    public ThingDescription(String manufacturer, String deviceName, String deviceModel, ThingTypeUID thingTypeUid) {
        Manufacturer = manufacturer;
        DeviceName = deviceName;
        DeviceModel = deviceModel;
        ThingTypeUid = thingTypeUid;
    }
}

