/*
 *  Copyright 2012 Hippo.
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

import org.hippoecm.hst.content.beans.index.IndexField;

/**
 * The base marker interface for all beans: This includes beans that can be completely
 * independent of jcr, for example a bean that represents some external src
 */
public interface ContentBean {

    /**
     * This returns the absolute path of the backing provider for this bean, for example /documents/content/myprojec/news/article
     *
     * When the provider is a jcr node and is virtual, it returns the virtual path.
     *
     * @return the absolute jcr path of the backing jcr node. 
     */

    // the path is used as index id, not the canonical id as we can index
    // one node in multiple locations
    @IndexField(name="id")
    String getPath();

    
    void setPath(String path);
}
