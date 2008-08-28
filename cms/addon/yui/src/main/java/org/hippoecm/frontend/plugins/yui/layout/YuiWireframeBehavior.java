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
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;
import org.hippoecm.frontend.plugins.yui.layout.YuiWireframeConfig.Unit;
import org.hippoecm.frontend.plugins.yui.util.JavascriptUtil;

public class YuiWireframeBehavior extends AbstractDefaultAjaxBehavior implements IHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private YuiWireframeConfig configuration;

    public YuiWireframeBehavior() {
        this(null, false);
    }

    public YuiWireframeBehavior(boolean linkedWithParent) {
        this(null, linkedWithParent);
    }

    public YuiWireframeBehavior(String rootElementId, boolean linkedWithParent) {
        configuration = new YuiWireframeConfig(rootElementId, linkedWithParent);
    }

    public YuiWireframeBehavior registerUnitElement(String position, String elId) {
        configuration.registerUnitElement(position, elId);
        return this;
    }

    public YuiWireframeBehavior addUnit(String position, Map<String, String> options) {
        configuration.addUnit(position, options);
        return this;
    }

    public YuiWireframeBehavior addUnit(String position, String... options) {
        configuration.addUnit(position, options);
        return this;
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
                Unit u = configuration.getUnitByPosition(key);
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
        YuiHeaderContributor.forModule(HippoNamespace.NS, "layoutmanager").renderHead(response);

        fillConfig();
        TextTemplateHeaderContributor.forJavaScript(YuiWireframeBehavior.class, "YuiWireframeBehavior.js",
                getHeaderContributorVariablesModel(configuration)).renderHead(response);
        response.renderOnLoadJavascript("YAHOO.hippo.LayoutManager.onLoad()");
    }

    private void fillConfig() {
        if (configuration.isLinkedWithParent()) {
            Component parent = getComponent();
            boolean found = false;
            while (!found) {
                parent = parent.getParent();
                if (parent == null) {
                    throw new RuntimeException("Parent layout behavior not found");
                }
                for (Iterator j = parent.getBehaviors().iterator(); j.hasNext();) {
                    Object parentBehavior = j.next();
                    if (parentBehavior instanceof YuiWireframeBehavior) {
                        YuiWireframeBehavior parentWireframe = (YuiWireframeBehavior) parentBehavior;
                        configuration.setParentId(parentWireframe.getConfiguration().getRootElementId());
                        found = true;
                        break;
                    }
                }
            }
        }

        configuration.setBaseMarkupId(getComponent().getMarkupId(true));
        MarkupContainer cont = (MarkupContainer) getComponent();
        cont.visitChildren(new IVisitor() {
            public Object component(Component component) {
                for (Iterator i = component.getBehaviors().iterator(); i.hasNext();) {
                    Object behavior = i.next();
                    if (behavior instanceof YuiWireframeBehavior) {
                        return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    } else if (behavior instanceof YuiUnitBehavior) {
                        YuiUnitBehavior unitBehavior = (YuiUnitBehavior) behavior;
                        unitBehavior.addUnit(component, configuration, getComponent().getMarkupId(true));
                        return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    }
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });
    }

    private IModel getHeaderContributorVariablesModel(final YuiWireframeConfig config) {
        IModel variablesModel = new AbstractReadOnlyModel() {
            private static final long serialVersionUID = 1L;

            private Map<String, Object> variables;

            @Override
            public Object getObject() {
                if (variables == null) {
                    variables = config.getMap();
                    String callback = getCallbackUrl(false).toString();
                    variables.put("callbackUrl", JavascriptUtil.serialize2JS(callback));
                }
                return variables;
            }
        };
        return variablesModel;
    }

    public YuiWireframeConfig getConfiguration() {
        return configuration;
    }
}
