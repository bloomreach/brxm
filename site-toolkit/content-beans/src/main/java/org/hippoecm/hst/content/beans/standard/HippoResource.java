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

import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bean mapping class for the 'hippo:resource' document type
 *
 */
@Node(jcrType = "hippo:resource")
public class HippoResource extends HippoItem implements HippoResourceBean {


    private static Logger log = LoggerFactory.getLogger(HippoResource.class);
    /**
     * 
     * @return the number of bytes of binary stored in this resoure
     */
    public long getLength(){
        if(this.getNode() == null) {
            log.warn("Cannot get length for detached node");
            return 0;
        }
        // a hippo:resource has a mandatory jcr:data property by cnd definition so not testing needed
        try {
            return this.getNode().getProperty("jcr:data").getLength();
        }catch (RepositoryException e) {
           log.warn("Error while fetching binary data length : {}", e);
        }
        return 0;
    }
    
}
