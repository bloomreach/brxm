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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.yui.accordion.AccordionManagerBehavior;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An accordion for the browser.
 */
public class BrowserSectionAccordion extends Panel {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowserSectionAccordion.class);

    private IRenderService parentService;
    private BrowserSections sections;
    private IBrowserSection focussed;

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

            public Iterator<String> iterator(int first, int count) {
                load();
                return names.subList(first, first + count).iterator();
            }

            public IModel<String> model(String object) {
                return new Model<String>(object);
            }

            public int size() {
                return sections.getSections().size();
            }

            public void detach() {
                names = null;
            }
        };
        add(new AbstractView<String>("list", sectionProvider) {
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

                Component c = section.getComponent();
                c.add(accordionManager.newSection());
                item.add(c);
            }

            @Override
            protected void destroyItem(Item<String> item) {
                IBrowserSection section = sections.getSection(item.getModelObject());
                section.unbind();
            }
        });

        String selectedBrowserSection = (String) getDefaultModelObject();
        if (selectedBrowserSection != null) {
            IBrowserSection section = sections.getSection(selectedBrowserSection);
            if (section != null) {
                onFocusSection(section);
            }
        }

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
                component.setVisible(section == focussed);
            }
        }
        super.onBeforeRender();
    }

    public void onSelect(String extension) {
    }

    private void onFocusSection(IBrowserSection section) {
        if (section != focussed) {
            focussed = section;
            AjaxRequestTarget target = AjaxRequestTarget.get();
            if (target != null) {
                target.addComponent(this);
            }
        }
    }

    public void select(String extension) {
        if (extension != null) {
            onFocusSection(sections.getSection(extension));
        }
    }

}
