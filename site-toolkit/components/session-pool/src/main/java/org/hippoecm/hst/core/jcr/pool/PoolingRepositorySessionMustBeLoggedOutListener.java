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
package org.hippoecm.hst.core.jcr.pool;

import java.util.List;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolingRepositorySessionMustBeLoggedOutListener extends GenericEventListener {

    static Logger log = LoggerFactory.getLogger(PoolingRepositorySessionMustBeLoggedOutListener.class);

    // This is the statically decided pools list which should be refreshed on the specific content changes.
    protected List<PoolingRepository> poolingRepositories;

    // This is just for finding the disposable pools
    private MultipleRepository multipleRepository;

    public void setPoolingRepositories(List<PoolingRepository> poolingRepositories) {
        this.poolingRepositories = poolingRepositories;
    }

    public void setMultipleRepository(MultipleRepository multipleRepository) {
        this.multipleRepository = multipleRepository;
    }

    public void onEvent(EventIterator events) {
        boolean invalidatePools = false;

        while (events.hasNext()) {
            Event event = events.nextEvent();
            try {
                if (isEventOnSkippedPath(event)) {
                    continue;
                }
                if (eventIgnorable(event)) {
                    continue;
                }
            } catch (RepositoryException e) {
                continue;
            }

            invalidatePools = true;
            break;
        }

        if (invalidatePools) {
            log.debug("Event received. Invalidating session pools.");
            doInvalidation();
        }
    }

    @Override
    protected boolean isEventOnSkippedPath(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        if (eventPath.endsWith("/"+ HippoNodeType.HIPPO_PASSKEY)) {
            // we skip this property as it gets written if people login in the cms with remember me enabled
            return true;
        }
        return super.isEventOnSkippedPath(event);
    }

    private void doInvalidation() {
        long currentTimeMillis = System.currentTimeMillis();

        if (this.poolingRepositories != null) {
            for (PoolingRepository poolingRepository : this.poolingRepositories) {
                poolingRepository.clear();
                poolingRepository.setSessionsInvalidIfCreatedBeforeTimeMillis(currentTimeMillis);
            }
        }

        if (multipleRepository != null) {
            for (Repository repository : multipleRepository.getRepositoryMap().values()) {
                if (repository instanceof PoolingRepository) {
                    PoolingRepository poolingRepository = (PoolingRepository) repository;
                    poolingRepository.clear();
                    poolingRepository.setSessionsInvalidIfCreatedBeforeTimeMillis(currentTimeMillis);
                }
            }
        }
    }
}
