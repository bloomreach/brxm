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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.components.gui.ComponentsWizard;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ComponentsUtils;
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
    private static Logger log = LoggerFactory.getLogger(AttachComponentPanel.class);

    private final ComponentsWizard parent;
    private List<String> selectedDocuments;
    private final ListChoice<String> sitesChoice;
    private final ListChoice<String> containerChoice;
    private String selectedSite;
    private String selectedContainer;
    private final ListMultipleChoice<String> componentsChoice;
    private  List<String> componentsList;

    public AttachComponentPanel(final ComponentsWizard parent, final String id) {
        super(id);
        this.parent = parent;
        final PluginContext context = parent.getContext();
        final Form<?> form = new Form("form");
        //############################################
        // SITES SELECT
        //############################################
        final List<String> sites = ComponentsUtils.getAllAvailableSites(context);

        final PropertyModel<String> siteModel = new PropertyModel<>(this, "selectedSite");
        sitesChoice = new ListChoice<>("sites", siteModel, sites);
        sitesChoice.setNullValid(false);
        sitesChoice.setOutputMarkupId(true);

        sitesChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                final String selectedInput = sitesChoice.getInput();
                if(Strings.isNullOrEmpty(selectedInput)){
                    log.debug("No site selected");
                    return;
                }
                selectedSite = sitesChoice.getChoices().get(Integer.valueOf(selectedInput));
                log.info("#selected site: {}", selectedSite);
                // load components:
                final List<String> addedComponents = ComponentsPanel.Util.getAddedComponents(context, selectedSite);
                componentsChoice.setChoices(addedComponents);
                // load site containers:
                final List<String> existingContainers = ComponentsPanel.Util.getExistingContainers(context, selectedSite);
                containerChoice.setChoices(existingContainers);
                // render components
                target.add(componentsChoice, containerChoice);

            }
        });
        //############################################
        // COMPONENTS SELECT
        //############################################

        // component list is populated on site select:
        componentsList = new ArrayList<>();
        final PropertyModel<List<String>> componentModel = new PropertyModel<>(this, "componentsList");
        componentsChoice = new ListMultipleChoice<>("componentList", componentModel, componentsList);
        componentsChoice.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                onComponentSelected(target);

            }
        });
        //############################################
        // CONTAINERS
        //############################################
        final List<String> containers = new ArrayList<>();
        final PropertyModel<String> containerModel = new PropertyModel<>(this, "selectedContainer");
        containerChoice = new ListChoice<>("containers", containerModel, containers);

        //############################################
        // SETUP
        //############################################

        sitesChoice.setNullValid(false);
        componentsChoice.setOutputMarkupId(true);
        containerChoice.setOutputMarkupId(true);
        sitesChoice.setOutputMarkupId(true);

        add(form);
        form.add(sitesChoice);
        form.add(componentsChoice);
        form.add(containerChoice);
    }

    private void onComponentSelected(final AjaxRequestTarget target) {
        log.info("Component selected# {}", target);
    }


    @Override
    public void applyState() {
        setComplete(false);

        setComplete(true);

    }
}


