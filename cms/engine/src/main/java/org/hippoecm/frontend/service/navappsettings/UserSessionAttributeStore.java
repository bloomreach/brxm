/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.service.navappsettings;

import java.io.Serializable;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.session.UserSession;

class UserSessionAttributeStore implements SessionAttributeStore, Serializable {

    private final IModel<UserSession> userSessionSupplier;

    public UserSessionAttributeStore(final IModel<UserSession> userSessionSupplier) {
        this.userSessionSupplier = userSessionSupplier;
    }


    @Override
    public Serializable getAttribute(final String name) {
        return userSessionSupplier.getObject().getAttribute(name);
    }

    @Override
    public Session setAttribute(final String name, final Serializable value) {
        return userSessionSupplier.getObject().setAttribute(name, value);
    }
}
