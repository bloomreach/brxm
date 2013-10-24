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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.onehippo.cms7.essentials.dashboard.panels.DropdownPanel;
import org.onehippo.cms7.essentials.dashboard.panels.EventListener;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class SelectPowerpackStep extends EssentialsWizardStep {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(SelectPowerpackStep.class);
    private final DropdownPanel powerpackDropdown;
    public SelectPowerpackStep(final String title) {
        super(title);
        Form<?> form = new Form<>("form");

        final List<String> powerpacks = new ArrayList<>();
        powerpacks.add("News and events powerpack");
        powerpacks.add("Enterprise forms powerpack");

        powerpackDropdown = new DropdownPanel("powerpacks", "Select a powerpack", form, powerpacks, new EventListener<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSelected(final AjaxRequestTarget target, final Collection<String> selectedItems) {
                final String selectedPowerpack = powerpackDropdown.getSelectedItem();
                if (Strings.isNullOrEmpty(selectedPowerpack)) {
                    log.debug("No powerpack selected");
                    return;
                }

                log.info("selectedPowerpack: {}", selectedPowerpack);
            }
        });
        add(form);
    }

    @Override
    public void applyState() {

    }

}
