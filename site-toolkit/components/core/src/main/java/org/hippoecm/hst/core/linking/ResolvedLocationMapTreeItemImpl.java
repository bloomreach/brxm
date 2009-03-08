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

public class ResolvedLocationMapTreeItemImpl implements ResolvedLocationMapTreeItem{

    private static final long serialVersionUID = 1L;
    
    private String path;
    private String hstSiteMapItemId;
    
    public ResolvedLocationMapTreeItemImpl(String path, String hstSiteMapItemId){
        this.path = path;
        this.hstSiteMapItemId = hstSiteMapItemId;
    }
    
    public String getHstSiteMapItemId() {
        return hstSiteMapItemId;
    }

    public String getPath() {
        return path;
    }

}
