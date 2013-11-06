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

package org.onehippo.cms7.essentials.setup.panels;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.panels.DropdownPanel;
import org.onehippo.cms7.essentials.dashboard.panels.EventListener;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.onehippo.cms7.essentials.setup.SetupPage;
import org.onehippo.cms7.essentials.setup.panels.model.ProjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @version "$Id$"
 */
public class SelectPowerpackStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(SelectPowerpackStep.class);
    private final DropdownPanel powerpackDropdown;
    private final SetupPage myParent;
    private String selectedPowerpack;
    private String selectedTemplatesType;
    @Inject
    private EventBus eventBus;
    @Inject
    private MemoryPluginEventListener listener;

    public SelectPowerpackStep(final SetupPage component, final String title) {
        super(title);
        myParent = component;
        ProjectModel projectModel = new ProjectModel();
        org.apache.maven.model.Model pomModel = ProjectUtils.getSitePomModel();
        if (pomModel != null) {
            projectModel.setName(pomModel.getName());
            projectModel.setVersion(pomModel.getVersion());
        } else {
            projectModel.setName("No project found");
            projectModel.setVersion("NA");
        }
        setDefaultModel(new Model<Serializable>(projectModel));

        Form<?> form = new Form<>("form");

        final Label packDescription = new Label("pack.description");
        packDescription.setOutputMarkupId(true);
        form.add(packDescription);

        final List<String> powerpackList = new ArrayList<>();
        powerpackList.add("powerpack.news.and.event.label");
        powerpackList.add("powerpack.none.label");

        powerpackDropdown = new DropdownPanel("powerpackDropdown", getString("powerpack.select.label"), form, powerpackList, new EventListener<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelected(final AjaxRequestTarget target, final Collection<String> selectedItems) {
                selectedPowerpack = powerpackDropdown.getSelectedItem();
                if (Strings.isNullOrEmpty(selectedPowerpack)) {
                    log.debug("No powerpack selected");
                    return;
                }
                setComplete(true);
                packDescription.setDefaultModel(new Model<>(getString(selectedPowerpack.replace("label", "description"))));
                target.add(packDescription);
                log.info("selectedPowerpack: {}", selectedPowerpack);
            }
        }, new IChoiceRenderer<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getDisplayValue(String value) {
                return getString(value);
            }

            @Override
            public String getIdValue(String value, int index) {
                return String.valueOf(index);
            }
        }
        );

        CheckBox sampleContentCheckBox = new CheckBox("sampleContentCheckbox", Model.of(Boolean.TRUE));
        sampleContentCheckBox.setEnabled(false);
        form.add(sampleContentCheckBox);

        RadioGroup<String> radioGroup = new RadioGroup<String>("templatesRadioGroup", new PropertyModel<String>(this, "selectedTemplatesType"));
        radioGroup.setRequired(true);

        radioGroup.add(new Radio<String>("jspFilesystemRadio", new Model<String>(getString("templates.radio.jsp.filesystem"))));
        radioGroup.add(new Radio<String>("freemarkerFilesystemRadio", new Model<String>(getString("templates.radio.freemarker.filesystem"))).setEnabled(false));
        radioGroup.add(new Radio<String>("freemarkerRepositoryRadio", new Model<String>(getString("templates.radio.freemarker.repository"))).setEnabled(false));
        form.add(radioGroup);

        add(form);
    }

    @Override
    public void applyState(final AjaxRequestTarget target) {
        // clear previous events, e.g. when on back button is used:
        listener.consumeEvents();
        if (Strings.isNullOrEmpty(selectedPowerpack)) {
            eventBus.post(new DisplayEvent(getString("powerpack.none.selected.label")));
        } else if (selectedPowerpack.equals("powerpack.none.label")) {
            eventBus.post(new DisplayEvent(getString("powerpack.none.description")));
        } else {
            eventBus.post(new DisplayEvent(getString("powerpack.news.and.event.description")));
        }

        final FinalStep finalStep = myParent.getFinalStep();
        finalStep.displayEvents(target);
    }

    public String getSelectedPowerpack() {
        return selectedPowerpack;
    }
}
