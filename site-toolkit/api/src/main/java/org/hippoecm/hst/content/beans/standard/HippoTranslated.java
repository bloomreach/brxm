/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.standard;

import java.util.Locale;

/**
 * Interface for beans of hippotranslation:translated.
 * @version $Id$
 */
public interface HippoTranslated {
    
    /**
     * Return string expression of the locale for content bean. If there is no locale found, <code>null</code> is returned
     * @return the string expression of the locale  for content bean and <code>null</code> when missing
     */
    String getLocaleString();
    
    /**
     * Returns the {@link Locale} for content bean. If there is no {@link Locale} found, <code>null</code> is returned
     * @return the {@link Locale} for content bean and <code>null</code> when missing
     */
    Locale getLocale();
    
}
