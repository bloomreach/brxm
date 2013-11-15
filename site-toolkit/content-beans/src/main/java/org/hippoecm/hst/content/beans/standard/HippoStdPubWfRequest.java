/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;

import javax.jcr.Property;
import javax.jcr.PropertyType;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Node(jcrType = "hippostdpubwf:request")
public class HippoStdPubWfRequest extends HippoItem implements HippoRequestBean {
    
    static Logger log = LoggerFactory.getLogger(HippoStdPubWfRequest.class);
    
    private HippoBean document;
    
    public String getType() {
        return getProperty("hippostdpubwf:type");
    }
    
    public HippoBean getDocument() {
        if (document == null) {
            javax.jcr.Node jcrRequestNode = this.getNode();
            try {
                Property prop = jcrRequestNode.getProperty("hippostdpubwf:document");
                if(prop.getType() == PropertyType.REFERENCE) {
                    javax.jcr.Node documentNode = prop.getNode();
                    document = (HippoBean) getObjectConverter().getObject(documentNode);
                } else {
                    log.warn("Unexpected property type for 'hippostdpubwf:document'");
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.info("Failed to retrieve document node.", e);
                } else if (log.isWarnEnabled()) {
                    log.info("Failed to retrieve document node. {}", e.getMessage());
                }
            } 
        }
        return document;
    }
    
    public String getRequestUsername() {
        return getProperty("hippostdpubwf:username");
    }
    
    public String getReason() {
        return getProperty("hippostdpubwf:reason");
    }

    public Calendar getRequestDate() {
        return getProperty("hippostdpubwf:reqdate");
    }

}
