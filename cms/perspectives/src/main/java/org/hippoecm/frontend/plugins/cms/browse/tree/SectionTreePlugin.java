/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.AbstractRenderService;
import org.hippoecm.frontend.service.render.ListRenderService;
import org.hippoecm.frontend.service.render.RenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SectionTreePlugin extends ListRenderService implements IPlugin {

    private static final long serialVersionUID = 1L;

    private class Section implements IDetachable {
        private static final long serialVersionUID = 1L;

        String extension;
        String header;
        IModel<String> focusModel;
        boolean focused;
        boolean selected;
        AbstractRenderService.ExtensionPoint extPt;

        Section(String extension, String header) {
            this.extension = extension;
            this.header = header;
            this.focused = false;
            this.extPt = children.get(extension);
            this.focusModel = new LoadableDetachableModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected String load() {
                    if (focused) {
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

        boolean hasChildren() {
            return extPt.getChildren().size() > 0;
        }

        @SuppressWarnings("unchecked")
        List<IRenderService> getChildren() {
            return extPt.getChildren();
        }

        public IRenderService getRenderer() {
            if (hasChildren()) {
                return (IRenderService) extPt.getChildren().get(0);
            }
            return null;
        }

        public void detach() {
            for (IRenderService service : getChildren()) {
                service.getComponent().detach();
            }
        }
    }

    static final Logger log = LoggerFactory.getLogger(SectionTreePlugin.class);

    final DropDownChoice<Section> select;
    final IModel<List<Section>> sections;
    boolean findSectionForInitialFocus = true;

    public SectionTreePlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        setOutputMarkupId(true);
        add(new AttributeAppender("class", Model.of("section-viewer"), " "));

        final List<String> headers = Arrays.asList(config.getStringArray("headers"));
        final List<String> extensions = Arrays.asList(config.getStringArray(RenderService.EXTENSIONS_ID));
        final List<Section> allSections = new ArrayList<>(extensions.size());
        for (int i = 0; i < extensions.size(); i++) {
            String extension = extensions.get(i);
            String header = extension;
            if (headers.size() > 0 && i < headers.size()) {
                header = headers.get(i);
            }
            allSections.add(new Section(extension, header));
        }

        sections = new AbstractReadOnlyModel<List<Section>>() {
            @Override
            public List<Section> getObject() {
                return allSections.stream().filter(Section::hasChildren).collect(Collectors.toList());
            }
        };

        add(new ListView<Section>("list", sections) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Section> item) {
                final Section section = item.getModelObject();
                if (section.hasChildren()) {
                    Component c = section.getRenderer().getComponent();
                    item.add(c);
                } else {
                    item.add(new EmptyPanel("id"));
                }

                item.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return section.focused ? "selected" : "unselected";
                    }
                }, " "));

            }
        });

        final Form form = new Form("selection-form");
        add(form);

        final IModel<Section> selectModel = new Model<>(null);
        select = new DropDownChoice<>("select", selectModel, sections,
                new IChoiceRenderer<Section>() {
                    @Override
                    public Object getDisplayValue(final Section object) {
                        return new StringResourceModel(object.header, SectionTreePlugin.this, null).getObject();
                    }

                    @Override
                    public String getIdValue(final Section object, final int index) {
                        return object.extension;
                    }
                }
        );
        select.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                Section section = selectModel.getObject();
                focusSection(section);
                SectionTreePlugin.this.redraw();
            }
        });
        form.add(select);
    }

    @Override
    public void onBeforeRender() {
        if (findSectionForInitialFocus) {
            Section section = findFocus();
            if (section != null) {
                if (section.getRenderer() != null) {
                    section.getRenderer().focus(null);
                }
            } else if (sections.getObject().size() > 0) {
                section = sections.getObject().get(0);
                select.getModel().setObject(section);
                focusSection(section);
            }
            findSectionForInitialFocus = false;
        }

        for (Section section : sections.getObject()) {
            for (IRenderService service : section.getChildren()) {
                Component component = service.getComponent();
                component.setVisible(section.focused);
            }
        }
        super.onBeforeRender();
    }

    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        super.renderHead(container);

        final IHeaderResponse response = container.getHeaderResponse();
        response.render(OnDomReadyHeaderItem.forScript(String.format("jQuery('#%s').selectric();", select.getMarkupId())));
    }

    @Override
    public void focus(IRenderService child) {
        if (child != null) {
            for (Section section : sections.getObject()) {
                if (section.extPt.getChildren().contains(child)) {
                    if (updateStates(section)) {
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
        Section section = findFocus();
        if (section != null && section.getRenderer() != null) {
            section.getRenderer().focus(null);
        }
    }

    @Override
    protected void onDetach() {
        for (Section section : sections.getObject()) {
            section.detach();
        }
        super.onDetach();
    }

    private Section findFocus() {
        JcrNodeModel model = (JcrNodeModel) getDefaultModel();
        if (model == null || model.getItemModel() == null || model.getItemModel().getPath() == null) {
            return null;
        }

        int matchLength = 0;
        Section focusedSection = null;
        for (Section section : sections.getObject()) {
            if (section.hasChildren()) {
                IRenderService renderService = section.getRenderer();
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
                                    focusedSection = section;
                                }
                            }
                        }
                    }
                }
            }
        }
        return focusedSection;
    }

    private void focusSection(final Section section) {
        if (section.hasChildren()) {
            IRenderService renderer = section.getRenderer();
            IModelReference modelService = getPluginContext().getService(getPluginContext().getReference(renderer)
                    .getServiceId(), IModelReference.class);
            if (modelService != null) {
                IModel sectionModel = modelService.getModel();
                SectionTreePlugin.this.setDefaultModel(sectionModel);
            }
        }
        updateStates(section);
    }

    private boolean updateStates(Section section) {
        boolean dirty = false;
        for (Section curSection : sections.getObject()) {
            if (curSection == section) {
                if (!curSection.focused) {
                    curSection.focused = true;
                    dirty = true;
                }
                if (!curSection.selected) {
                    curSection.selected = true;
                    dirty = true;
                }
            } else {
                if (curSection.focused) {
                    curSection.focused = false;
                    dirty = true;
                }
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
