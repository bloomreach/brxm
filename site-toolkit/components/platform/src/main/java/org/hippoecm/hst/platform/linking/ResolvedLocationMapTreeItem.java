/*
 *  Copyright 2008-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import java.io.Serializable;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * 
 */
public interface ResolvedLocationMapTreeItem extends Serializable {
    
    String getPath();

    /**
     * @return {@link HstSiteMapItem} belonging to this {@link ResolvedLocationMapTreeItem}. Returns <code>null</code> if
     * no {@link HstSiteMapItem} is attached to this  {@link ResolvedLocationMapTreeItem}.
     */
    HstSiteMapItem getSiteMapItem();

    /**
     * @return <code>true</code> when the {@link ResolvedLocationMapTreeItem} is a representation
     * of a document
     */
    boolean representsDocument();

    /**
     * @return {@code true} if this {@link ResolvedLocationMapTreeItem} was the result of a document/folder being
     * matched to an {@link HstNodeTypes#INDEX} sitemap item
     */
    boolean representsIndex();
}
