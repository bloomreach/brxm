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

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.rmi.client.ClientRepositoryFactory;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebApplication;

public class Main extends WebApplication {

    private Session jcrSession;

    public Main() {
    }

    protected void init() {
        super.init();
        getDebugSettings().setAjaxDebugModeEnabled(true);
    }

    public Class getHomePage() {
        return Browser.class;
    }

    public static Session getSession() {
        Main main = (Main) RequestCycle.get().getApplication();
        if (main.jcrSession == null || !main.jcrSession.isLive()) {
            try {
                ClientRepositoryFactory repositoryFactory = new ClientRepositoryFactory();
                Repository repository = repositoryFactory.getRepository("rmi://localhost:1099/jackrabbit.repository");
                main.jcrSession = repository.login(new SimpleCredentials("username", "password".toCharArray()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ClassCastException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            } catch (LoginException e) {
                e.printStackTrace();
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
        return main.jcrSession;
    }

}
