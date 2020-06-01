/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

public class DefaultPooledSessionRefresher implements PooledSessionRefresher {
    
    private boolean checkLivenessBeforeRefresh = true;
    
    public boolean isCheckLivenessBeforeRefresh() {
        return checkLivenessBeforeRefresh;
    }

    public void setCheckLivenessBeforeRefresh(boolean checkLivenessBeforeRefresh) {
        this.checkLivenessBeforeRefresh = checkLivenessBeforeRefresh;
    }

    @Override
    public void refresh(PooledSession pooledSession, boolean keepChanges) throws RepositoryException {
        // HSTTWO-1337: Hippo Repository requires to check isLive() before logout(), refresh(), etc.
        if (checkLivenessBeforeRefresh) {
            if (pooledSession.isLive()) {
                if (keepChanges) {
                    pooledSession.refresh(true);
                } else {
                    pooledSession.localRefresh();
                }
            }
        } else {
            if (keepChanges) {
                pooledSession.refresh(true);
            } else {
                pooledSession.localRefresh();
            }
        }
    }

}
