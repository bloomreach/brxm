/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.session.LoginException;

public class InvalidLoginPage extends PluginPage {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private final static String DEFAULT_KEY = "invalid.login";
    private final static Map<LoginException.CAUSE, String> causeKeys;

    static {
        causeKeys = new HashMap<LoginException.CAUSE, String>(2);
        causeKeys.put(LoginException.CAUSE.INCORRECT_CREDENTIALS, "invalid.login");
        causeKeys.put(LoginException.CAUSE.ACCESS_DENIED, "access.denied");
        causeKeys.put(LoginException.CAUSE.REPOSITORY_ERROR, "repository.error");
    }

    public InvalidLoginPage(final PageParameters parameters) {
        super();
        String key = DEFAULT_KEY;

        if (parameters != null) {
            Object loginException = (LoginException) parameters.get(LoginException.class.getName());
            
            if ((loginException != null) && (loginException instanceof LoginException)) {
                key = causeKeys.get(((LoginException) loginException).getLoginExceptionCause());
                key = StringUtils.isNotBlank(key) ? key : DEFAULT_KEY;
            }

        }

        info(new StringResourceModel(key, this, null).getString());
    }

}
