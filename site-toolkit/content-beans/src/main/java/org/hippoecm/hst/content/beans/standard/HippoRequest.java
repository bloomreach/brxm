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
package org.hippoecm.hst.content.beans.standard;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Node(jcrType = "hippo:request")
public class HippoRequest extends HippoItem {
    
    static Logger log = LoggerFactory.getLogger(HippoRequest.class);
    
    private String type;
    private HippoItem document;
    private Long requestedDate;
    private String requestUsername;
    private String reason;
    
    public String getType() {
        if (type == null) {
            type = getProperty("type");
        }
        
        return type;
    }
    
    public HippoItem getDocument() {
        if (document == null) {
            String documentUuid = getProperty("document");
            
            if (documentUuid != null) {
                try {
                    javax.jcr.Node documentNode = getNode().getSession().getNodeByUUID(documentUuid);
                    document = (HippoItem) getObjectConverter().getObject(documentNode);
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to retrive document node.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to retrive document node. {}", e.toString());
                    }
                }
            }
        }
        
        return document;
    }
    
    public Long getRequestedDate() {
        if (requestedDate == null) {
            requestedDate = getProperty("reqdate");
        }
        
        return requestedDate;
    }
    
    public String getRequestUsername() {
        if (requestUsername == null) {
            requestUsername = getProperty("username");
        }
        
        return requestUsername;
    }
    
    public String getReason() {
        if (reason == null) {
            reason = getProperty("reason");
        }
        
        return reason;
    }
    
}
