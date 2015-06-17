/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.editor;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.validation.IValidator;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

public class NamespaceValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z]+$");
    private static final Pattern URL_PATTERN = Pattern.compile("^http:.*/[0-9].[0-9]$");

    private NamespaceValidator() {
    }

    public static void checkName(String name) throws Exception {
        if (name == null || "".equals(name)) {
            throw new Exception("No name specified");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new Exception("Invalid name; only alphabetic characters allowed in lower- or uppercase");
        }
    }

    public static void checkURI(String name) throws Exception {
        if (name == null) {
            throw new Exception("No URI specified");
        }
        if (!URL_PATTERN.matcher(name).matches()) {
            throw new Exception("Invalid URL; ");
        }
    }

    public static IValidator<String> createNameValidator() {
        return validatable -> {
            final String name = validatable.getValue();
            if (StringUtils.isBlank(name)) {
                validatable.error(message -> new ClassResourceModel("namespace.name.empty", NamespaceValidator.class).getObject());
            } else if (!NAME_PATTERN.matcher(name).matches()) {
                validatable.error(message -> new ClassResourceModel("namespace.name.invalid", NamespaceValidator.class).getObject());
            }
        };
    }

    public static IValidator<String> createUrlValidator() {
        return validatable -> {
            final String url = validatable.getValue();
            if (StringUtils.isBlank(url)) {
                validatable.error(message -> new ClassResourceModel("namespace.url.empty", NamespaceValidator.class).getObject());
            } else if (!URL_PATTERN.matcher(url).matches()) {
                validatable.error(message -> new ClassResourceModel("namespace.url.invalid", NamespaceValidator.class).getObject());
            }
        };
    }
}
