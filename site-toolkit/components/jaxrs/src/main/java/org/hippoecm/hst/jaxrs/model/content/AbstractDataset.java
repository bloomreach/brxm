/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.jaxrs.model.content;

import java.io.Serializable;

public abstract class AbstractDataset implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private long totalSize = -1;
    
    private long beginIndex = -1;
    
    public AbstractDataset() {
        
    }
    
    public long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }
    
    public long getBeginIndex() {
        return beginIndex;
    }
    
    public void setBeginIndex(long beginIndex) {
        this.beginIndex = beginIndex;
    }
}
