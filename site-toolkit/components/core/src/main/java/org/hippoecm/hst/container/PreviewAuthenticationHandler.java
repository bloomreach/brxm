/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container;

import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.container.ContainerException;

public class PreviewAuthenticationHandler {

    final static Cache<String, SimpleCredentials> cmsSCIDRegistry = CacheBuilder.newBuilder().build();

    public PreviewAuthenticationContext handle(final VirtualHosts vHosts, final HstContainerRequest request, final HttpServletResponse res) throws ContainerException {

        return new PreviewAuthenticationContext(vHosts, request, res, cmsSCIDRegistry);

    }

}
