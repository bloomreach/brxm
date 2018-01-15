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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.onehippo.cms7.essentials.plugin.sdk.utils.EssentialConst;

import com.google.common.base.Strings;

/**
 * Used for template (& extra templates & sample data InstructionPackage). Properties for templates and sample data are:
 * <p>{@code sampleData}</p>
 * <p>{@code templateName}</p>
 * <p>{@code extraTemplates}</p>
 * Extra templates is only added in case of freemarker templates
 *
 *
 * @version "$Id$"
 * @see EssentialConst#PROP_SAMPLE_DATA
 * @see EssentialConst#PROP_TEMPLATE_NAME
 * @see EssentialConst#PROP_EXTRA_TEMPLATES
 */
public class TemplateSupportInstructionPackage extends DefaultInstructionPackage {

    @Override
    public Set<String> groupNames() {
        final Map<String, Object> props = getProperties();
        final Boolean extraTemplates = props.containsKey(EssentialConst.PROP_EXTRA_TEMPLATES)
                                     ? (Boolean) props.get(EssentialConst.PROP_EXTRA_TEMPLATES) : true;
        final Boolean sampleData = props.containsKey(EssentialConst.PROP_SAMPLE_DATA)
                                 ? (Boolean) props.get(EssentialConst.PROP_SAMPLE_DATA) : true;
        final String templateName = (String) props.get(EssentialConst.PROP_TEMPLATE_NAME);
        final String templateGroup = Strings.isNullOrEmpty(templateName) ? EssentialConst.TEMPLATE_JSP : templateName;
        final boolean freemarker = templateGroup.equals(EssentialConst.TEMPLATE_FREEMARKER);
        final Set<String> names = new HashSet<>();
        names.add(EssentialConst.INSTRUCTION_GROUP_DEFAULT);
        names.add(templateGroup);
        if (sampleData) {
            names.add(EssentialConst.PROP_SAMPLE_DATA);
        }
        // add extra templates only in case freemarker is used
        if (extraTemplates && freemarker) {
            names.add(EssentialConst.PROP_EXTRA_TEMPLATES);
        }
        return names;
    }

}
