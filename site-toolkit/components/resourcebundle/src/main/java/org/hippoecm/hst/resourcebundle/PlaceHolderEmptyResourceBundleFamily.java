/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
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
 * PlaceHolderEmptyResourceBundleFamily
 * <P>
 * This implementation is just for place holder in order to denote that
 * it already looked up the resource bundle family from the underlying source,
 * but it wasn't able to find the resource bundle family without any exceptions.
 * In that way, it doesn't have to try to look up every time for the non-existing
 * basename resource bundle family.
 * </P>
 */
public class PlaceHolderEmptyResourceBundleFamily implements ResourceBundleFamily {

    @Override
    public String getBasename() {
        return null;
    }

    @Override
    public ResourceBundle getDefaultBundle() {
        return null;
    }

    @Override
    public ResourceBundle getDefaultBundleForPreview() {
        return null;
    }

    @Override
    public ResourceBundle getLocalizedBundle(Locale locale) {
        return null;
    }

    @Override
    public ResourceBundle getLocalizedBundleForPreview(Locale locale) {
        return null;
    }

}
