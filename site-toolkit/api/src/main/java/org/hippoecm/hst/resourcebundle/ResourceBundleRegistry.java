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
package org.hippoecm.hst.resourcebundle;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * ResourceBundleRegistry
 * <P>
 * ResourceBundleRegistry enables to look up a resource bundle.
 * </P>
 */
public interface ResourceBundleRegistry {

    /**
     * Returns the resource bundle based on the specific basename for the default locale.
     * @param basename
     * @return
     */
    ResourceBundle getBundle(String basename);

    /**
     * Returns the resource bundle based on the specific basename for the default locale.
     * @param basename
     * @return
     */
    ResourceBundle getBundleForPreview(String basename);

    /**
     * Returns the resource bundle based on the specific basename and locale.
     * @param basename
     * @param locale
     * @return
     */
    ResourceBundle getBundle(String basename, Locale locale);

    /**
     * Returns the resource bundle based on the specific basename and locale.
     * @param basename
     * @param locale
     * @return
     */
    ResourceBundle getBundleForPreview(String basename, Locale locale);

}
