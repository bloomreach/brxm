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

package org.onehippo.cms7.essentials.setup;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.onehippo.cms7.essentials.documents.panels.DocumentsCndStep;
import org.onehippo.cms7.essentials.setup.panels.WelcomeStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class SetupPage extends WebPage implements IHeaderContributor {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(SetupPage.class);

    public SetupPage(final PageParameters parameters) {
        super(parameters);
        final AjaxWizardPanel wizard = new AjaxWizardPanel("wizard");
        wizard.addWizard(new WelcomeStep("Hippo Essentials setup"));
        wizard.addWizard(new WelcomeStep("Hippo Essentials setup"));
        wizard.addWizard(new WelcomeStep("Hippo Essentials setup"));
        wizard.addWizard(new WelcomeStep("Hippo Essentials setup"));
        add(wizard);
    }



}
