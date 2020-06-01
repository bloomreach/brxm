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
package org.hippoecm.repository.security.group;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.security.ManagerContext;

/**
 * Dummy group implementation that is used when the provider doesn't support
 * it's own group manager implementation.
 */
public class DummyGroupManager extends AbstractGroupManager {


    public static final String PROVIDER_ID = "<dummyProvider>";

    public DummyGroupManager() {
        providerId = PROVIDER_ID;
    }

    public void initManager(ManagerContext context) {
        initialized = true;
    }

    public Set<String> backendGetMemberships(Node user) {
        return new HashSet<String>(0);
    }

    @Override
    public boolean isExternal() {
        return false;
    }

    public String getNodeType() {
        return HippoNodeType.NT_GROUP;
    }

    public boolean isCaseSensitive() {
        return true;
    }
}
