/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;

public interface ContainerItemComponentResourceInterface {


    /**
     * Returns all variants of this container item component. Note that the returned list might contain variants that
     * are not available any more in the variants store.
     *
     * @return all the configured unique variants (i.e. parameter prefixes) currently configured for this component
     */
    Response getVariants();


    /**
     * Retains all given variants. All other variants from this container item are removed.
     *
     * @param variants the variants to keep
     * @return the variants that have been removed from this container item
     */
    Response retainVariants(final String[] variants, long versionStamp);

    /**
     * Returns all parameters of a specific variant. The names of the parameters will be in the given locale.
     *
     * @param variant      the variant
     * @param localeString the desired locale of the parameter names
     * @return the values and translated names of the parameters of the given variant.
     */
    ContainerItemComponentRepresentation getVariant(String variant, String localeString);

    /**
     * Saves parameters for the given new variant. If a sub-set of the parameters is provided, only that sub-set is
     * changed and the other parameters are left as-is.
     *
     * If a new variant is provided, the old variant is removed and a new variant is created with the given
     * parameters. This effectively renames the old variant to the new one. Note that in this case, all parameters
     * for the new variant have to be provided.
     *
     * @param variantId the variant to update parameters of, or (if a newVariantId is provided) remove.
     * @param newVariantId the new variant to store parameters for. Can be null, in which case only the given
     *                     variant is updated.
     * @param versionStamp the version (time) stamp against used for optimistic locking: in case the {@code versionStamp}
     *                     argument does not match the stored versionStamp on the container item, an error response will
     *                     be returned
     * @param params     the parameters to store
     * @return whether saving the parameters went successfully or not.
     */
    Response moveAndUpdateVariant(String variantId, String  newVariantId, long versionStamp, final MultivaluedHashMap<String, String> params);

    Response deleteVariant(String variantId, long versionStamp);
}
