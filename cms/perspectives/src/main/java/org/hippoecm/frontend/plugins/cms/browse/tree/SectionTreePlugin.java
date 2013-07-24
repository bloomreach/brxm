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
package org.hippoecm.frontend.plugins.cms.browse.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionConfiguration;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.service.render.ListRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.util.MappingException;
import org.hippoecm.frontend.util.PluginConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionTreePlugin extends ListRenderService implements IPlugin {

    private static final long serialVersionUID = 1L;

    private class Section implements IDetachable {
        private static final long serialVersionUID = 1L;

        String extension;
        IModel<String> focusModel;
        boolean focussed;
        boolean selected;
        AbstractRenderService.ExtensionPoint extPt;

        Section(String extension) {
            this.extension = extension;
            this.focussed = false;
            this.extPt = children.get(extension);
            this.focusModel = new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
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

        public void detach() {
            for (IRenderService service : (List<IRenderService>) extPt.getChildren()) {
                service.getComponent().detach();
            }
        }
    }

    static final Logger log = LoggerFactory.getLogger(SectionTreePlugin.class);

    List<Section> sections;
    boolean toggleBehaviour = false;
    boolean findSectionForInitialFocus = false;

    AccordionManagerBehavior accordionManager;

    public SectionTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        AccordionConfiguration accordionConfig = new AccordionConfiguration();
        if (config.containsKey("yui.config.accordion")) {
            try {
                PluginConfigMapper.populate(accordionConfig, config.getPluginConfig("yui.config.accordion"));
            } catch (MappingException e) {
                log.warn(e.getMessage());
            }
        }
        add(accordionManager = new AccordionManagerBehavior(accordionConfig));

        final List<String> headers = Arrays.asList(config.getStringArray("headers"));
        String[] behaviours = config.getStringArray("behaviours");
        if (behaviours != null) {
            for (int i = 0; i < behaviours.length; i++) {
                if ("toggle".equals(behaviours[i])) {
                    toggleBehaviour = true;
                }
            }
        }

        List<String> extensions = Arrays.asList(config.getStringArray(RenderService.EXTENSIONS_ID));
        sections = new ArrayList<Section>(extensions.size());
        for (String extension : extensions) {
            sections.add(new Section(extension));
        }

        if (!toggleBehaviour) {
            findSectionForInitialFocus = true;
        }

        add(new ListView<Section>("list", sections) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Section> item) {
                final Section section = item.getModelObject();
                AjaxLink<Void> link = new AjaxLink<Void>("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (section.extPt.getChildren().size() > 0) {
                            IRenderService renderer = (IRenderService) section.extPt.getChildren().get(0);
                            IModelReference modelService = context.getService(context.getReference(renderer)
                                    .getServiceId(), IModelReference.class);
                            if (modelService != null) {
                                IModel sectionModel = modelService.getModel();
                                SectionTreePlugin.this.setDefaultModel(sectionModel);
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
                if (sections.indexOf(section) < headers.size()) {
                    label = headers.get(sections.indexOf(section));
                }
                link.add(new Label("header", new StringResourceModel(label, SectionTreePlugin.this, null)));

                if (section.extPt.getChildren().size() > 0) {
                    Component c = ((IRenderService)section.extPt.getChildren().get(0)).getComponent();
                    item.add(c);
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
        if (findSectionForInitialFocus) {
            IRenderService renderer = findFocus();
            if (renderer != null) {
                renderer.focus(null);
            } else {
                focusSection(sections.get(0), false);
            }
            findSectionForInitialFocus = false;
        }
        if (sections != null) {
            for (Section section : sections) {
                for (IRenderService service : (List<IRenderService>) section.extPt.getChildren()) {
                    Component component = service.getComponent();
                    component.setVisible(section.focussed);
                }
            }
        }
        super.onBeforeRender();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        for (Section section : sections) {
            final List sectionChildren = section.extPt.getChildren();
            if (sectionChildren.size() > 0) {
                final Component component = ((IRenderService) sectionChildren.get(0)).getComponent();
                if (component.isVisible()) {
                    accordionManager.activate(component);
                    break;
                }
            }
        }
    }

    @Override
    public void focus(IRenderService child) {
        if (child != null) {
            for (Section section : sections) {
                if (section.extPt.getChildren().contains(child)) {
                    if (focusSection(section, false)) {
                        redraw();
                    }
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
        IRenderService renderer = findFocus();
        if (renderer != null) {
            renderer.focus(null);
        }
    }

    @Override
    protected void onDetach() {
        for (Section section : sections) {
            section.detach();
        }
        super.onDetach();
    }

    private IRenderService findFocus() {
        JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        if (model == null || model.getItemModel() == null || model.getItemModel().getPath() == null) {
            return null;
        }

        int matchLength = 0;
        IRenderService renderer = null;
        for (Section section : sections) {
            if (section.extPt.getChildren().size() > 0) {
                IRenderService renderService = (IRenderService) section.extPt.getChildren().get(0);
                IModelReference modelService = getPluginContext().getService(
                        getPluginContext().getReference(renderService).getServiceId(), IModelReference.class);
                if (modelService != null) {
                    IModel sectionModel = modelService.getModel();
                    if (sectionModel instanceof JcrNodeModel) {
                        JcrNodeModel sectionRoot = (JcrNodeModel) sectionModel;
                        if (sectionRoot.getItemModel() != null) {
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
        }
        return renderer;
    }

    private boolean focusSection(Section section, boolean toggle) {
        boolean dirty = false;
        if (toggleBehaviour) {
            if (toggle) {
                section.focussed = !section.focussed;
                dirty = true;
            } else {
                if (!section.focussed) {
                    section.focussed = true;
                    dirty = true;
                }
            }
        } else {
            for (Section curSection : sections) {
                if (curSection == section) {
                    if (!curSection.focussed) {
                        curSection.focussed = true;
                        dirty = true;
                    }
                } else {
                    if (curSection.focussed) {
                        curSection.focussed = false;
                        dirty = true;
                    }
                }
            }
        }
        for (Section curSection : sections) {
            if (curSection == section) {
                if (!curSection.selected) {
                    curSection.selected = true;
                    dirty = true;
                }
            } else {
                if (curSection.selected) {
                    curSection.selected = false;
                    dirty = true;
                }
            }
        }
        return dirty;
    }

    public void start() {
    }

    public void stop() {
    }

}
