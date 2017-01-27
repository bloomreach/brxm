/**
 * Copyright 2013-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.resourcebundle.internal;

import org.hippoecm.hst.resourcebundle.ResourceBundleFamily;
import org.hippoecm.hst.resourcebundle.ResourceBundleRegistry;

/**
 * MutableResourceBundleRegistry
 * <P>
 * MutableResourceBundleRegistry allows to manage the internal resource families in the registry.
 * </P>
 * <P>
 * @deprecated the mutable parts of an implementation of the resource bundle registry are internal
 * to HST and should be nor implemented nor used by project code.
 * </P>
 */
@Deprecated
public interface MutableResourceBundleRegistry extends ResourceBundleRegistry {

    /**
     * Registers the internal resource bundle family specified by the basename.
     * @param basename
     * @param bundleFamily
     */
    void registerBundleFamily(String basename, ResourceBundleFamily bundleFamily);

    /**
     * Register the internal resource bundle family specified by basename and live/preview scope.
     *
     * @param basename     basename of the bundle family (for look-up)
     * @param preview      live(false)/preview(true) scpe of the bundle family
     * @param bundleFamily to-be-registered bundle family
     */
    void registerBundleFamily(String basename, boolean preview, ResourceBundleFamily bundleFamily);

    /**
     * Unregisters the internal resource bundle family specified by the basename if found.
     * @param basename
     */
    void unregisterBundleFamily(String basename);

    /**
     * Unregisters an internal resource bundle family by (repository) identifier (handle UUID), if found.
     * @param identifier
     * @param preview
     */
    void unregisterBundleFamily(String identifier, boolean preview);

    /**
     * Unregisters all the internal resource bundle families in the registry.
     */
    void unregisterAllBundleFamilies();

}
