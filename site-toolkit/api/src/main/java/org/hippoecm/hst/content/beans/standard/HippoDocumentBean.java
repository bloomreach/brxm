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

/**
 * This is a marker interface for all beans that represent a document. When developers implement their own bean which
 * does not extend the standard HippoDocument bean, they should implement this interface. This ensures that linkrewriting
 * works correctly for extensions like .html or .xml etc etc
 */
public interface HippoDocumentBean extends HippoBean, HippoTranslated {

    /**
     * Returns the jcr uuid of the backing canonical <b>handle</b>  jcr node or <code>null</code> when 
     * <p>
     * <ul>
     *  <li>there is no canonical node</li>
     *  <li>the jcr node is detached</li>
     *  <li>a repository exception happened</li>
     * </ul>
     * </p>
     * @see HippoBean#getCanonicalUUID()
     * @return the uuid of the canonical handle or <code>null</code>
     */
    String getCanonicalHandleUUID();
    
    /**
     * Returns the jcr path of the backing canonical <b>handle</b> jcr node or <code>null</code> when 
     * <p>
     * <ul>
     *  <li>there is no canonical node</li>
     *  <li>the jcr node is detached</li>
     *  <li>a repository exception happened</li>
     * </ul>
     * </p>
     * @see HippoBean#getCanonicalPath()
     * @return the jcr path of the canonical handle or <code>null</code>
     */
    String getCanonicalHandlePath();

    /**
     * @param beanMappingClass only return translations of type <code>beanMappingClass</code>
     * @return a {@link HippoAvailableTranslationsBean} where the translations must be of type <code>beanMappingClass</code>. This method never returns <code>null</code>
     */
    <T extends HippoBean> HippoAvailableTranslationsBean<T> getAvailableTranslations(Class<T> beanMappingClass);

    @Override
    default String getRepresentationId() {
        String canonicalHandleUUID = getCanonicalHandleUUID();
        if (canonicalHandleUUID != null) {
            return canonicalHandleUUID;
        }
        return getIdentifier();
    }
}
