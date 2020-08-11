/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.service;

import java.util.Set;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

import com.google.common.collect.ImmutableSet;

import static javax.jcr.PropertyType.BOOLEAN;
import static javax.jcr.PropertyType.DATE;
import static javax.jcr.PropertyType.DOUBLE;
import static javax.jcr.PropertyType.LONG;
import static javax.jcr.PropertyType.STRING;
import static org.onehippo.repository.util.JcrConstants.JCR_PATH;
import static org.onehippo.repository.util.JcrConstants.JCR_PRIMARY_TYPE;
import static org.onehippo.repository.util.JcrConstants.JCR_UUID;

/**
 * AbstractSecurityNodeInfo is a base class for {@link UserImpl} and {@link GroupImpl} providing the shared (protected)
 * logic for {@link #isInfoProperty(Property) determining} which 'info' properties to be loaded and cached as String value.
 */
public abstract class AbstractSecurityNodeInfo {

    private static final Set<String> PROTECTED_JCR_PROPERTIES = ImmutableSet.of(JCR_PRIMARY_TYPE, JCR_PATH, JCR_UUID);

    private static final Set<Integer> SUPPORTED_INFO_PROPERTY_TYPES = ImmutableSet.of(STRING, BOOLEAN, DATE, DOUBLE, LONG);

    abstract protected Set<String> getProtectedPropertyNames();

    /**
     * Determine if a property qualified/allowed to load and cache its 'info' value as String value
     * <ul>
     *     <li>property must be single value</li>
     *     <li>property must be of {@link javax.jcr.PropertyType type}: STRING, BOOLEAN, DATE, DOUBLE, LONG
     *     <li>property must not be a {@link #PROTECTED_JCR_PROPERTIES protected JCR Property}: JCR_PRIMARY_TYPE, JCR_PATH, JCR_UUID</li>
     *     <li>property must not be a {@link #getProtectedPropertyNames()} defined by a downstream implementation (if any)</li>
     * </ul>
     *
     * @param property property to check
     * @return true if this is a 'info' property, false otherwise
     * @throws RepositoryException
     */
    protected boolean isInfoProperty(final Property property) throws RepositoryException {
        return (!property.isMultiple()
                && SUPPORTED_INFO_PROPERTY_TYPES.contains(property.getType())
                && !PROTECTED_JCR_PROPERTIES.contains(property.getName())
                && !getProtectedPropertyNames().contains(property.getName()));

    }
}
