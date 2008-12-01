package org.hippoecm.frontend.plugins.yui.layout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.Settings;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class PageLayoutBehavior extends AbstractYuiAjaxBehavior implements IWireframeService  {

    private static final long serialVersionUID = 1L;
        
    private static final PackagedTextTemplate INIT_PAGE = new PackagedTextTemplate(PageLayoutBehavior.class,
    "init_page.js");
    
    private PageLayoutSettings settings;
    
    public PageLayoutBehavior(IYuiManager manager, PageLayoutSettings settings) {
        super(manager, settings);
        this.settings = settings;
    }
    
    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addTemplate(new HippoTextTemplate(INIT_PAGE, "YAHOO.hippo.GridsRootWireframe"){
            private static final long serialVersionUID = 1L;
            
            @Override
            public String getId() {
                return settings.getRootId();
            }

            @Override
            public Settings getSettings() {
                return settings;
            }
        });
        context.addOnload("YAHOO.hippo.LayoutManager.render()");
    }
    
    @Override
    protected void respond(AjaxRequestTarget target) {
    }
    
    
    //Implement IWireframeService
    public String getParentId() {
        return settings.getRootId();
    }

}
