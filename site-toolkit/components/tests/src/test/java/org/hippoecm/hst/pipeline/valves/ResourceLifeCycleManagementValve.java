/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pipeline.valves;

import javax.jcr.Repository;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.container.AbstractBaseOrderableValve;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.hippoecm.hst.core.jcr.pool.PoolingRepository;

public class ResourceLifeCycleManagementValve extends AbstractBaseOrderableValve {

    private Repository repository;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }
    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        if (repository instanceof MultipleRepository) {
            final ResourceLifecycleManagement[] resourceLifecycleManagements = ((MultipleRepository) repository).getResourceLifecycleManagements();
            for (ResourceLifecycleManagement resourceLifecycleManagement : resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeResourcesAndReset();
            }
        } else if (repository instanceof PoolingRepository) {
            ((PoolingRepository) repository).getResourceLifecycleManagement().disposeResourcesAndReset();
        }
    }

    @Override
    public void initialize() throws ContainerException {

    }

    @Override
    public void destroy() {

    }

}
