/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.tabs;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;

/**
 * 
 */
public class TabbedPanel extends org.apache.wicket.extensions.markup.html.tabs.TabbedPanel {
    private static final long serialVersionUID = 1L;

    private TabsPlugin plugin;

    public TabbedPanel(String wicketId, List<TabsPlugin.Tab> tabs, TabsPlugin plugin) {
        super(wicketId, tabs);
        this.plugin = plugin;

        setOutputMarkupId(true);
        setVersioned(false);
    }

    @Override
    protected WebMarkupContainer newLink(String linkId, final int index) {
        assert (linkId.equals("link"));
        WebMarkupContainer container = new WebMarkupContainer("container", new Model(new Integer(index)));
        final TabsPlugin.Tab tabbie = (TabsPlugin.Tab) getTabs().get(index);
        if (tabbie.canClose()) {
            container.add(new AjaxFallbackLink("close") {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    plugin.onClose(target, tabbie);
                }
            });
        } else {
            container.add(new Label("close", ""));
        }
        return container;
    }

    // used by superclass to add title to the container
    @Override
    protected Component newTitle(String titleId, IModel titleModel, final int index) {
        final TabsPlugin.Tab tabbie = (TabsPlugin.Tab) getTabs().get(index);
        return new AjaxFallbackLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                plugin.onSelect(target, tabbie);
            }
        }.add(new Label(titleId, titleModel));
    }
}
