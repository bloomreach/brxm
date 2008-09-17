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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;

import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ListRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.service.render.AbstractRenderService;

public class SectionTreePlugin extends ListRenderService implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(SectionTreePlugin.class);

    boolean selected = true; // true means collapsed all by default, false means that it will expand the first entry
    boolean toggleBehaviour = false;

    public SectionTreePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        setModel(new Model(0));

        final List<String> extensions = Arrays.asList(config.getStringArray(RenderService.EXTENSIONS_ID));
        final List<String> headers = Arrays.asList(config.getStringArray("headers"));
        String[] behaviours = config.getStringArray("behaviours");
        if(behaviours != null) {
            for(int i=0; i<behaviours.length; i++) {
                if("toggle".equals(behaviours[i]))
                    toggleBehaviour = true;
            }
        }

        add(new ListView("list", extensions) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final String plugin = (String) item.getModel().getObject();
                AjaxLink link;
                if(toggleBehaviour) {
                    link = new AjaxLink("link", new Model(plugin)) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                boolean hasFocus = false;
                                for(Map.Entry<String, AbstractRenderService.ExtensionPoint> entry : children.entrySet()) {
                                    if(entry.getKey().equals(plugin)) {
                                        for(IRenderService service : entry.getValue().getChildren()) {
                                            Component component = service.getComponent();
                                            if(component.isVisible()) {
                                                service.getComponent().setVisible(hasFocus = false);
                                            } else {
                                                service.focus(service);
                                                service.getComponent().setVisible(hasFocus = true);
                                            }
                                        }
                                    }
                                }
                                if(hasFocus) {
                                    add(new AttributeModifier("class", true, new Model("select")));
                                } else {
                                    add(new AttributeModifier("class", true, new Model("unfocus")));
                                }
                                SectionTreePlugin.this.redraw();
                            }
                        };
                } else {
                    link = new AjaxLink("link", new Model(plugin)) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                for(Map.Entry<String, AbstractRenderService.ExtensionPoint> entry : children.entrySet()) {
                                    if(entry.getKey().equals(plugin)) {
                                        add(new AttributeModifier("class", true, new Model("focus")));
                                        for(IRenderService service : entry.getValue().getChildren()) {
                                            service.focus(service);
                                            service.getComponent().setVisible(true);
                                        }
                                    } else {
                                        add(new AttributeModifier("class", true, new Model("unfocus")));
                                        for(IRenderService service : entry.getValue().getChildren())
                                            service.getComponent().setVisible(false);
                                    }
                                }
                                SectionTreePlugin.this.redraw();
                            }
                        };
                }
                item.add(link);

                String label = plugin;
                if(extensions.indexOf(plugin) < headers.size())
                    label = headers.get(extensions.indexOf(plugin));
                link.add(new Label("header", new Model(label)));

                boolean added = false;
                if (children.containsKey(plugin)) {
                    for(IRenderService service : children.get(plugin).getChildren()) {
                        added = true;
                        Component component = service.getComponent();
                        if(!selected) {
                            selected = true;
                            component.setVisible(true);
                            link.add(new AttributeModifier("class", true, new Model("focus")));
                        } else {
                            if(component.isVisible()) {
                                link.add(new AttributeModifier("class", true, new Model("focus")));
                            } else {
                                link.add(new AttributeModifier("class", true, new Model("unfocus")));
                            }
                        }
                        item.add(component);
                        break;
                    }
                }
                if(!added) {
                    item.add(new EmptyPanel("id"));
                    link.setVisible(false);
                    item.setVisible(false);
                }
            }
        });
    }
}
