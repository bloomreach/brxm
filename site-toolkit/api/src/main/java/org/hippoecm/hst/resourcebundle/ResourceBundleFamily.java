/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.resourcebundle;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * ResourceBundleFamily
 * <P>
 * ResourceBundleFamily contains resource bundles, each of which can be the
 * default resource bundle or localized resource bundle, and each of which
 * shares the same basename.
 * </P>
 */
public interface ResourceBundleFamily {

    /**
     * Returns the resource bundle base name.
     * @return
     */
    String getBasename();

    /**
     * Returns the default resource bundle for live mode.
     * @return
     */
    ResourceBundle getDefaultBundle();

    /**
     * Returns the default resource bundle for preview mode.
     * @return
     */
    ResourceBundle getDefaultBundleForPreview();

    /**
     * Returns a localized resource bundle for the specific locale for live mode.
     * @param locale
     * @return
     */
    ResourceBundle getLocalizedBundle(Locale locale);

    /**
     * Returns a localized resource bundle for the specific locale for preview mode.
     * @param locale
     * @return
     */
    ResourceBundle getLocalizedBundleForPreview(Locale locale);

}
