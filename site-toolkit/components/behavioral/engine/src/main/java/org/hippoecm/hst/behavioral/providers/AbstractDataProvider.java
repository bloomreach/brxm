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

import org.hippoecm.hst.behavioral.BehavioralData;
import org.hippoecm.hst.behavioral.BehavioralDataProvider;
import org.hippoecm.hst.behavioral.BehavioralNodeTypes;


public abstract class AbstractDataProvider implements BehavioralDataProvider {

    private static final String SIZE_PROPERTY_NAME = BehavioralNodeTypes.BEHAVIORAL_PROVIDER_PROPERTY_SIZE;
    
    private static final int DEFAULT_SIZE = 100;
    
    private final String id;
    private final String name;
    protected int size = DEFAULT_SIZE;

    public AbstractDataProvider(String id, String name, Node node) throws RepositoryException {
        this.id = id;
        this.name = name;
        if (node.hasProperty(SIZE_PROPERTY_NAME)) {
            this.size = (int) node.getProperty(SIZE_PROPERTY_NAME).getLong();
        }
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getName() {
        return name;
    }
    
    protected static abstract class AbstractBehavioralData implements BehavioralData {

        private static final long serialVersionUID = 1L;

        private final String providerId;
        
        protected AbstractBehavioralData(String providerId) {
            this.providerId = providerId;
        }
        
        @Override
        public String getProviderId() {
            return providerId;
        }
        
    }

}