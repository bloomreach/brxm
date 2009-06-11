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

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Component.IVisitor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;
import org.hippoecm.frontend.plugins.yui.javascript.IYuiListener;
import org.hippoecm.frontend.plugins.yui.javascript.YuiId;
import org.hippoecm.frontend.plugins.yui.javascript.YuiObject;
import org.hippoecm.frontend.plugins.yui.webapp.IYuiManager;

public class WireframeBehavior extends AbstractYuiBehavior implements IWireframeService {

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    private static final PackagedTextTemplate behaviorJs = new PackagedTextTemplate(WireframeBehavior.class,
            "add_wireframe.js");

    private WireframeSettings settings;
    private Component component;
    private HippoTextTemplate template;

    public WireframeBehavior(IYuiManager manager, final WireframeSettings settings) {
        super(manager);
        this.settings = settings;
        this.settings.addListener(new IYuiListener() {
            private static final long serialVersionUID = 1L;

            public void onEvent(Event event) {
                if (component != null) {
                    AjaxRequestTarget target = AjaxRequestTarget.get();
                    if (target != null) {
                        target.addComponent(component);
                    }
                }
            }

        });

        template = new HippoTextTemplate(behaviorJs, settings.getClientClassName()) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return settings.getRootElementId().getElementId();
            }

            @Override
            public YuiObject getSettings() {
                return settings;
            }
        };
    }

    /**
     * Implements IWireframeService
     */
    public YuiId getParentId() {
        return settings.getRootElementId();
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        this.component = component;
    }

    @Override
    public void detach(Component component) {
        super.detach(component);
        template.detach();
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        settings.setMarkupId(component.getMarkupId(true));

        if (settings.isLinkedWithParent()) {
            Component parent = component;
            boolean found = false;
            while (!found) {
                parent = parent.getParent();
                if (parent == null) {
                    throw new RuntimeException("Parent layout behavior not found for component[" + component.getMarkupId() + "]");
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

        MarkupContainer cont = (MarkupContainer) component;
        cont.visitChildren(new IVisitor() {
            public Object component(Component component) {
                for (Iterator i = component.getBehaviors().iterator(); i.hasNext();) {
                    Object behavior = i.next();
                    if (behavior instanceof IWireframeService) {
                        return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    } else if (behavior instanceof UnitBehavior) {
                        settings.register(((UnitBehavior) behavior).getSettings());
                        return IVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
                    }
                }
                return IVisitor.CONTINUE_TRAVERSAL;
            }
        });

        super.renderHead(response);
    }

}
