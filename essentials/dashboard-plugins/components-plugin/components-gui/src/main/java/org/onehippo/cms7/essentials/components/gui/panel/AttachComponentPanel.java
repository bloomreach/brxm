/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.gui.panel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.onehippo.cms7.essentials.components.gui.ComponentsWizard;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.CatalogObject;
import org.onehippo.cms7.essentials.dashboard.panels.DropdownPanel;
import org.onehippo.cms7.essentials.dashboard.panels.EventListener;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ComponentsUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Attach component to a component container.
 *
 * @version "$Id$"
 */
public class AttachComponentPanel extends EssentialsWizardStep {

    public static final String JSP_TEMPLATE = "jsptemplate.ftl";
    public static final String FREEMARKER_TEMPLATE = "freemarkertemplate.ftl";
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(AttachComponentPanel.class);
    private final ComponentsWizard parent;
    private final DropdownPanel sitesChoice;
    private final DropdownPanel componentsChoice;
    private final DropdownPanel beansDropdown;
    private final TemplatePanel scriptPanel;
    private String selectedBean;
    private boolean freemarker;

    public AttachComponentPanel(final ComponentsWizard parent, final String id) {
        super(id);
        this.parent = parent;
        final PluginContext context = parent.getContext();
        final Form<?> form = new Form("form");
        //############################################
        // SITES SELECT
        //############################################
        final List<String> sites = ComponentsUtils.getAllAvailableSites(context);
        sitesChoice = new DropdownPanel("sites", "Select a site", form, sites, new EventListener<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelected(final AjaxRequestTarget target, final Collection<String> selectedItems) {
                final String selectedSite = sitesChoice.getSelectedItem();
                if (Strings.isNullOrEmpty(selectedSite)) {
                    log.debug("No site selected");
                    return;
                }
                final List<String> addedComponents = ComponentsPanel.Util.getAddedComponents(parent.getProvider(), context, selectedSite);
                componentsChoice.changeModel(target, addedComponents);
            }
        });

        //############################################
        // COMPONENTS SELECT
        //############################################

        componentsChoice = new DropdownPanel("componentList", "Select a component to configure:", form, Collections.<String>emptyList(), new EventListener<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelected(final AjaxRequestTarget target, final Collection<String> selectedItems) {
                if (selectedItems.size() > 0) {
                    final CatalogObject catalogObject = parent.getProvider().get(selectedItems.iterator().next());
                    //final CatalogObject catalogObject = choiceRenderer.getCatalogObject(selectedItems.iterator().next());
                    onComponentSelected(target, catalogObject);
                }
            }
        });

        //############################################
        // BEANS
        //############################################

        final List<String> beans = BeanWriterUtils.findExitingBeanNames(context, "java");
        beansDropdown = new DropdownPanel("beansList", "Select Hippo Bean for detail page:", form, beans, new EventListener<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelected(final AjaxRequestTarget target, final Collection<String> selectedItems) {
                if (selectedItems.size() > 0) {
                    onBeanSelected(target, selectedItems.iterator().next());
                }
            }
        });
        //############################################
        // ADD TEXT AREA
        //############################################

        scriptPanel = new TemplatePanel("scriptPanel", "JSP/FreemarkerTemplate", form) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onTemplateTypeChanged(final AjaxRequestTarget target, final boolean freemarker) {
                swapTemplate(target, freemarker);
            }
        };
        //############################################
        // SETUP
        //############################################
        beansDropdown.hide(null);
        scriptPanel.hide(null);
        add(form);
        //############################################
        // NEW SELECT PANEL
        //############################################

    }

    private void swapTemplate(final AjaxRequestTarget target, final boolean model) {
        freemarker = model;
        onBeanSelected(target, selectedBean);
    }

    private void onBeanSelected(final AjaxRequestTarget target, final String selected) {
        if (Strings.isNullOrEmpty(selected)) {
            log.debug("No bean selected");
            return;
        }
        selectedBean = selected;
        log.info("selected bean: {}", selected);
        // update model
        final Map<String, Path> beans = BeanWriterUtils.mapExitingBeanNames(parent.getContext(), "java");
        final Path path = beans.get(selected);
        if (path == null) {
            log.warn("Path was null for bean name {}", selected);
            return;
        }
        final Map<String, Object> data = new HashMap<>();
        data.put("beanReference", JavaSourceUtils.getFullQualifiedClassName(path));

        final List<TemplateUtils.PropertyWrapper> properties = TemplateUtils.parseBeanProperties(path);
        final Collection<String> listObject = new ArrayList<>();
        for (TemplateUtils.PropertyWrapper property : properties) {
            final String document = property.getFormattedJspProperty("document");
            listObject.add(document);
        }
        data.put("repeatable", listObject);
        final String myTemplate = freemarker ? FREEMARKER_TEMPLATE : JSP_TEMPLATE;
        String templateText = TemplateUtils.injectTemplate(myTemplate, data, getClass());
        log.info(templateText);
        scriptPanel.setTextModel(target, templateText);
        scriptPanel.show(target);

    }

    //todo
    private void onComponentSelected(final AjaxRequestTarget target, final CatalogObject selected) {
        log.info("Component selected# {}", selected);
        if (selected.isDetail()) {
            beansDropdown.show(target);
        } else {
            beansDropdown.hide(target);
        }
    }

    @Override
    public void refresh(final AjaxRequestTarget target) {
        // hide if needed:
        if (beansDropdown.isShown()) {
            sitesChoice.changeModel(target, ComponentsUtils.getAllAvailableSites(parent.getContext()));
            beansDropdown.hide(target);
        }


    }

    @Override
    public void applyState() {
        setComplete(false);
        // TODO finalize
        setComplete(true);

    }
}


