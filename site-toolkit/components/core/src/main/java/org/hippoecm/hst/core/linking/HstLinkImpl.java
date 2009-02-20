/*
 *  Copyright 2008 Hippo.
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

import org.hippoecm.hst.configuration.HstSite;

public class HstLinkImpl implements HstLink{

    private String path;
    private HstSite hstSite;
    
    public HstLinkImpl(String path, HstSite hstSite){
        this.path = path;
        this.hstSite = hstSite;
    }
    
    public HstSite getHstSite() {
        return this.hstSite;
    }

    public String getPath() {
        return this.path;
    }

}
