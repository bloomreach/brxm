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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.YuiPluginHelper;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionSettings;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.service.render.ListRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionTreePlugin extends ListRenderService implements IPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private class Section implements IClusterable {
        private static final long serialVersionUID = 1L;

        String extension;
        IModel focusModel;
        boolean focussed;
        boolean selected;
        AbstractRenderService.ExtensionPoint extPt;

        Section(String extension) {
            this.extension = extension;
            this.focussed = false;
            this.extPt = children.get(extension);
            this.focusModel = new Model() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    if (focussed) {
                        if (selected) {
                            return "select focus";
                        } else {
                            return "focus";
                        }
                    } else {
                        return "unfocus";
                    }
                }
            };
        }
    }

    static final Logger log = LoggerFactory.getLogger(SectionTreePlugin.class);

    List<Section> sections;
    boolean toggleBehaviour = false;

    AccordionManagerBehavior accordionManager;

    public SectionTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AccordionSettings settings = new AccordionSettings(config.getPluginConfig(AccordionSettings.CONFIG_KEY));
        add(accordionManager = new AccordionManagerBehavior(YuiPluginHelper.getManager(context), settings));

        final List<String> headers = Arrays.asList(config.getStringArray("headers"));
        String[] behaviours = config.getStringArray("behaviours");
        if (behaviours != null) {
            for (int i = 0; i < behaviours.length; i++) {
                if ("toggle".equals(behaviours[i]))
                    toggleBehaviour = true;
            }
        }

        List<String> extensions = Arrays.asList(config.getStringArray(RenderService.EXTENSIONS_ID));
        sections = new ArrayList(extensions.size());
        for (String extension : extensions) {
            sections.add(new Section(extension));
        }
        if (!toggleBehaviour) {
            sections.get(0).focussed = true;
        }

        add(new ListView("list", sections) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem item) {
                final Section section = (Section) item.getModelObject();
                AjaxLink link;
                link = new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (section.extPt.getChildren().size() > 0) {
                            IRenderService renderer = section.extPt.getChildren().get(0);
                            IModelReference modelService = context.getService(context.getReference(renderer)
                                    .getServiceId(), IModelReference.class);
                            if (modelService != null) {
                                IModel sectionModel = modelService.getModel();
                                SectionTreePlugin.this.setModel(sectionModel);
                            } else {
                                focusSection(section, true);
                            }
                            SectionTreePlugin.this.redraw();
                        }
                    }
                };
                link.add(new AttributeModifier("class", true, section.focusModel));
                item.add(link);

                String label = section.extension;
                if (sections.indexOf(section) < headers.size())
                    label = headers.get(sections.indexOf(section));
                link.add(new Label("header", new StringResourceModel(label, SectionTreePlugin.this, null)));

                if (section.extPt.getChildren().size() > 0) {
                    Component c = section.extPt.getChildren().get(0).getComponent();
                    item.add(c);
                    c.add(accordionManager.newAccordion());
                } else {
                    item.add(new EmptyPanel("id"));
                    link.setVisible(false);
                    item.setVisible(false);
                }
            }
        });
    }

    @Override
    public void onBeforeRender() {
        for (Section section : sections) {
            for (IRenderService service : section.extPt.getChildren()) {
                Component component = service.getComponent();
                component.setVisible(section.focussed);
            }
        }
        super.onBeforeRender();
    }

    @Override
    public void focus(IRenderService child) {
        if (child != null) {
            for (Section section : sections) {
                if (section.extPt.getChildren().contains(child)) {
                    focusSection(section, false);
                    redraw();
                    break;
                }
            }
        } else {
            super.focus(null);
        }
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        IPluginContext context = getPluginContext();
        JcrNodeModel model = (JcrNodeModel) getModel();
        int matchLength = 0;
        IRenderService renderer = null;
        for (Section section : sections) {
            if (section.extPt.getChildren().size() > 0) {
                IRenderService renderService = section.extPt.getChildren().get(0);
                IModelReference modelService = context.getService(context.getReference(renderService).getServiceId(),
                        IModelReference.class);
                if (modelService != null) {
                    IModel sectionModel = modelService.getModel();
                    if (sectionModel instanceof JcrNodeModel) {
                        JcrNodeModel sectionRoot = (JcrNodeModel) sectionModel;
                        if (model.getItemModel().getPath().startsWith(sectionRoot.getItemModel().getPath())) {
                            if (sectionRoot.getItemModel().getPath().length() > matchLength) {
                                matchLength = sectionRoot.getItemModel().getPath().length();
                                renderer = renderService;
                            }
                        }
                    }
                }
            }
        }
        if (renderer != null) {
            renderer.focus(null);
        }
    }

    private void focusSection(Section section, boolean toggle) {
        if (toggleBehaviour) {
            if (toggle) {
                section.focussed = !section.focussed;
            } else {
                section.focussed = true;
            }
        } else {
            for (Section curSection : sections) {
                curSection.focussed = (curSection == section);
            }
        }
        for (Section curSection : sections) {
            curSection.selected = (curSection == section);
        }
    }

}
