/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.demo.servlet;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.site.HstServices;

/**
 * NOTE : never use this servlet for real purposes: This is only there for stress tests to be able to also 
 * change the HST configuration.
 * 
 * We only change the hst configuration with a single Thread at most : If there is already a request that is running for this servlet,
 * another request is ignored
 */
public class ModifyingHstConfigurationServlet  extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    private volatile boolean running = false;
    
    private volatile HstConfigurationModifier job = null;
    
    private static int counter = 1;
    
    private final static int SLEEP_TIME_MS = 100;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!HstServices.isAvailable()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        
        if(request.getParameter("start") != null && request.getParameter("start").equals("true") && !running ) {
            job = new HstConfigurationModifier();
            job.setDaemon(true);
            job.start();
            running = true;
        }
                
        if(request.getParameter("stop") != null && request.getParameter("stop").equals("true") && running) {
            if(job != null) {
                job.interrupt();
                job = null;
            }
            running = false;
        }
    }
    
    
    
    @Override
    public void destroy() {
        if(job != null) {
            job.interrupt();
            job = null;
        }
        super.destroy();
    }



    private class HstConfigurationModifier extends Thread {

        @Override
        public void run() {
            Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
            Credentials defaultCredentials = HstServices.getComponentManager().getComponent(
                    Credentials.class.getName() + ".writable");
            Session session = null;
            try {
                session = repository.login(defaultCredentials);
                while (true) {
                    try {
                        Node node = session.getNode("/hst:hst/hst:hosts");
                        node.setProperty("hst:defaulthostname", "127.0.0." + counter++);
                        session.save();
                        Thread.sleep(SLEEP_TIME_MS);
                    } catch (RepositoryException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // job interrupted. Stop running
                        if (session != null && session.isLive()) {
                            session.logout();
                        }
                        break;
                    }
                }
            } catch (LoginException e1) {
                e1.printStackTrace();
            } catch (RepositoryException e1) {
                e1.printStackTrace();
            }
            
        }

    }
    

    
    
    
}
