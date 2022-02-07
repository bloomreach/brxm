/*
 *  Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Locale;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;

public class LoginResourceModel extends ClassResourceModel {
    public LoginResourceModel(final String key, final Object... parameters) {
        this(key, LoginPlugin.class, getLocale(), getStyle(), parameters);
    }

    public LoginResourceModel(final String key, final Class<?> clazz, final Object... parameters) {
        super(key, clazz, getLocale(), getStyle(), parameters);
    }

    private static String getStyle() {
        return Session.get().getStyle();
    }

    private static IModel<Locale> getLocale() {
        return () -> Session.exists()
                ? Session.get().getLocale()
                : Locale.getDefault();
    }
}
