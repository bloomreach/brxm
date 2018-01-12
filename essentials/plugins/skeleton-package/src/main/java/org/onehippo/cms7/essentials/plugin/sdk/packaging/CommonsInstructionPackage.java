/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.packaging;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.plugin.sdk.ctx.PluginContext;
import org.onehippo.cms7.essentials.plugin.sdk.instructions.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.model.MavenDependency;
import org.onehippo.cms7.essentials.plugin.sdk.model.TargetPom;
import org.onehippo.cms7.essentials.plugin.sdk.service.MavenDependencyService;
import org.onehippo.cms7.essentials.plugin.sdk.service.ProjectService;

/**
 * Adds all files that are shared between other installer packages,
 * like JSP/Freemarker includes, pagination, template skeleton etc.
 *
 * @version "$Id$"
 */
public class CommonsInstructionPackage extends TemplateSupportInstructionPackage {

    private static final MavenDependency ESSENTIALS_COMPONENTS
            = new MavenDependency(ProjectService.GROUP_ID_COMMUNITY, "hippo-essentials-components-hst", null, null, "provided");

    @Inject
    private MavenDependencyService dependencyService;

    @Override
    public Instruction.Status execute(final PluginContext context) {
        // add provided dependency to webfiles artifact so we have freemarker autocompletion support
        dependencyService.addDependency(TargetPom.REPOSITORY_DATA_WEB_FILES, ESSENTIALS_COMPONENTS);

        return super.execute(context);
    }

    @Override
    public String getInstructionPath() {
        return "/META-INF/commons_instructions.xml";
    }
}
