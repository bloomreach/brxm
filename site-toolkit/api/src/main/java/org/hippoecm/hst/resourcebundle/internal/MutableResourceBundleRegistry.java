/**
 * Copyright 2013 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
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
 */
public interface MutableResourceBundleRegistry extends ResourceBundleRegistry {

    /**
     * Registers the internal resource bundle family specified by the basename.
     * @param basename
     * @param bundleFamily
     */
    void registerBundleFamily(String basename, ResourceBundleFamily bundleFamily);

    /**
     * Unregisters the internal resource bundle family specified by the basename if found.
     * @param basename
     */
    void unregisterBundleFamily(String basename);

    /**
     * Unregisters all the internal resource bundle families in the registry.
     */
    void unregisterAllBundleFamilies();

}
