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
package org.hippocms.repository.webapp;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippocms.repository.webapp.model.JcrSessionProvider;

public class Main extends WebApplication {

    public Main() {
    }

    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(true);
    }

    public Class getHomePage() {
        return Browser.class;
    }

    public Session newSession(Request request, Response response) {
        return new JcrSessionProvider(this, request, getRepositoryAddress());
    }

    public String getRepositoryAddress() {
        String address = getInitParameter("repository-address");
        if (address == null || address.equals("")) {
            address = getServletContext().getInitParameter("repository-address");
        }
        if (address == null || address.equals("")) {
            address = "rmi://localhost:1099/jackrabbit.repository";
        }
        return address;
    }

}
