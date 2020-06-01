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
package org.hippoecm.hst.demo.wicketexamples;

import java.io.Serializable;

public class ItemBean implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String name;
    protected String path;
    protected int depth;
    protected boolean ismodified;
    protected boolean isnew;
    protected boolean isnode;
    
    public ItemBean() {
        
    }
    
    public ItemBean(String name, String path, int depth, boolean ismodified, boolean isnew, boolean isnode) {
        this.name = name;
        this.path = path;
        this.depth = depth;
        this.ismodified = ismodified;
        this.isnew = isnew;
        this.isnode = isnode;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public int getDepth() {
        return depth;
    }
    
    public boolean isModified() {
        return ismodified;
    }
    
    public boolean isNew() {
        return isnew;
    }
    
    public boolean isNode() {
        return isnode;
    }
    
}
