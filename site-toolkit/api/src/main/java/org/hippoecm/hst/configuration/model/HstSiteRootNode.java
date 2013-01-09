/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.model;


public interface HstSiteRootNode extends HstNode{

    /**
     * @return the content path of this {@link HstSiteRootNode}
     */
    String getContentPath();
    
    /**
     * @return the canonical content path of this {@link HstSiteRootNode}. It can be the same as the {@link #getContentPath()} but
     * it doesn't have to be: the {@link #getContentPath()} can be a virtual entry path
     */
    
    String getCanonicalContentPath();

    /**
     * @return the version of the hst configuration this {@link HstSiteRootNode} should use. If no version is set, -1
     * is returned
     */
    long getVersion();

    
    String getConfigurationPath();
}