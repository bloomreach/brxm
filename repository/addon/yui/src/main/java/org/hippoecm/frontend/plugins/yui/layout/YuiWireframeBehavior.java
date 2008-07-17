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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.YuiHeaderContributor;

public class YuiWireframeBehavior extends AbstractBehavior implements IHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    //private static List<YuiLayoutConfig> configurations = Collections.synchronizedList(new ArrayList<YuiLayoutConfig>());

    private YuiWireframeConfig configuration;
    private Component component;

    public YuiWireframeBehavior() {
        this(null, false);
    }

    public YuiWireframeBehavior(boolean linkedWithParent) {
        this(null, linkedWithParent);
    }
    
    public YuiWireframeBehavior(String rootElementId) {
        this(rootElementId, false);
    }
    
    public YuiWireframeBehavior(String rootElementId, boolean linkedWithParent) {
        configuration = new YuiWireframeConfig(linkedWithParent);
        configuration.setRootElementId(rootElementId);
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

    public YuiWireframeBehavior addUnit(String position, String id) {
        Map<String, String> opts = new HashMap<String, String>();
        opts.put("id", id);
        configuration.addUnit(position, opts);
        return this;
    }
    
    
    @Override
    public void bind(Component component) {
        if(!MarkupContainer.class.isAssignableFrom(component.getClass())) {
            throw new RuntimeException("YuiWireframeBehavior can only be added to a MarkupContainer");
        }
        this.component = component;
        
        if(configuration.getRootElementId() == null) {
            configuration.setRootElementId(component.getMarkupId(true));
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        YuiHeaderContributor.forModule(HippoNamespace.NS, "layoutmanager").renderHead(response);

        fillConfig();
        TextTemplateHeaderContributor tthc = TextTemplateHeaderContributor.forJavaScript(YuiWireframeBehavior.class, "YuiWireframeBehavior.js",
                getHeaderContributorVariablesModel(configuration));
        TextTemplateHeaderContributor.forJavaScript(YuiWireframeBehavior.class, "YuiWireframeBehavior.js",
                getHeaderContributorVariablesModel(configuration)).renderHead(response);
        response.renderOnLoadJavascript("YAHOO.hippo.LayoutManager.onLoad()");
    }

    private void fillConfig() {
        if (configuration.isLinkedWithParent()) {
            Component parent = component;
            boolean found = false;
            while (!found) {
                parent = parent.getParent();
                if (parent == null) {
                    throw new RuntimeException("Parent layout behavior not found");
                }
                for (Iterator j = parent.getBehaviors().iterator(); j.hasNext();) {
                    Object parentBehavior = j.next();
                    if (YuiWireframeBehavior.class.isAssignableFrom(parentBehavior.getClass())) {
                        YuiWireframeBehavior parentWireframe = (YuiWireframeBehavior)parentBehavior;
                        configuration.setParentId(parentWireframe.getConfiguration().getRootElementId());
                        found = true;
                        break;
                    }
                }
            }
        }

        MarkupContainer cont = (MarkupContainer)component;
        cont.visitChildren(new IVisitor() {

            public Object component(Component component) {
                Component c = component;
                for (Iterator i = component.getBehaviors().iterator(); i.hasNext();) {
                    Object behavior = i.next();
                    if(YuiWireframeBehavior.class.isAssignableFrom(behavior.getClass())) {
                        return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    } else if (YuiUnitBehavior.class.isAssignableFrom(behavior.getClass())) {
                        YuiUnitBehavior unitBehavior = (YuiUnitBehavior) behavior;
                        unitBehavior.addUnit(configuration);
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
