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

import java.util.Collection;

/**
 * Mapper to convert a {@link Resource} object to a bean.
 */
public interface ResourceBeanMapper {

    /**
     * Map a {@link Resource} object to a bean of {@code type}.
     *
     * @param resource a {@link Resource} object to convert
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @return a {@link Resource} object to a bean of {@code type}
     * @throws ResourceException if the {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> T map(Resource resource, Class<T> beanType) throws ResourceException;

    /**
     * Map the child resources of the {@link ResourceCollection} to a new collection of {@code beanType} to return.
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @return a new collection of beans converted from the child resources of the {@code resourceCollection}
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType)
            throws ResourceException;

    /**
     * Map the child resources of the {@link ResourceCollection} to a new collection of {@code beanType} to return.
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @param offset start index of {@code resourceCollection} to convert from.
     *        If offset is a negative value, it's ignored, meaning zero index being effective.
     * @param limit max size of {@code resourceCollection} to convert from {@code offset} index.
     *        If limit is a negative value, it's ignored, meaning no limit being applied.
     * @return a new collection of beans converted from the child resources of the {@code resourceCollection}
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType, int offset,
            int limit) throws ResourceException;

    /**
     * Map the child resources of the {@link ResourceCollection} and push them to the given {@code targetBeanCollection}
     * of {@code beanType}.
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @param targetBeanCollection target collection of beans converted from the child resources of the {@code resourceCollection}.
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> void mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> targetBeanCollection) throws ResourceException;

    /**
     * Map the child resources of the {@link ResourceCollection} and push them to the given {@code targetBeanCollection}
     * of {@code beanType} from the {@code offset} index up to {@code limit} size at max.
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @param targetBeanCollection target collection of beans converted from the child resources of the {@code resourceCollection}.
     * @param offset start index of {@code resourceCollection} to convert from.
     *        If offset is a negative value, it's ignored, meaning zero index being effective.
     * @param limit max size of {@code resourceCollection} to convert from {@code offset} index.
     *        If limit is a negative value, it's ignored, meaning no limit being applied.
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> void mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> targetBeanCollection, int offset, int limit) throws ResourceException;

}
