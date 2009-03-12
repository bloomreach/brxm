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
package org.hippoecm.hst.ocm;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType="hippo:document", discriminator=false)
public class HippoStdDocument extends HippoStdNode implements SessionAware {

    private static Logger log = LoggerFactory.getLogger(HippoStdDocument.class);

    private transient Session session;
    private String stateSummary;
    private String state;
    private HippoStdCollection parentCollection;

    public Session getSession() {
        return this.session;
    }
    
    public void setSession(Session session) {
        this.session = session;
    }
    
    @Field(jcrName="hippostd:stateSummary") 
    public String getStateSummary() {
        return this.stateSummary;
    }
    
    public void setStateSummary(String stateSummary) {
        this.stateSummary = stateSummary;
    }
    
    @Field(jcrName="hippostd:state") 
    public String getState() {
        return this.state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public HippoStdCollection getCollection() {
        if (this.parentCollection == null) {
            if (this.session != null && getNode() != null && getSimpleObjectConverter() != null) {
                try {
                    javax.jcr.Node parent = getNode().getParent();
                    
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        parent = parent.getParent();
                    }
                    
                    this.parentCollection = (HippoStdCollection) getSimpleObjectConverter().getObject(this.session, parent.getPath());
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve parent collections: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve parent collections: {}", e.getMessage());
                    }
                }

                // Now detach the session because the session is probably from the pool.
                setSession(null);
            }
        }
        
        return this.parentCollection;
    }
    
}