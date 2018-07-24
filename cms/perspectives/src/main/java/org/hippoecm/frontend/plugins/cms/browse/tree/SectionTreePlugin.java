/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.l10n.ResourceBundleModel;
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

@SuppressWarnings("unused")
public class SectionTreePlugin extends ListRenderService implements IPlugin {

    private static final long serialVersionUID = 1L;

    private class Section implements IDetachable {
        private static final long serialVersionUID = 1L;

        private final String extension;
        private final String header;
        private boolean focused;
        private boolean selected;
        private final AbstractRenderService.ExtensionPoint extPt;

        Section(String extension, String header) {
            this.extension = extension;
            this.header = header;
            this.focused = false;
            this.extPt = children.get(extension);
        }

        boolean hasChildren() {
            return !extPt.getChildren().isEmpty();
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
            if (!headers.isEmpty() && i < headers.size()) {
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
        select = new DropDownChoice<Section>("select", selectModel, sections,
                new IChoiceRenderer<Section>() {
                    @Override
                    public Object getDisplayValue(final Section section) {
                        return new ResourceBundleModel("hippo:cms.sections", section.header).getObject();
                    }

                    @Override
                    public String getIdValue(final Section section, final int index) {
                        return section.extension;
                    }

                    @Override
                    public Section getObject(final String id, final IModel<? extends List<? extends Section>> choicesModel) {
                        final List<? extends Section> choices = choicesModel.getObject();
                        for (int index = 0; index < choices.size(); index++) {
                            final Section choice = choices.get(index);
                            if (getIdValue(choice, index).equals(id)) {
                                return choice;
                            }
                        }
                        return null;
                    }
                }
        ) {
            @Override
            public boolean isEnabled(){
                if (sections != null){
                    final List<Section> choices = sections.getObject();
                    return choices != null && choices.size() > 1;
                }
                return false;
            }
        };
        select.add(new AjaxFormComponentUpdatingBehavior("change") {
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
        final List<Section> sectionList = sections.getObject();

        if (findSectionForInitialFocus) {
            Section section = findFocus();
            if (section != null) {
                focusRenderer(section);
            } else {
                if (!sectionList.isEmpty()) {
                    section = sectionList.get(0);
                    select.getModel().setObject(section);
                    focusSection(section);
                }
            }
            findSectionForInitialFocus = false;
        }

        for (Section section : sectionList) {
            for (IRenderService service : section.getChildren()) {
                Component component = service.getComponent();
                component.setVisible(section.focused);
            }
        }
        super.onBeforeRender();
    }

    private void focusRenderer(final Section section) {
        final IRenderService renderer = section.getRenderer();
        if (renderer != null) {
            renderer.focus(null);
        }
    }

    @Override
    public void internalRenderHead(final HtmlHeaderContainer container) {
        super.internalRenderHead(container);

        final IHeaderResponse response = container.getHeaderResponse();
        final String selectricOptions =
            "{ " +
                "optionsItemBuilder: '<span class=\"{value}\">{text}</span>'," +
                "labelBuilder: '<span title=\"{text}\">{text}</span>'" +
            "}";
        final String selectricInit = String.format("jQuery('#%s').selectric(%s);", select.getMarkupId(), selectricOptions);
        response.render(OnDomReadyHeaderItem.forScript(selectricInit));
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
        if (section != null) {
            focusRenderer(section);
        }
    }

    @Override
    protected void onDetach() {
        sections.getObject().forEach(SectionTreePlugin.Section::detach);
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
