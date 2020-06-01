/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.login;

import java.util.List;

import org.apache.wicket.util.io.IClusterable;

public class LoginConfig implements IClusterable {

    private final boolean autoComplete;
    private final List<String> locales;
    private final String[] supportedBrowsers;

    public LoginConfig(final boolean autoComplete, final List<String> locales, final String[] supportedBrowsers) {

        if (locales == null || locales.isEmpty()) {
            throw new IllegalArgumentException("Argument locales can not be null or empty");
        }

        this.autoComplete = autoComplete;
        this.locales = locales;
        this.supportedBrowsers = supportedBrowsers;
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    public List<String> getLocales() {
        return locales;
    }

    public String[] getSupportedBrowsers() {
        return supportedBrowsers;
    }
}
