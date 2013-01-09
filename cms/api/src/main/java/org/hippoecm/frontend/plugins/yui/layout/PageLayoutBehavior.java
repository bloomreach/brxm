/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.HippoTextTemplate;

import net.sf.json.JsonConfig;

/**
 * Special purpose {@link WireframeBehavior} that automatically binds itself to the document body and creates a 
 * wireframe that takes up the full viewport and is registered as the root of the applications wireframe structure.
 * <p>
 * In accordance with YUI-grids CSS, the default id value for the wireframe root element is set to "doc3".
 * </p>
 * <p>
 * Note: It doesn't support dynamic registration with parent wireframes or looking up {@link UnitBehavior}'s from child
 * components because of it's static nature. This can be achieved by the {@link WireframeBehavior}
 * <p>
 * 
 * @see WireframeBehavior
 */
public class PageLayoutBehavior extends AbstractYuiBehavior implements IWireframe {

    private static final long serialVersionUID = 1L;

    private final PackagedTextTemplate INIT_PAGE = new PackagedTextTemplate(PageLayoutBehavior.class, "init_page.js");

    private PageLayoutSettings settings;
    private HippoTextTemplate template;

    public PageLayoutBehavior(final PageLayoutSettings settings) {
        this.settings = settings;
        template = new HippoTextTemplate(INIT_PAGE, "YAHOO.hippo.GridsRootWireframe") {
            private static final long serialVersionUID = 1L;

            @Override
            public String getId() {
                return settings.getRootId().getElementId();
            }

            @Override
            public PageLayoutSettings getSettings() {
                return settings;
            }

            @Override
            public JsonConfig getJsonConfig() {
                JsonConfig jsonConfig = new JsonConfig();
                jsonConfig.registerJsonValueProcessor(YuiId.class, new YuiIdProcessor());
                return jsonConfig;
            }
        };
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addTemplate(template);
        context.addOnDomLoad("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    public void resize(final AjaxRequestTarget target) {
        target.appendJavascript("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    public void detach(Component component) {
        template.detach();
    }

    //Implement IWireframeService
    public YuiId getYuiId() {
        return settings.getRootId();
    }

}
