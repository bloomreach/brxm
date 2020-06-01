/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.api.resource;

import java.util.Map;

/**
 * Responsible for resolving a link (type of {@link ResourceLink}) for a {@link Resource} representation.
 */
public interface ResourceLinkResolver {

    /**
     * Resolves a {@link ResourceLink} for the given {@code resource}.
     * @param resource resource representation
     * @return a {@link ResourceLink} for the given {@code resource}
     * @throws ResourceException if resource resolution operation fails
     */
    ResourceLink resolve(Resource resource) throws ResourceException;

    /**
     * Resolves a {@link ResourceLink} for the given {@code resource} with passing {@code linkVariables} that can be
     * used by implementation to expand its internal link generation template.
     * <p>How the {@code linkVariables} is used in link generation template expansion is totally up to an implementation.</p>
     * @param resource resource representation
     * @param linkVariables the variables to expand the internal link generation template
     * @return a {@link ResourceLink} for the given {@code resource}
     * @throws ResourceException if resource resolution operation fails
     */
    ResourceLink resolve(Resource resource, Map<String, Object> linkVariables) throws ResourceException;

}
