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
package org.hippoecm.frontend.plugins.standards.deque;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.render.ListViewPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DequePlugin extends ListViewPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: TabsPlugin.java 12669 2008-07-18 14:42:13Z fvlankvelt $";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DequePlugin.class);

    private Item selected;
    private IBehavior selectBehavior;

    public DequePlugin(IPluginContext context, IPluginConfig properties) {
        super(context, properties);

        selectBehavior = new AbstractBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentTag(Component component, ComponentTag tag) {
                tag.put("class", "selected");
                tag.put("style", "width: 50%;");
            }
        };
    }

    @Override
    public void onAddRenderService(final Item item, final IRenderService service) {
        IPluginContext context = getPluginContext();
        String serviceId = context.getReference(service).getServiceId();

        ITitleDecorator titleDecor = context.getService(serviceId, ITitleDecorator.class);
        String title;
        if (titleDecor != null) {
            title = titleDecor.getTitle();
        } else {
            title = "title";
        }

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (selected != item) {
                    if (selected != null) {
                        selected.remove(selectBehavior);
                    }
                    item.add(selectBehavior);
                }
            }

        };
        link.add(new Label("title", title));
        item.add(link);
    }

}
