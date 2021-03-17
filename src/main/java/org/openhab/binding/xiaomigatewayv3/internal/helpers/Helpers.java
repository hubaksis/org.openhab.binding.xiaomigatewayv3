package org.openhab.binding.xiaomigatewayv3.internal.helpers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;



import org.eclipse.jdt.annotation.Nullable;

import org.openhab.binding.xiaomigatewayv3.internal.json.ZigbeeSendMessageReportParams;

public class Helpers {
    @Nullable
    private static ZigbeeSendMessageReportParams GetOption(List<ZigbeeSendMessageReportParams> params, String paramName){
        if(params == null || params.isEmpty())
            return null;
            
        List<ZigbeeSendMessageReportParams> filtered = params.stream().filter(x -> x.Name.equals(paramName)).collect(Collectors.toList());
        
        if(filtered.isEmpty())
            return null;
        else
            return filtered.get(0);        
    }
    

    public static boolean OptionExists(List<ZigbeeSendMessageReportParams> params, String paramName){
        return GetOption(params, paramName) != null;
    }

    public static Optional<Integer> GetValue(List<ZigbeeSendMessageReportParams> params, String paramName){
        ZigbeeSendMessageReportParams option = GetOption(params, paramName);
        if(option == null)
            return Optional.empty();
        else 
            return Optional.ofNullable(option.Value);
    }    

}
