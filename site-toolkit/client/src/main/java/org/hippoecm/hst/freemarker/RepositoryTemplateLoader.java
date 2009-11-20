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

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.site.HstServices;

import freemarker.cache.TemplateLoader;

public class RepositoryTemplateLoader implements TemplateLoader{


    private Repository repository;
    
    private Credentials defaultCredentials;
    
    public void closeTemplateSource(Object templateSource) throws IOException {
        return;
    }

    /*
     * TODO return a self validatin object registring itself to some event aware cache : it must implement an equals method such that:
     * newlyFoundSource.equals(cachedTemplate.source); returns false if the source has changed in the repository: see 
     * freemarker.cache.TemplateCache#getTemplate
     */
    
    public Object findTemplateSource(String templateSource) throws IOException {
       // if templateSource starts with '/repository://' we need the repository loader. Otherwise, return null
       if(templateSource != null && templateSource.startsWith("/repository://")) {
        // the name is our identifier of the source
           return templateSource;
       }
        
       // the templateSource is not a repository source: return null
       return null;
    }

    public long getLastModified(Object templateSource) {
        return 0;
    }

    public Reader getReader(Object templateSource, String encoding) throws IOException {
        String template = null;
        Session session = null;
        try {
            session = getSession();
            String repositoryLocation = "/" + ((String)templateSource).substring("/repository://".length());
            if(session.itemExists(repositoryLocation)) {
                Item item = session.getItem(repositoryLocation);
                if(item.isNode()) {
                    template = ((Node)item).getProperty("template").getValue().getString();
                } else {
                    template = ((Property)item).getValue().getString();
                }
            }
            
        } catch (RepositoryException e) {
            e.printStackTrace();
        } finally {
            if(session != null) {
                session.logout();
            }
        }
        
        if(template == null ) {
            template = "Template source '"+ ((String)templateSource).substring(1) +"' not found in the repository. ";
        }
        return new StringReader(template);
    }

    
    
    private Session getSession() throws RepositoryException {
        Session session = null;
    
        if (this.repository == null) {
            if (HstServices.isAvailable()) {
                this.defaultCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default");
                this.repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
            }
        }

        if (this.repository != null) {
            if (this.defaultCredentials != null) {
                session = this.repository.login(this.defaultCredentials);
            } else {
                session = this.repository.login();
            }
        }
     
        return session;
    }
}
