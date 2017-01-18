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

/**
 * ResourceBundleFamilyFactory
 * <P>
 * ResourceBundleFamilyFactory is responsible for creating a
 * ResourceBundleFamily based on the basename.
 * </P>
 * <P>
 * For example, implementations may load resource bundles to construct
 * a resource bundle family from JCR Repository, database, web resources, etc.
 * </P>
 * <P>
 * @deprecated the resource bundle family and its factory are internal to HST
 * and should be neither implemented nor used by projects.
 * </P>
 */
@Deprecated
public interface ResourceBundleFamilyFactory {

    /**
     * Creates and returns a resource bundle family based on the specified basename.
     * @param basename
     */
    ResourceBundleFamily createBundleFamily(String basename);

    /**
     * Creates and returns a resource bundle family based on the specified basename, with live or preview scope.
     *
     * @param basename basename of the resource bundle
     * @param preview  if true, creates the bundle family for the preview user. If false, does the same for live user.
     * @return
     */
    ResourceBundleFamily createBundleFamily(String basename, boolean preview);
}
