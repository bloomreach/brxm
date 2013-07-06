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

import java.util.Arrays;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.util.template.PackageTextTemplate;
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

    private final PackageTextTemplate INIT_PAGE = new PackageTextTemplate(PageLayoutBehavior.class, "init_page.js");

    private PageLayoutSettings settings;
    private HippoTextTemplate template;
    private boolean rendered = false;

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

    //Implement IWireframeService
    public YuiId getYuiId() {
        return settings.getRootId();
    }

    @Override
    public boolean isRendered() {
        return rendered;
    }

    @Override
    public void render(final AjaxRequestTarget target) {
    }

    @Override
    public void addHeaderContribution(IYuiContext context) {
        context.addModule(HippoNamespace.NS, "layoutmanager");
        context.addTemplate(new IHeaderContributor() {
            @Override
            public void renderHead(final IHeaderResponse response) {
                if (rendered) {
                    return;
                }
                response.render(getHeaderItem());
                rendered = true;
            }
        });
    }

    @Override
    public void resize(final AjaxRequestTarget target) {
        target.appendJavaScript("YAHOO.hippo.LayoutManager.render()");
    }

    @Override
    public HeaderItem getHeaderItem() {
        return new OnDomReadyHeaderItem("not-empty") {

            private String getId() {
                return getComponent().getMarkupId(true) + "-wireframe-behavior";
            }

            @Override
            public int hashCode() {
                return getId().hashCode();
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj instanceof OnDomReadyHeaderItem) {
                    return getRenderTokens().equals(((OnDomReadyHeaderItem) obj).getRenderTokens());
                }
                return false;
            }

            @Override
            public CharSequence getJavaScript() {
                return template.getString();
            }

            @Override
            public Iterable<?> getRenderTokens() {
                return Arrays.asList(getId());
            }
        };
    }

}
