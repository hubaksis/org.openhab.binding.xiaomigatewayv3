package org.openhab.binding.xiaomigatewayv3.internal.json;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class GatewayDeviceItem {    
    @SerializedName("did")
    @NonNull
    public String did = "";

    @SerializedName("model")
    @NonNull
    public String model = "";

    @SerializedName("num")
    @Nullable
    public Integer num;

    @SerializedName("total")
    @Nullable
    public Integer total;
}
