/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.HstConfigurationException;
import org.onehippo.cms7.services.hst.Channel;

/**
 * Composer service to manage 'hst:configuration' nodes at '/hst:hst/hst:configurations'
 */
public interface HstConfigurationService {

    void delete(Session session, String configurationPath) throws RepositoryException, HstConfigurationException;

    void delete(HstRequestContext requestContext, Channel channel) throws RepositoryException, HstConfigurationException;

    /**
     * Get all container nodes of type {@link HstNodeTypes#NODETYPE_HST_CONTAINERCOMPONENT} in the given
     * configuration path
     *
     * @param session
     * @param configurationPath The absolute jcr path of the 'hst:configuration' node
     * @return
     * @throws RepositoryException
     */
    List<Node> getContainerNodes(final Session session, final String configurationPath) throws RepositoryException;
}
