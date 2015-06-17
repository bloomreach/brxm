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

    private enum ValidationResult { OK, ERROR_EMPTY, ERROR_INVALID }

    private NamespaceValidator() {
    }

    public static void checkName(String name) throws Exception {
        switch (validateName(name)) {
            case ERROR_EMPTY:
                throw new Exception("No name specified");
            case ERROR_INVALID:
                throw new Exception("Invalid name; only alphabetic characters allowed in lower- or uppercase");
        }
    }

    public static void checkURI(String name) throws Exception {
        switch (validateUrl(name)) {
            case ERROR_EMPTY:
                throw new Exception("No URI specified");
            case ERROR_INVALID:
                throw new Exception("Invalid URL; ");
        }
    }

    public static IValidator<String> createNameValidator() {
        return validatable -> {
            final String name = validatable.getValue();
            switch (validateName(name)) {
                case ERROR_EMPTY:
                    validatable.error(message -> translate("namespace.name.empty"));
                    break;
                case ERROR_INVALID:
                    validatable.error(message -> translate("namespace.name.invalid"));
                    break;
            }
        };
    }

    public static IValidator<String> createUrlValidator() {
        return validatable -> {
            final String url = validatable.getValue();
            switch (validateUrl(url)) {
                case ERROR_EMPTY:
                    validatable.error(message -> translate("namespace.url.empty"));
                    break;
                case ERROR_INVALID:
                    validatable.error(message -> translate("namespace.url.invalid"));
                    break;
            }
        };
    }

    private static ValidationResult validateName(final String name) {
        if (StringUtils.isBlank(name)) {
            return ValidationResult.ERROR_EMPTY;
        } else if (!NAME_PATTERN.matcher(name).matches()) {
            return ValidationResult.ERROR_INVALID;
        }
        return ValidationResult.OK;
    }

    private static ValidationResult validateUrl(final String url) {
        if (StringUtils.isBlank(url)) {
            return ValidationResult.ERROR_EMPTY;
        } else if (!URL_PATTERN.matcher(url).matches()) {
            return ValidationResult.ERROR_INVALID;
        }
        return ValidationResult.OK;
    }

    private static String translate(final String key) {
        return new ClassResourceModel(key, NamespaceValidator.class).getObject();
    }

}
