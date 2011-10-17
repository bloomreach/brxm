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
package org.hippoecm.hst.behavioral.providers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.behavioral.BehavioralData;
import org.hippoecm.hst.behavioral.Rule;

public class AuthorizationDataProvider extends AbstractDataProvider {

    public AuthorizationDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
        this.size = 1;
    }
    
    @Override
    public boolean isSessionLevel() {
        return false;
    }

    @Override
    public boolean evaluate(Rule rule, BehavioralData data) {
        
        if (!(data instanceof AuthorizationBehavioralData)) {
            throw new IllegalArgumentException("BehavioralData is not of the expected type");
        }
        
        return ((AuthorizationBehavioralData) data).isAuthorized;
    }

    @Override
    public BehavioralData updateBehavioralData(BehavioralData behavioralData, HttpServletRequest request)
            throws IllegalArgumentException {
        
        if(behavioralData != null && !(behavioralData instanceof AuthorizationBehavioralData)) {
            throw new IllegalArgumentException("BehavioralData is not of the expected type.");
        }
        
        if (behavioralData == null) {
            behavioralData = new AuthorizationBehavioralData(getId());
        }
        
        ((AuthorizationBehavioralData) behavioralData).isAuthorized = request.getUserPrincipal() != null;
        
        return behavioralData;
    }

    private static class AuthorizationBehavioralData extends AbstractBehavioralData {

        private static final long serialVersionUID = 1L;

        private boolean isAuthorized = false;
        
        protected AuthorizationBehavioralData(String providerId) {
            super(providerId);
        }
        
    }
}
