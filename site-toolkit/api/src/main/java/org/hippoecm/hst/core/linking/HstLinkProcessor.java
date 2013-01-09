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
package org.hippoecm.hst.core.linking;

/**
 * After linkcreation by HstLinkCreator all HstLinkProcessor's postProcess are called, 
 * and before a path is matched in HstSiteMapMatcher all HstLinkProcessor's preProcess are called.
 *
 */
public interface HstLinkProcessor {

    /**
     * The postProcess of the HstLink
     * @param link
     * @return the postProcessed HstLink
     */
    HstLink postProcess(HstLink link);
    
    /**
     * The preProcess of the HstLink
     * @param link
     * @return the preProcessed HstLink
     */
    HstLink preProcess(HstLink link);
}
