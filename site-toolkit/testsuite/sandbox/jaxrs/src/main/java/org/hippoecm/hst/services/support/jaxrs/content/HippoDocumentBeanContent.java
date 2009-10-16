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
package org.hippoecm.hst.services.support.jaxrs.content;

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;

@XmlRootElement(name = "document")
public class HippoDocumentBeanContent extends HippoBeanContent {
    
    private String canonicalHandleUuid;
    
    public HippoDocumentBeanContent() {
        super();
    }
    
    public HippoDocumentBeanContent(HippoDocumentBean bean) throws RepositoryException {
        super(bean);
        
        this.canonicalHandleUuid = bean.getCanonicalHandleUUID();
    }
    
    public String getCanonicalHandleUuid() {
        return canonicalHandleUuid;
    }
    
    public void setCanonicalHandleUuid(String canonicalHandleUuid) {
        this.canonicalHandleUuid = canonicalHandleUuid;
    }
    
}
