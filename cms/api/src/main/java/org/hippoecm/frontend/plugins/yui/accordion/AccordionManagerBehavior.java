/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.accordion;

import java.util.Arrays;

import org.apache.wicket.Component;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.util.template.PackageTextTemplate;
import org.hippoecm.frontend.plugins.yui.AbstractYuiBehavior;
import org.hippoecm.frontend.plugins.yui.HippoNamespace;
import org.hippoecm.frontend.plugins.yui.header.IYuiContext;
import org.hippoecm.frontend.plugins.yui.header.templates.DynamicTextTemplate;
import org.hippoecm.frontend.plugins.yui.layout.IWireframe;
import org.hippoecm.frontend.plugins.yui.layout.WireframeUtils;

public class AccordionManagerBehavior extends AbstractYuiBehavior {
    private static final long serialVersionUID = 1L;

    //Provide a more generic approach by making the function call variable as well
    private final PackageTextTemplate INIT = new PackageTextTemplate(AccordionManagerBehavior.class, "init.js");

    private DynamicTextTemplate template;

    public AccordionManagerBehavior(AccordionConfiguration accordionSettings) {
        this.template = new DynamicTextTemplate(INIT);
        template.setConfiguration(accordionSettings);
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        template.setId(component.getMarkupId());
    }

    @Override
    public void addHeaderContribution(IYuiContext helper) {
        helper.addModule(HippoNamespace.NS, "accordionmanager");
        helper.addTemplate(new IHeaderContributor() {

            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(getHeaderItem());
            }
        });
    }

    public HeaderItem getHeaderItem() {
        return new OnDomReadyHeaderItem("not-empty") {

            private String getId() {
                return getComponent().getMarkupId(true) + "-accordion-behavior";
            }

            @Override
            public Iterable<?> getRenderTokens() {
                return Arrays.asList(getId());
            }

            @Override
            public Iterable<? extends HeaderItem> getDependencies() {
                IWireframe wireframe = WireframeUtils.getParentWireframe(getComponent());
                if (wireframe != null && !wireframe.isRendered()) {
                    return Arrays.asList(wireframe.getHeaderItem());
                }
                return super.getDependencies();
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

        };
    }

}
