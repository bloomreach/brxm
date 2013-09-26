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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.components.gui.ComponentsWizard;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.panels.DropdownPanel;
import org.onehippo.cms7.essentials.dashboard.panels.EventListener;

import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ComponentsUtils;
import org.onehippo.cms7.essentials.dashboard.utils.JavaSourceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Attach component to a component container.
 *
 * @version "$Id$"
 */
public class AttachComponentPanel extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    public static final String JSP_TEMPLATE = "jsptemplate.ftl";
    private static Logger log = LoggerFactory.getLogger(AttachComponentPanel.class);
    private final ComponentsWizard parent;
    private final DropdownPanel sitesChoice;
    private final DropdownPanel componentsChoice;
    private final DropdownPanel beansDropdown;
    private  String templateText;
    private final TextArea<String> scriptTemplate;

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
                // load components:
                final List<String> addedComponents = ComponentsPanel.Util.getAddedComponents(context, selectedSite);
                componentsChoice.changeModel(target, addedComponents);
           /*     // load site containers:
                final List<String> existingContainers = ComponentsPanel.Util.getExistingContainers(context, selectedSite);
                containerChoice.setChoices(existingContainers);
                // render components
                target.add(componentsChoice, containerChoice);*/

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
                    onComponentSelected(target, selectedItems.iterator().next());
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

        scriptTemplate = new TextArea<>("scriptTemplate", new PropertyModel<String>(this, "templateText"));

        //############################################
        // SETUP
        //############################################
        beansDropdown.hide(null);
        scriptTemplate.setOutputMarkupPlaceholderTag(true);
        scriptTemplate.setOutputMarkupId(true);
        scriptTemplate.setVisible(false);
        form.add(scriptTemplate);
        add(form);
        //############################################
        // NEW SELECT PANEL
        //############################################

    }

    private void onBeanSelected(final AjaxRequestTarget target, final String selected) {
        if(Strings.isNullOrEmpty(selected)){
            log.debug("No bean selected");
            return;
        }
        log.info("selected bean: {}", selected);
        scriptTemplate.setVisible(true);
        // update model
        final Map<String, Path> beans = BeanWriterUtils.mapExitingBeanNames(parent.getContext(), "java");
        final Path path = beans.get(selected);
        if(path ==null){
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
        templateText = TemplateUtils.injectTemplate(JSP_TEMPLATE, data, getClass());
        log.info(templateText);
        scriptTemplate.modelChanged();
        target.add(scriptTemplate);




    }

    private void onComponentSelected(final AjaxRequestTarget target, final String selected) {
        log.info("Component selected# {}", selected);
        if(selected.equals("Document Component")){
            beansDropdown.show(target);
        }

    }

    @Override
    public void applyState() {
        setComplete(false);
        // TODO finalize
        setComplete(true);

    }
}


