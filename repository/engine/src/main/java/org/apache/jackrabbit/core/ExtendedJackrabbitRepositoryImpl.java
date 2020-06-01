/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.ClusterContext;
import org.apache.jackrabbit.core.config.RepositoryConfig;

/**
 * ExtendedJackrabbitRepositoryImpl extends the Jackrabbit RepositoryImpl to provide (protected) access to
 * package private members for the org.hippoecm.repository.jackrabbit.RepositoryImpl which is (now) extending this
 * class.
 *
 */
public class ExtendedJackrabbitRepositoryImpl extends RepositoryImpl {

    protected ExtendedJackrabbitRepositoryImpl(final RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
    }

    /**
     * @return a new ExternalEventListener instance, which is package private within the Jackrabbit RepositoryImpl
     */
    protected ClusterContext createClusterContext() {
        return new ExternalEventListener();
    }
}
