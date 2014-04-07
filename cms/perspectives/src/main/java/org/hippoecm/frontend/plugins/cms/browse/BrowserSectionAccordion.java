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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.internal.HtmlHeaderContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.render.ICardView;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An accordion for the browser.
 */
public class BrowserSectionAccordion extends Panel implements ICardView {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserSectionAccordion.class);

    private IRenderService parentService;
    private BrowserSections sections;
    private IBrowserSection focussed;
    private final AbstractView<String> view;

    public BrowserSectionAccordion(String id, final BrowserSections sections,
            final AccordionManagerBehavior accordionManager, IRenderService parentRenderService) {
        super(id, new Model<String>(null));

        this.parentService = parentRenderService;
        this.sections = sections;

        add(accordionManager);
        setOutputMarkupId(true);

        IDataProvider<String> sectionProvider = new IDataProvider<String>() {
            private static final long serialVersionUID = 1L;

            private transient List<String> names;

            private void load() {
                if (names == null) {
                    names = new ArrayList<String>(sections.getSections());
                }
            }

            @Override
            public Iterator<String> iterator(long first, long count) {
                load();
                return names.subList((int) first, (int) (first + count)).iterator();
            }

            @Override
            public IModel<String> model(String object) {
                return new Model<String>(object);
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
        view = new AbstractView<String>("list", sectionProvider) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final Item<String> item) {
                final IBrowserSection section = sections.getSection(item.getModelObject());
                AjaxLink<Void> link = new AjaxLink<Void>("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        onSelect(item.getModelObject());
                    }
                };

                IModel<String> focusModel = new LoadableDetachableModel<String>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected String load() {
                        if (section == focussed) {
                            return "focus";
                        } else {
                            return "unfocus";
                        }
                    }
                };
                link.add(new AttributeModifier("class", true, focusModel));
                link.add(new Label("header", section.getTitle()));
                item.add(link);

                section.bind(parentService, "id");

                final Component component = section.getComponent();
                component.setOutputMarkupId(true);
                component.setOutputMarkupPlaceholderTag(true);
                item.add(component);
            }

            @Override
            protected void destroyItem(Item<String> item) {
                IBrowserSection section = sections.getSection(item.getModelObject());
                section.unbind();
            }
        };
        add(view);

        String selectedBrowserSection = (String) getDefaultModelObject();
        if (selectedBrowserSection != null) {
            IBrowserSection section = sections.getSection(selectedBrowserSection);
            if (section != null) {
                onFocusSection(section);
            }
        }

    }

    public void render(PluginRequestTarget target) {
        view.populate();
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
                component.setVisible(section == focussed);
            }
        }
        super.onBeforeRender();
    }

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

    public void onSelect(String extension) {
    }

    private void onFocusSection(IBrowserSection section) {
        if (section != focussed) {
            focussed = section;
            AjaxRequestTarget target = RequestCycle.get().find(AjaxRequestTarget.class);
            if (target != null) {
                target.add(this);
            }
        }
    }

    public void select(String extension) {
        if (extension != null) {
            onFocusSection(sections.getSection(extension));
        }
    }

    private boolean isActive() {
        ICardView cardView = findParent(ICardView.class);
        return cardView == null || cardView.isActive(this);
    }

    @Override
    public boolean isActive(Component component) {
        if (isActive()) {
            if (focussed != null) {
                Component focussedComponent = focussed.getComponent();
                while (component != this) {
                    if (component == focussedComponent) {
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
}
