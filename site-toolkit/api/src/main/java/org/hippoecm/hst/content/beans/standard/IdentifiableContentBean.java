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

import org.hippoecm.hst.content.beans.index.IgnoreForCompoundBean;
import org.hippoecm.hst.content.beans.index.IndexField;

/**
 * The base interface for all identifiable beans: This includes beans that can be completely
 * independent of jcr, for example a bean that represents some external src. The {@link #getPath()} must return
 * the unique identifier for this {@link IdentifiableContentBean} : This is typically the identifier used in indexes
 */
public interface IdentifiableContentBean extends ContentBean {

    /**
     * This returns the path of the backing provider for this bean, for example
     * /documents/content/myprojec/news/article or http://www.example.com/foo/bar
     * It is not allowed for any implementation to return <code>null</code>
     * @return the path for this {@link IdentifiableContentBean}
     */
    // the path is used as index id, not the canonical id as we can index
    // one node in multiple locations
    @IgnoreForCompoundBean
    @IndexField(name="id")
    String getPath();

    /**
     * @param path sets the path for this {@link IdentifiableContentBean}
     * @see #getPath()
     */
    void setPath(String path);
}
