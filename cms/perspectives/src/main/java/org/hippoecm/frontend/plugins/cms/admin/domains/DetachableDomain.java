/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.domains;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.DomainAuth;

public final class DetachableDomain extends LoadableDetachableModel<DomainAuth> {

    private static final Logger log = LoggerFactory.getLogger(DetachableDomain.class);

    private final String path;

    /**
     * @param domain the Domain to wrap
     */
    public DetachableDomain(final DomainAuth domain) {
        this(domain.getPath());
        this.setObject(domain);
    }

    /**
     * @param path the path to the Domain
     */
    public DetachableDomain(final String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path argument can not be empty");
        }
        this.path = path;
    }

    /**
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * used for dataview with ReuseIfModelsEqualStrategy item reuse strategy
     *
     * @see org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof DetachableDomain) {
            final DetachableDomain other = (DetachableDomain) obj;
            return path.equals(other.path);
        }
        return false;
    }

    @Override
    protected DomainAuth load() {
        // loads contact from jcr
        try {
            return SecurityManagerHelper.getDomainsManager().getDomainAuth(path);
        } catch (RepositoryException e) {
            log.error("Unable to load domain, returning null", e);
            return null;
        }
    }
}
