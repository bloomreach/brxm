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
package org.hippoecm.hst.service;

import java.io.Serializable;

import javax.jcr.Node;

import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.service.jcr.JCRService;


/**
 * @deprecated since 2.28.05 (CMS 7.9.1). Do not use any more. No replacement
 */
@Deprecated
public abstract class AbstractJCRService implements JCRService, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private transient JCRValueProvider valueProvider;
    
    public AbstractJCRService(Node jcrNode) {
        valueProvider = new JCRValueProviderImpl(jcrNode);
    }
    
    public JCRValueProvider getValueProvider() {
        return this.valueProvider;
    }
    
    public void closeValueProvider(boolean closeChildServices) {
        if(closeChildServices) {
            if(getChildServices() != null) {
                for(Service s : getChildServices()) {
                    if (s != null)
                        s.closeValueProvider(closeChildServices);
                }
            }
        }
        if(this.valueProvider != null) {
            this.valueProvider.detach();
        }
    }

}
