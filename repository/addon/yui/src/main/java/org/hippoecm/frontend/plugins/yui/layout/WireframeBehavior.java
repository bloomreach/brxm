/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.layout;

import java.util.Iterator;
import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.AbstractYuiAjaxBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.JavascriptSettings;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.layout.WireframeSettings.Unit;

public class WireframeBehavior extends AbstractYuiAjaxBehavior implements IWireframeService {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final PackagedTextTemplate behaviorJs = new PackagedTextTemplate(WireframeBehavior.class, "add_wireframe.js");

    public WireframeSettings settings;
    private boolean initialized = false;
    
    public WireframeBehavior(IPluginContext context, IPluginConfig config, WireframeSettings settings) {
        super(context, config);
        this.settings = settings;
    }

    public WireframeBehavior registerUnitElement(String position, String elId) {
        settings.registerUnitElement(position, elId);
        return this;
    }

    public WireframeBehavior addUnit(String position, Map<String, String> options) {
        settings.addUnit(position, options);
        return this;
    }

    public WireframeBehavior addUnit(String position, String... options) {
        settings.addUnit(position, options);
        return this;
    }
    
    public WireframeSettings getConfiguration() {
        return settings;
    }

    public String getParentId() {
        return settings.getRootElementId();
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        RequestCycle requestCycle = RequestCycle.get();
        String sizes = requestCycle.getRequest().getParameter("sizes");
        if (sizes != null) {
            JSONObject json = JSONObject.fromObject(sizes);
            Iterator<String> i = json.keys();
            while (i.hasNext()) {
                String key = i.next();
                Unit u = settings.getUnitByPosition(key);
                if (u != null) {
                    u.addOption("width", json.getJSONObject(key).getString("w"));
                    u.addOption("height", json.getJSONObject(key).getString("h"));
                }
            }
        }
    }

    @Override
    protected void onBind() {
        super.onBind();
        if (!(getComponent() instanceof MarkupContainer)) {
            throw new RuntimeException("YuiWireframeBehavior can only be added to a MarkupContainer");
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        if (!initialized) {
            initialize();    
        }
        super.renderHead(response);
    }
    
    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addTemplate(new HippoTextTemplate(behaviorJs, settings.getClientClassName()){
            private static final long serialVersionUID = 1L;
            
            @Override
            public String getId() {
                return settings.getRootElementId();
            }

            @Override
            public JavascriptSettings getJavascriptSettings() {
                return settings.getSettings();
            }
        });
        helper.addOnload("YAHOO.hippo.LayoutManager.render()");
    }

    private void initialize() {
        if (settings.isLinkedWithParent()) {
            Component parent = getComponent();
            boolean found = false;
            while (!found) {
                parent = parent.getParent();
                if (parent == null) {
                    throw new RuntimeException("Parent layout behavior not found");
                }
                for (Iterator j = parent.getBehaviors().iterator(); j.hasNext();) {
                    Object parentBehavior = j.next();
                    if (parentBehavior instanceof IWireframeService) {
                        IWireframeService service = (IWireframeService) parentBehavior;
                        settings.setParentId(service.getParentId());
                        found = true;
                        break;
                    }
                }
            }
        }

        settings.setBaseMarkupId(getComponent().getMarkupId(true));
        MarkupContainer cont = (MarkupContainer) getComponent();
        cont.visitChildren(new IVisitor() {
            public Object component(Component component) {
                for (Iterator i = component.getBehaviors().iterator(); i.hasNext();) {
                    Object behavior = i.next();
                    if (behavior instanceof IWireframeService) {
                        return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    } else if (behavior instanceof UnitBehavior) {
                        UnitBehavior unitBehavior = (UnitBehavior) behavior;
                        unitBehavior.addUnit(component, settings, getComponent().getMarkupId(true));
                        return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    }
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
        initialized = true;
    }
    
}
