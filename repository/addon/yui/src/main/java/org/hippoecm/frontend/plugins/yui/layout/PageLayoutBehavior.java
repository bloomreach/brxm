package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.JavascriptSettings;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;

public class PageLayoutBehavior extends AbstractYuiAjaxBehavior implements IWireframeService  {

    private static final long serialVersionUID = 1L;
        
    private static final PackagedTextTemplate INIT_PAGE = new PackagedTextTemplate(PageLayoutBehavior.class,
    "init_page.js");
    
    private String rootElementId;
    private int headerHeight;

    public PageLayoutBehavior(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        IPluginConfig yuiConfig = config.getPluginConfig("yui.config");
        rootElementId = yuiConfig.getString("root.element.id", "doc3");
        headerHeight = yuiConfig.getInt("header.height", 0);
    }
    
    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addModule(HippoNamespace.NS, "layoutmanager");
        helper.addTemplate(new HippoTextTemplate(INIT_PAGE, "YAHOO.hippo.GridsRootWireframe"){
            private static final long serialVersionUID = 1L;
            
            @Override
            public String getId() {
                return rootElementId;
            }

            @Override
            public JavascriptSettings getJavascriptSettings() {
                JavascriptSettings settings = new JavascriptSettings();
                settings.put("headerHeight", headerHeight);
                return settings;
            }
        });
        helper.addOnload("YAHOO.hippo.LayoutManager.render()");
    }
    
    @Override
    protected void respond(AjaxRequestTarget target) {
    }

    public String getParentId() {
        return rootElementId;
    }

}
