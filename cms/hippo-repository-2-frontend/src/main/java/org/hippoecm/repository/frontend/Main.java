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

import javax.jcr.RepositoryException;

import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.apache.wicket.Session;
import org.apache.wicket.protocol.http.WebApplication;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

public class Main extends WebApplication {

    private HippoRepository repository;

    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(false);
        
        String repositoryAddressConfigParam = "repository-address";
        String defaultRepositoryAddress = "rmi://localhost:1099/jackrabbit.repository";
        String repositoryAdress = getConfigurationParameter(repositoryAddressConfigParam, defaultRepositoryAddress);
        try {
            repository = HippoRepositoryFactory.getHippoRepository(repositoryAdress);
        } catch (RepositoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void destroy() {
        super.destroy();
        repository.close();
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

    public HippoRepository getRepository() {
        return repository;
    }

}
