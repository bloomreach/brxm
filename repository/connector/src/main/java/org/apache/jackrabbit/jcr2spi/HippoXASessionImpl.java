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
package org.apache.jackrabbit.jcr2spi;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.apache.jackrabbit.spi.XASessionInfo;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;

import javax.transaction.xa.XAResource;

/**
 * <code>HippoXASessionImpl</code> extends the regular session implementation with
 * access to the <code>XAResource</code>.
 */
public class HippoXASessionImpl extends HippoSessionImpl implements XASession {

    /**
     * The HippoXASessionInfo of this <code>SessionImpl</code>.
     */
    private final XASessionInfo sessionInfo;

    /**
     * Creates a new <code>HippoXASessionImpl</code>.
     *
     * @param repository the repository instance associated with this session.
     * @param sessionInfo the session info.
     * @param config the underlying repository configuration.
     * @throws RepositoryException if an error occurs while creating a session.
     */
    HippoXASessionImpl(XASessionInfo sessionInfo, Repository repository,
                  RepositoryConfig config) throws RepositoryException {
        super(sessionInfo, repository, config);
        this.sessionInfo = sessionInfo;
    }

    //--------------------------------< XASession >-----------------------------

    /**
     * @inheritDoc
     * @see org.apache.jackrabbit.jcr2spi.XASession#getXAResource()
     */
    public XAResource getXAResource() {
        return sessionInfo.getXAResource();
    }

}
