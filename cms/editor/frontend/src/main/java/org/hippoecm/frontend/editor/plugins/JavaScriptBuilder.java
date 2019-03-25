/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.frontend.editor.plugins;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.template.PackageTextTemplate;

class JavaScriptBuilder {

    private final PackageTextTemplate template;
    private final Map<String, String> variables;

    JavaScriptBuilder(final Class<?> clazz, final String template) {
        this.template = new PackageTextTemplate(clazz, template);
        variables = new HashMap<>();
    }

    void setVariable(final String name, final String value) {
        final String jsSafeString = StringEscapeUtils.escapeJavaScript(StringUtils.defaultString(value));
        variables.put(name, jsSafeString);
    }

    String build() {
        return template.asString(variables);
    }
}
