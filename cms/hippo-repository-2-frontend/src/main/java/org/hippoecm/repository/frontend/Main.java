/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.frontend;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.repository.frontend.Home;
import org.hippoecm.repository.frontend.UserSession;

public class Main extends WebApplication {
    
    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(false);
    }

    public Class getHomePage() {
        return Home.class;
    }

    public Session newSession(Request request, Response response) {
        return new UserSession(this, request);
    }
    
    public String getConfigurationParameter(String parameterName, String defaultValue) {
        String result = getInitParameter(parameterName);
        if (result == null || result.equals("")) {
            result = getServletContext().getInitParameter(parameterName);
        }
        if (result == null || result.equals("")) {
            result = defaultValue;
        }
        return result; 
    }
    
}
