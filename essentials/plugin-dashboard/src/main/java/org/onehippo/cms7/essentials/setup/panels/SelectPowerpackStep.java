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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.onehippo.cms7.essentials.setup.SetupPage;
import org.onehippo.cms7.essentials.setup.panels.model.ProjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SelectPowerpackStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    public static final String POWERPACK_NEWS_AND_EVENT_LABEL = "powerpack.news.and.event.label";
    public static final String POWERPACK_REST_LABEL = "powerpack.rest.label";
    public static final String POWERPACK_NONE_LABEL = "powerpack.none.label";
    private static Logger log = LoggerFactory.getLogger(SelectPowerpackStep.class);
    private final DropDownChoice<String> powerpackDropdown;
    private final SetupPage myParent;
    private String selectedPowerpack;
    private boolean installSampleContent = true;
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

        final List<String> selectOptionList = new ArrayList<>();
        selectOptionList.add(POWERPACK_NEWS_AND_EVENT_LABEL);
        selectOptionList.add(POWERPACK_REST_LABEL);
        selectOptionList.add(POWERPACK_NONE_LABEL);

        final PropertyModel<String> powerpackModel = new PropertyModel<>(this, "selectedPowerpack");
        powerpackDropdown = new DropDownChoice<String>("powerpackDropdown", powerpackModel, selectOptionList) {
            @Override
            protected boolean isDisabled(String object, int index, String selected) {
                switch (object) {
                    case POWERPACK_NEWS_AND_EVENT_LABEL:
                        return false;
                    case POWERPACK_REST_LABEL:
                        return true;
                    case POWERPACK_NONE_LABEL:
                        return true;
                    default:
                        return false;
                }

            }
        };

        powerpackDropdown.setNullValid(false);
        powerpackDropdown.add(new AjaxEventBehavior("onchange") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                final String selectedInput = powerpackDropdown.getInput();

                if (!(selectedInput == null && selectedInput.isEmpty())) {
                    selectedPowerpack = powerpackDropdown.getChoices().get(Integer.valueOf(selectedInput));
                    log.debug("#selected powerpack: {}", selectedPowerpack);
                    setComplete(true);
                    packDescription.setDefaultModel(new Model<>(getString(selectedPowerpack.replace("label", "description"))));
                    target.add(packDescription);
                } else {
                    setComplete(false);
                    packDescription.setDefaultModel(new Model<>(""));
                    target.add(packDescription);
                }
                target.add(powerpackDropdown);
            }
        });
        powerpackDropdown.setChoiceRenderer(new IChoiceRenderer<String>() {
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

        form.add(powerpackDropdown);

        CheckBox sampleContentCheckBox = new CheckBox("sampleContentCheckbox", new PropertyModel<Boolean>(this, "installSampleContent"));
        form.add(sampleContentCheckBox);

        RadioGroup<String> radioGroup = new RadioGroup<String>("templatesRadioGroup", new PropertyModel<String>(this, "selectedTemplatesType"));
        radioGroup.setRequired(true);

        radioGroup.add(new Radio<Boolean>("jspFilesystemRadio", Model.of(Boolean.TRUE)));
        radioGroup.add(new Radio<Boolean>("freemarkerFilesystemRadio", Model.of(Boolean.FALSE)).

                setEnabled(false)

        );
        radioGroup.add(new Radio<Boolean>("freemarkerRepositoryRadio", Model.of(Boolean.FALSE)).

                setEnabled(false)

        );
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

    public boolean isInstallSampleContentChecked() {
        return installSampleContent;
    }
}
