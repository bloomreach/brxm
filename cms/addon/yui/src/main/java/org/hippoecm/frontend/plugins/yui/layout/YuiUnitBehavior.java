package org.hippoecm.frontend.plugins.yui.layout;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.hippoecm.frontend.plugins.yui.util.OptionsUtil;

public class YuiUnitBehavior extends AbstractBehavior {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String position;
    private Map<String, String> options;
    private Component component;
    
    /**
     * Configure a LayoutUnit
     * @param position  Position in the wireframe
     * @param options   String array containing options in key=value scheme
     */
    public YuiUnitBehavior(String position, String... options) {
        this(null, position, null);
        OptionsUtil.addKeyValuePairsToMap(this.options, options);
    }
    
    public YuiUnitBehavior(String position, Map<String, String> options) {
        this(null, position, options);
    }

    public YuiUnitBehavior(String id, String position, Map<String, String> options) {
        this.id = id;
        this.position = position;
        if(options == null) {
            options = new HashMap<String, String>();
        }
        this.options = options;
    }
    
    @Override
    public void bind(Component component) {
        this.component = component;
    }
    
    public String addUnit(YuiWireframeConfig config) {
        String unitElementId = config.getUnitElement(position);
        String bodyId = (id == null) ? component.getMarkupId(true) : id;
        if(unitElementId == null) {
            options.put("id", bodyId);
        } else  {
            options.put("id", unitElementId);
            options.put("body", bodyId);
        }
        config.addUnit(position, options);
        return position;
    }
    
}
