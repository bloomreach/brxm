/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.freemarker;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.site.HstServices;

import freemarker.cache.TemplateLoader;

public class RepositoryTemplateLoader implements TemplateLoader{


    private Repository repository;
    
    private Credentials defaultCredentials;
    
    private Session observerSession;
    
    private Map<String, RepositorySource> cache =  Collections.synchronizedMap(new HashMap<String, RepositorySource>());
    
    public void closeTemplateSource(Object templateSource) throws IOException {
        return;
    }

    public Object findTemplateSource(String templateSource) throws IOException {
        
        if(templateSource != null && templateSource.startsWith("/repository://")) {
           if (repository == null) {
              synchronized (this) {
                  doInit();
              }
           }
           if(repository == null) {
               return null;
           }
           String absPath = "/" + ((String)templateSource).substring("/repository://".length());
           RepositorySource source = cache.get(absPath);
           if(source != null) {
               return source;
           } 
           synchronized(this) {
               source = cache.get(absPath);
               if(source == null) {
                   source = getRepositoryTemplate(absPath);
                   cache.put(absPath, source);
               }
           }
           return source;
       }
       // the templateSource is not a repository source: return null
       return null;
    }

    public long getLastModified(Object templateSource) {
        if(templateSource instanceof RepositorySource) {
            return ((RepositorySource)templateSource).getPlaceHolderLastModified();
        } else {
            // cannot happen
            return -1;
        }
        
    }

    public Reader getReader(Object templateSource, String encoding) throws IOException {
        if(templateSource instanceof RepositorySource) {
            return new StringReader(((RepositorySource)templateSource).getTemplate());
        } else {
            // cannot happen
        }
        return null;
    }
    
    private void doInit() {
        if(repository != null) {
            return;
        }
        
        if (HstServices.isAvailable()) {
            this.defaultCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default");
            this.repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        }
        // do not log out observerSession!
        try {
             observerSession = getSession();
             ObservationManager obMgr;
             obMgr = observerSession.getWorkspace().getObservationManager();
             EventListener listener = new TemplateChangeListener();
             // TODO : only catch event from hst:script nodetype
             String[] nodeTypes = {HstNodeTypes.NODETYPE_HST_SCRIPT};
             obMgr.addEventListener(listener, Event.NODE_ADDED | Event.PROPERTY_ADDED | Event.NODE_REMOVED
                     | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, "/", true, null, nodeTypes, true);
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
     }

    private RepositorySource getRepositoryTemplate(String absPath) {
        String template = null;
        Session session = null;
        try {
            session = getSession();
            if(session.itemExists(absPath)) {
                Item item = session.getItem(absPath);
                if(item.isNode()) {
                    template = ((Node)item).getProperty(HstNodeTypes.SCRIPT_PROPERTY_TEMPLATE).getValue().getString();
                } else {
                    template = ((Property)item).getValue().getString();
                }
                return new RepositorySource(template);
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
        } finally {
            if(session != null) {
                session.logout();
            }
        }
        if(template == null ) {
            template = "Template source '"+ absPath +"' not found in the repository. ";
        }
        return RepositorySource.repositorySourceNotFound;
    }
    
    private Session getSession() throws RepositoryException {
        Session session = null;
        if (this.repository != null) {
            if (this.defaultCredentials != null) {
                session = this.repository.login(this.defaultCredentials);
            } else {
                session = this.repository.login();
            }
        }
     
        return session;
    }
    
    
    private class TemplateChangeListener implements EventListener {
        
        public void onEvent(EventIterator events) {
            while(events.hasNext()){
                Event event = events.nextEvent();
                try {
                    String toEvict = event.getPath();
                    cache.remove(toEvict);
                    // and evict the parent (if a property changed, we need to evict the parent which is the node):
                    if (toEvict.indexOf("/") > -1) {
                        toEvict = toEvict.substring(0, toEvict.lastIndexOf("/"));
                        cache.remove(toEvict);
                    }
                } catch (RepositoryException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
