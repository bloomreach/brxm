/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.io.IClusterable;

public interface LoginHandler extends IClusterable  {

    /**
     * Called when a user has changed the login locale.
     * @param selectedLocale the newly selected locale
     * @param target the current Ajax request target
     */
    void localeChanged(final String selectedLocale, final AjaxRequestTarget target);

    /**
     * Called when a user has logged in successfully.
     */
    void loginSuccess();
}
