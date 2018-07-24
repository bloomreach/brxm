/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.IContextMenuManager;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.ReadOnlyModel;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClass;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ICardView;
import org.hippoecm.frontend.widgets.AbstractView;

public class SectionViewer extends Panel implements ICardView {

    private final IRenderService parentRenderService;
    private final BrowserSections sections;
    private final DropDownChoice<String> select;

    public SectionViewer(final String id, final BrowserSections sections, final IRenderService parentRenderService) {
        super(id, new Model<String>(null));

        this.parentRenderService = parentRenderService;
        this.sections = sections;

        setOutputMarkupId(true);
        add(CssClass.append("section-viewer"));

        final IDataProvider<String> sectionProvider = new IDataProvider<String>() {

            private transient List<String> names;

            private void load() {
                if (names == null) {
                    names = new ArrayList<>(sections.getSections());
                }
            }

            @Override
            public Iterator<String> iterator(final long first, final long count) {
                load();
                return names.subList((int) first, (int) (first + count)).iterator();
            }

            @Override
            public IModel<String> model(final String object) {
                return new Model<>(object);
            }

            @Override
            public long size() {
                return sections.getSections().size();
            }

            @Override
            public void detach() {
                names = null;
            }
        };

        add(new AbstractView<String>("list", sectionProvider) {

            @Override
            protected void populateItem(final Item<String> item) {
                final IBrowserSection section = sections.getSection(item.getModelObject());

                section.bind(SectionViewer.this.parentRenderService, "section-view");

                final Component component = section.getComponent();
                component.setOutputMarkupId(true);
                component.setOutputMarkupPlaceholderTag(true);
                item.add(component);
                item.add(CssClass.append(ReadOnlyModel.of(() -> sections.isActive(section) ? "selected" : "unselected")));
            }

            @Override
            protected void destroyItem(final Item<String> item) {
                final IBrowserSection section = sections.getSection(item.getModelObject());
                section.unbind();
            }
        });

        final String selectedBrowserSection = (String) getDefaultModelObject();
        if (selectedBrowserSection != null) {
            select(selectedBrowserSection);
        }

        final Form form = new Form("selection-form");
        add(form);

        final SectionNamesModel sectionNamesModel = new SectionNamesModel();
        this.sections.addListener(sectionNamesModel);

        final IModel<String> selectModel = new SelectedSectionModel();
        select = new DropDownChoice<>("select", selectModel, sectionNamesModel,
            new IChoiceRenderer<String>() {
                @Override
                public Object getDisplayValue(final String sectionId) {
                    final IBrowserSection section = sections.getSection(sectionId);
                    return section.getTitle().getObject();
                }

                @Override
                public String getIdValue(final String sectionId, final int index) {
                    return sectionId;
                }

                @Override
                public String getObject(final String id, final IModel<? extends List<? extends String>> choicesModel) {
                    final List<? extends String> choices = choicesModel.getObject();
                    return choices.stream().filter(choice -> choice.equals(id)).findFirst().orElse(null);
                }
            }
        );
        select.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                onSelect(selectModel.getObject());
            }
        });
        form.add(select);

        this.sections.addListener((IChangeListener) () -> select(sections.getActiveSectionName()));
    }

    public void render(final PluginRequestTarget target) {
        for (final String name : sections.getSections()) {
            sections.getSection(name).render(target);
        }
    }

    @Override
    public void onBeforeRender() {
        if (sections != null) {
            for (final String extension : sections.getSections()) {
                final IBrowserSection section = sections.getSection(extension);
                final Component component = section.getComponent();
                component.setVisible(sections.isActive(extension));
            }
        }
        super.onBeforeRender();
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

    public void onSelect(final String extension) {
        sections.setActiveSectionByName(extension);
        onSectionChange(extension);
    }

    public void select(final String sectionName) {
        if (sectionName != null) {
            sections.setActiveSectionByName(sectionName);
            onSectionChange(sectionName);

            final AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(this);
            }
        }
    }

    @Override
    public boolean isActive(Component component) {
        if (isActive()) {
            final IBrowserSection active = sections.getActiveSection();
            if (active != null) {
                final Component focusedComponent = active.getComponent();
                while (component != this) {
                    if (component == focusedComponent) {
                        return true;
                    }
                    component = component.getParent();
                }
            } else {
                return true;
            }
        }
        return false;
    }

    protected void onSectionChange(final String sectionName) {
        final IContextMenuManager menuManager = findParent(IContextMenuManager.class);
        if (menuManager != null) {
            menuManager.collapseAllContextMenus();
        }
    }

    private boolean isActive() {
        final ICardView cardView = findParent(ICardView.class);
        return cardView == null || cardView.isActive(this);
    }

    private class SectionNamesModel extends AbstractReadOnlyModel<List<String>> implements IChangeListener {

        private ArrayList<String> names;

        @Override
        public List<String> getObject() {
            if (names == null) {
                names = new ArrayList<>(sections.getSections());
            }
            return names;
        }

        @Override
        public void onChange() {
            names = null;
        }
    }

    private class SelectedSectionModel implements IModel<String> {

        @Override
        public String getObject() {
            return sections.getActiveSectionName();
        }

        @Override
        public void setObject(final String sectionName) {
            sections.setActiveSectionByName(sectionName);
        }

        @Override
        public void detach() {
            // Not implemented
        }
    }
}
