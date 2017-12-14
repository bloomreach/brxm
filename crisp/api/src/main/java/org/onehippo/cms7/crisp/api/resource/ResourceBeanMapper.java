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
     * Map the child resources of the {@link ResourceCollection} to the given {@code beanCollection} of {@code beanType}.
     * <P>
     * This method does not create a new collection of {@code beanType}, but push mapped beans into the given {@code beanCollection}
     * and return it.
     * </P>
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @param beanCollection target collection of beans converted from the child resources of the {@code resourceCollection}.
     * @return collection of beans converted from the child resources of the {@code resourceCollection}
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> beanCollection) throws ResourceException;

    /**
     * Map the child resources of the {@link ResourceCollection} to the given {@code beanCollection} of {@code beanType}
     * from the {@code offset} index up to {@code legnth} size at max.
     * <P>
     * This method does not create a new collection of {@code beanType}, but push mapped beans into the given {@code beanCollection}
     * by converting {@link Resource} items in {@code resourceCollection} from the given {@code offset} index
     * up to max {@code length} size items.
     * </P>
     *
     * @param resourceCollection a {@link ResourceCollection} object
     * @param beanType type of bean to which the {@link Resource} should be mapped
     * @param beanCollection target collection of beans converted from the child resources of the {@code resourceCollection}.
     * @param offset start index of {@code resourceCollection} to convert from.
     * @param length max size of {@code resourceCollection} to convert from {@code offset} index.
     * @return collection of beans converted from the child resources of the {@code resourceCollection}
     * @throws ResourceException if a {@code resource} cannot be mapped to the {@code beanType}
     */
    public <T> Collection<T> mapCollection(ResourceCollection resourceCollection, Class<T> beanType,
            Collection<T> beanCollection, int offset, int length) throws ResourceException;

}
