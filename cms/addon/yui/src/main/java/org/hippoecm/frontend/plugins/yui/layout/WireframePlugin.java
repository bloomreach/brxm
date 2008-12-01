package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.service.IBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WireframePlugin extends WireframeBehavior implements IPlugin, IBehaviorService, IDetachable {
    private static final long serialVersionUID = 1L;
    
    private final static Logger log = LoggerFactory.getLogger(WireframePlugin.class);

    public static final String UNITS = "yui.units";
    public static final String WRAPPERS = "yui.wrappers";
    public static final String ROOT = "yui.root";
    public static final String LINKED = "yui.linked";
    public static final String CLIENT_CLASSNAME = "yui.classname";

    
    private IPluginConfig config;
    
    public WireframePlugin(IPluginContext context, IPluginConfig config) {
        super(YuiPluginHelper.getManager(context), new WireframeSettings(YuiPluginHelper.getConfig(config)));
        
//        String[] units = config.getStringArray(UNITS);
//        if (units != null) {
//            for (String unit : units) {
//                String serialized = config.getString(unit);
//                ValueMap map;
//                if (serialized != null) {
//                    map = new ValueMap(config.getString(unit));
//                } else {
//                    map = new ValueMap();
//                    log.warn("No config found for unit {}", unit);
//                }
//                addUnit(unit, map);
//            }
//        }
//
//        String[] wrappers = config.getStringArray(WRAPPERS);
//        if (wrappers != null) {
//            for (String position : wrappers) {
//                registerUnitElement(position, config.getString(position));
//            }
//        }
//
//        if (units == null && wrappers == null) {
//            log.warn("No units defined");
//        }
        
        this.config = config;
        context.registerService(this, config.getString(ID));
    }
    
    public String getComponentPath() {
        return config.getString(IBehaviorService.PATH);
    }

    public void detach() {
        config.detach();
    }

}
