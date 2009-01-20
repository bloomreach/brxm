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
package org.hippoecm.hst.core.filters.observation;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.hippoecm.hst.caching.observation.EventListenerImpl;
import org.hippoecm.hst.jcr.JcrSessionFactory;
import org.hippoecm.hst.jcr.JcrSessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryObservationFilter implements Filter{
    
    private static final Logger log = LoggerFactory.getLogger(RepositoryObservationFilter.class);
    private volatile boolean isListenerRegistered = false;
    private Session observerSession;
    private EventListener listener;
    private FilterConfig filterConfig;
    
    
    public void init(FilterConfig filterConfig) throws ServletException {
      this.filterConfig = filterConfig;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
            synchronized (this) {
                if (!isListenerRegistered) {
                    registerEventListener(filterConfig);
                    isListenerRegistered = true;
                }
            }
            chain.doFilter(request, response);
    }
    
    public void destroy() {
        try {
            if (listener != null && observerSession != null && observerSession.getWorkspace() != null) {
                ObservationManager obMgr = observerSession.getWorkspace().getObservationManager();
                if (obMgr != null) {
                    obMgr.removeEventListener(listener);
                    observerSession.logout();
                }
                log.debug("Destroy succesfully disposed all jcr sessions");
            }
        } catch (UnsupportedRepositoryOperationException e) {
            log.warn("UnsupportedRepositoryOperationException during 'destroy()' of filter in disposing jcr sessions and unregistering listener. " + e.getMessage());
        } catch (RepositoryException e) {
            log.warn("RepositoryException during 'destroy()' of filter in disposing jcr sessions and unregistering listener. " + e.getMessage());
        }
    }

  private void registerEventListener(FilterConfig filterConfig) {
      observerSession = new JcrSessionFactoryImpl(filterConfig).getSession();
      
      ObservationManager obMgr;
      try {
          obMgr = observerSession.getWorkspace().getObservationManager();
          listener = new EventListenerImpl();
          obMgr.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                  "/",
                  true, null, null, true);
    
      } catch (UnsupportedRepositoryOperationException e) {
          log.error("Tried to perform something on the repository while adding eventlisteners that we cannot do",e);
      } catch (RepositoryException e) {
          log.error("Problem in the repository while addind event listeners",e);
      }
  }

}
