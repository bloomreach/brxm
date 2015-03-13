/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.packaging;

import java.util.Set;

import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

/**
 * Used for template (& extra templates & sample data InstructionPackage). Properties for templates and sample data are:
 * <p>{@code sampleData}</p>
 * <p>{@code templateName}</p>
 * <p>{@code extraTemplates}</p>
 * Extra templates is only added in case of freemarker templates
 *
 *
 * @version "$Id$"
 * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PROP_SAMPLE_DATA
 * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PROP_TEMPLATE_NAME
 * @see org.onehippo.cms7.essentials.dashboard.utils.EssentialConst#PROP_EXTRA_TEMPLATES
 */
public class TemplateSupportInstructionPackage extends DefaultInstructionPackage {

    public TemplateSupportInstructionPackage() {
    }

    public TemplateSupportInstructionPackage(final String path) {
        setInstructionPath(path);
    }


    @Override
    public Set<String> groupNames() {
        final Boolean sampleData = Boolean.valueOf((String) getProperties().get(EssentialConst.PROP_SAMPLE_DATA));
        final String templateName = (String) getProperties().get(EssentialConst.PROP_TEMPLATE_NAME);
        final String templateGroup = Strings.isNullOrEmpty(templateName) ? EssentialConst.TEMPLATE_JSP : templateName;
        final boolean freemarker = templateGroup.equals(EssentialConst.TEMPLATE_FREEMARKER);
        if (sampleData) {
            if (freemarker) {
                return new ImmutableSet.Builder<String>()
                        .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                        .add(EssentialConst.PROP_SAMPLE_DATA)
                        .add(EssentialConst.PROP_EXTRA_TEMPLATES)
                        .add(templateGroup).build();

            }
            return new ImmutableSet.Builder<String>()
                    .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                    .add(EssentialConst.PROP_SAMPLE_DATA)
                    .add(templateGroup).build();
        }
        if (freemarker) {
            return new ImmutableSet.Builder<String>()
                    .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                    .add(EssentialConst.PROP_EXTRA_TEMPLATES)
                    .add(templateGroup).build();
        }
        return new ImmutableSet.Builder<String>()
                .add(EssentialConst.INSTRUCTION_GROUP_DEFAULT)
                .add(templateGroup).build();
    }

}
