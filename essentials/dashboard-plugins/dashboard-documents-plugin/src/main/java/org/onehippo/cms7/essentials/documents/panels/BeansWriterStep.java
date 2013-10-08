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

package org.onehippo.cms7.essentials.documents.panels;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.BeanWriterUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.dashboard.wizard.EssentialsWizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BeansWriterStep extends EssentialsWizardStep {


    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BeansWriterStep.class);
    private final CheckBox checkBox;
    private final DashboardPlugin parent;
    private boolean createBeans;
    final transient Path namespacePath;
    public BeansWriterStep(final DashboardPlugin owner, final String title) {
        super(title);

        this.parent = owner;
        final String basePath = ProjectUtils.getBaseProjectDirectory();
        namespacePath = new File(basePath + File.separator + "bootstrap").toPath();
        checkBox = new AjaxCheckBox("checkBox", new PropertyModel<Boolean>(this, "createBeans")){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                // do nothing
            }
        };
        final Form<?> form = new Form<>("form");
        form.add(checkBox);
        add(form);

    }

    @Override
    public void applyState() {
        if (createBeans) {
            final PluginContext context = parent.getContext();
            final List<MemoryBean> memoryBeans = BeanWriterUtils.buildBeansGraph(namespacePath, context, EssentialConst.SOURCE_PATTERN_JAVA);
            BeanWriterUtils.addMissingMethods(context, memoryBeans, EssentialConst.FILE_EXTENSION_JAVA);
        }
    }
}
