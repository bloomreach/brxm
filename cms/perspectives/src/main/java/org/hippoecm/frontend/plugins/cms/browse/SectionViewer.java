/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ICardView;
import org.hippoecm.frontend.widgets.AbstractView;

public class SectionViewer extends Panel implements ICardView {

    private IRenderService parentService;
    private BrowserSections sections;

    public SectionViewer(final String id, final BrowserSections sections, IRenderService parentRenderService) {
        super(id, new Model<String>(null));

        setOutputMarkupId(true);
        
        add(new AttributeAppender("class", Model.of("section-viewer"), " "));

        this.parentService = parentRenderService;
        this.sections = sections;

        IDataProvider<String> sectionProvider = new IDataProvider<String>() {
            private static final long serialVersionUID = 1L;

            private transient List<String> names;

            private void load() {
                if (names == null) {
                    names = new ArrayList<>(sections.getSections());
                }
            }

            @Override
            public Iterator<String> iterator(long first, long count) {
                load();
                return names.subList((int) first, (int) (first + count)).iterator();
            }

            @Override
            public IModel<String> model(String object) {
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
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<String> item) {
                final IBrowserSection section = sections.getSection(item.getModelObject());

                section.bind(parentService, "section-view");

                final Component component = section.getComponent();
                component.setOutputMarkupId(true);
                component.setOutputMarkupPlaceholderTag(true);
                item.add(component);
                
                item.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {
                    @Override
                    public String getObject() {
                        return sections.isActive(section) ? "selected" : "unselected";
                    }
                }, " "));
            }

            @Override
            protected void destroyItem(Item<String> item) {
                IBrowserSection section = sections.getSection(item.getModelObject());
                section.unbind();
            }
        });

        String selectedBrowserSection = (String) getDefaultModelObject();
        if (selectedBrowserSection != null) {
            select(selectedBrowserSection);
        }

        final Form form = new Form("selection-form");
        add(form);

        final SectionNamesModel sectionNamesModel = new SectionNamesModel();
        this.sections.addListener(sectionNamesModel);
        
        //final IModel<String> selectModel = Model.of(selectedBrowserSection == null ? "0" : selectedBrowserSection);
        final IModel<String> selectModel = new SelectedSectionModel();
        
        DropDownChoice<String> select = new DropDownChoice<>("select", selectModel, sectionNamesModel, 
            new IChoiceRenderer<String>() {
                @Override
                public Object getDisplayValue(final String object) {
                    final IBrowserSection section = sections.getSection(object);
                    return section.getTitle().getObject();
                }
    
                @Override
                public String getIdValue(final String object, final int index) {
                    return object;
                }
            }
        );
        select.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                onSelect(selectModel.getObject());
            }
        });
        form.add(select);

        this.sections.addListener(new IChangeListener() {
            private static final long serialVersionUID = 1L;

            public void onChange() {
                select(sections.getActiveSectionName());
            }
        });

    }

    public void render(PluginRequestTarget target) {
        for (String name : sections.getSections()) {
            sections.getSection(name).render(target);
        }
    }

    @Override
    public void onBeforeRender() {
        if (sections != null) {
            for (String extension : sections.getSections()) {
                IBrowserSection section = sections.getSection(extension);
                Component component = section.getComponent();
                component.setVisible(sections.isActive(extension));
            }
        }
        super.onBeforeRender();
    }

    /*
    @Override
    public void renderHead(final HtmlHeaderContainer container) {
        super.renderHead(container);

        IHeaderResponse response = container.getHeaderResponse();
        final String activeSection = sections.getActiveSection();
        if (activeSection != null) {
            final IBrowserSection section = sections.getSection(activeSection);
            Component component = section.getComponent();
            response.render(OnDomReadyHeaderItem.forScript("YAHOO.hippo.AccordionManager.render('" + getMarkupId() + "', '"
                    + component.getMarkupId() + "')"));
        }
    }
    */

    public void onSelect(String extension) {
        sections.setActiveSection(extension);
    }

    public void select(String sectionName) {
        if (sectionName != null) {
            sections.setActiveSection(sectionName);

            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
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
                Component focusedComponent = active.getComponent();
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

    private boolean isActive() {
        ICardView cardView = findParent(ICardView.class);
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
        public void setObject(final String object) {
            sections.setActiveSection(object);
        }

        @Override
        public void detach() {
            // Not implemented
        }
    }
}
