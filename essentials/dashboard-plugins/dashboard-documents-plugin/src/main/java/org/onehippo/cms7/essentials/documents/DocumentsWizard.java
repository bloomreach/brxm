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

package org.onehippo.cms7.essentials.documents;

import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.wizard.AjaxWizardPanel;
import org.onehippo.cms7.essentials.documents.panels.BeansWriterStep;
import org.onehippo.cms7.essentials.documents.panels.DocumentsCndStep;
import org.onehippo.cms7.essentials.documents.panels.DocumentsTemplateStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DocumentsWizard extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(DocumentsWizard.class);

    public DocumentsWizard(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);
        final AjaxWizardPanel panel = new AjaxWizardPanel("wizard") {
            @Override
            public void onFinish() {
            }
        };
        panel.addWizard(new DocumentsCndStep(this, "Register document types"));
        panel.addWizard(new DocumentsTemplateStep(this, "Register document templates"));
        panel.addWizard(new BeansWriterStep(this, "Write HST beans"));
        add(panel);
    }
}
