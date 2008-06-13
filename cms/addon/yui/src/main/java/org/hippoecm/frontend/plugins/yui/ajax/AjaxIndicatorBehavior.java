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
package org.hippoecm.frontend.plugins.yui.ajax;

import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.AbstractHeaderContributor;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.ajax.markup.html.WicketAjaxIndicatorAppender;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.util.collections.MiniMap;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;
import org.wicketstuff.yui.YuiHeaderContributor;

public class AjaxIndicatorBehavior extends AbstractHeaderContributor {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    @Override
    public IHeaderContributor[] getHeaderContributors() {
        IHeaderContributor[] dependencies = new IHeaderContributor[3];
        dependencies[0] = YuiHeaderContributor.forModule("yahoo").getHeaderContributors()[0];
        dependencies[1] = YuiHeaderContributor.forModule("dom").getHeaderContributors()[0];
        dependencies[2] = HeaderContributor.forJavaScript(AjaxIndicatorBehavior.class, "ajax_indicator.js");
        return dependencies;
    }

    @Override
    public void bind(Component component) {

        final WicketAjaxIndicatorAppender ajaxIndicator = new WicketAjaxIndicatorAppender() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getIndicatorUrl() {
                return RequestCycle.get().urlFor(new ResourceReference(AjaxIndicatorBehavior.class, "ajax-loader.gif"));
            }
        };
        component.add(ajaxIndicator);
        component.add(TextTemplateHeaderContributor.forJavaScript(AjaxIndicatorBehavior.class,
                "init_ajax_indicator.js", new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    private Map<String, Object> variables;

                    @Override
                    public Object getObject() {
                        if (variables == null) {
                            variables = new MiniMap(1);
                            variables.put("id", ajaxIndicator.getMarkupId());
                        }
                        return variables;
                    }
                }));

        super.bind(component);
    }

}
