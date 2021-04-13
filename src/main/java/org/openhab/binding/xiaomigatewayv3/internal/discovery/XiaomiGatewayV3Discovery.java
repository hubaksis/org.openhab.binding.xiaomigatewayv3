package org.openhab.binding.xiaomigatewayv3.internal.discovery;

import static org.openhab.binding.xiaomigatewayv3.internal.XiaomiGatewayV3BindingConstants.*;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;

import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;

import org.openhab.binding.xiaomigatewayv3.internal.miio.cloud.CloudConnector;
import org.openhab.binding.xiaomigatewayv3.internal.handlers.XiaomiGatewayV3BridgeHandler;

import org.openhab.binding.xiaomigatewayv3.internal.json.GatewayDevicesList;
import org.openhab.binding.xiaomigatewayv3.internal.json.GatewayDeviceItem;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openhab.binding.xiaomigatewayv3.internal.dto.ThingDescription;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map.Entry;


@NonNullByDefault
@Component(service = DiscoveryService.class, configurationPid = "discovery.xiaomigatewayv3")
public class XiaomiGatewayV3Discovery extends AbstractDiscoveryService 
                                    implements ThingHandlerService 
                                    {
    private static final int DISCOVERY_TIME = 10;
    private static final long SEARCH_INTERVAL = 600;

    private final Logger logger = LoggerFactory.getLogger(XiaomiGatewayV3Discovery.class);

    @Nullable
    private CloudConnector cloudConnector;
    private static final String DISABLED = "disabled";
    
    private @Nullable ScheduledFuture<?> xiaomiGatewayDiscoveryJob;

    //private @Nullable Configuration config;

    //protected @Nullable MIIOCommunication miioComm;

    @Nullable
    protected XiaomiGatewayV3BridgeHandler bridgeHandler;


    // @Activate
    // public XiaomiGatewayV3Discovery(@Reference CloudConnector cloudConnector)
    //         throws IllegalArgumentException {
    //     super(DISCOVERY_TIME);
    //     this.cloudConnector = cloudConnector;
    //     // try {
    //     //     config = configAdmin.getConfiguration("binding.xiaomigatewayv3");
    //     // } catch (IOException | SecurityException e) {
    //     //     logger.debug("Error getting configuration: {}", e.getMessage());
    //     // }
    // }

    @Activate
    public XiaomiGatewayV3Discovery(){
        super(DISCOVERY_TIME);    
    }

    private String getCloudDiscoveryMode() {
        // if (miioConfig != null) {
        //     try {
        //         Dictionary<String, @Nullable Object> properties = miioConfig.getProperties();
        //         String cloudDiscoveryModeConfig;
        //         if (properties == null) {
        //             cloudDiscoveryModeConfig = DISABLED;
        //         } else {
        //             cloudDiscoveryModeConfig = (String) properties.get("cloudDiscoveryMode");
        //             if (cloudDiscoveryModeConfig == null) {
        //                 cloudDiscoveryModeConfig = DISABLED;
        //             } else {
        //                 cloudDiscoveryModeConfig = cloudDiscoveryModeConfig.toLowerCase();
        //             }
        //         }
        //         return Set.of(SUPPORTED, ALL).contains(cloudDiscoveryModeConfig) ? cloudDiscoveryModeConfig : DISABLED;
        //     } catch (ClassCastException | SecurityException e) {
        //         logger.debug("Error getting cloud discovery configuration: {}", e.getMessage());
        //     }
        // }
        return DISABLED;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypes() {
        return SUPPORTED_THING_TYPES_UIDS;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Start Xiaomi GatewayV3 background discovery with cloudDiscoveryMode: {}", getCloudDiscoveryMode());
        final @Nullable ScheduledFuture<?> xiaomiGatewayDiscoveryJob = this.xiaomiGatewayDiscoveryJob;
        if (xiaomiGatewayDiscoveryJob == null || xiaomiGatewayDiscoveryJob.isCancelled()) {
            this.xiaomiGatewayDiscoveryJob = scheduler.scheduleWithFixedDelay(this::discover, 0, SEARCH_INTERVAL,
                    TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop Xiaomi GatewayV3 background discovery");
        final @Nullable ScheduledFuture<?> xiaomiGatewayDiscoveryJob = this.xiaomiGatewayDiscoveryJob;
        if (xiaomiGatewayDiscoveryJob != null) {
            xiaomiGatewayDiscoveryJob.cancel(true);
            this.xiaomiGatewayDiscoveryJob = null;
        }
    }

    @Override
    public void deactivate() {       
        super.deactivate();
    }

    @Override
    protected void startScan() {
        logger.info("AutoDiscovery: StartScan...");
        discover();       
    }

    private void discover() {        
        logger.info("AutoDiscovery: Discover...");

        if(bridgeHandler != null){
            logger.info("Bridge not null");
            bridgeHandler.sendRequestForDeviceList();
            // var config = bridgeHandler.getThing().getConfiguration().get(PROPERTY_HOST_IP);
            // if(config != null){
            //     logger.info("AutoDiscovery host: {}", config.toString());            
            // }
        }
    }


    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof XiaomiGatewayV3BridgeHandler) {
            bridgeHandler = (XiaomiGatewayV3BridgeHandler) handler;
            logger.info("Class set");
            bridgeHandler.setDiscoveryService(this);
        } else
        {
            if(bridgeHandler != null)
                logger.info("Class: {}", bridgeHandler.getClass());
            else
                logger.info("Class - null");
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    public void createDisvoceryResult(GatewayDevicesList deviceList) {
        logger.info("AutoDiscovery: creating {} devices", deviceList.result.size());

        for (GatewayDeviceItem device : deviceList.result) {
            ThingDescription t = getThingByModelName(device.model);
            if(t == null)
                logger.info("AutoDiscovery: model {} is not yet supported", device.model);
            else{
                String uid = device.did.replace(".", "_");
                ThingUID thingUid = new ThingUID(t.ThingTypeUid, bridgeHandler.getThing().getUID(), uid);

                DiscoveryResultBuilder discoveryResultBuilder = DiscoveryResultBuilder.create(thingUid)
                    .withBridge(bridgeHandler.getThing().getUID()).withRepresentationProperty(THING_DEVICE_ID)
                    .withProperty(THING_DEVICE_ID, device.did)
                    .withProperty(THING_MANUFACTURER, t.Manufacturer)
                    .withProperty(THING_NAME, t.DeviceName)
                    .withProperty(THING_MODEL, t.DeviceModel)
                    .withProperty(THING_INTERNAL_MODEL, device.model)
                    .withLabel(t.DeviceName);
                
                DiscoveryResult result = discoveryResultBuilder.build();

                thingDiscovered(result);

            }
        }
    }


    @Nullable
    private ThingDescription getThingByModelName(@Nullable String model){
        List<Entry<String, ThingDescription>> filtered = ThingDescriptions.stream().filter(x -> x.getKey().equals(model)).collect(Collectors.toList());
        
        if(filtered.isEmpty())
            return null;
        else
            return filtered.get(0).getValue();        
    }
}
