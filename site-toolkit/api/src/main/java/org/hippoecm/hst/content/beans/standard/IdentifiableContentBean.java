/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
 * The base interface for all identifiable beans: This includes beans that can be completely
 * independent of jcr, for example a bean that represents some external src. The {@link #getIdentifier()} must return
 * the unique identifier for this {@link IdentifiableContentBean} : This is typically the identifier used in indexes
 */
public interface IdentifiableContentBean extends ContentBean {

    /**
     * <p>
     *     This returns the identifier of the backing provider for this bean, for example some UUID or
     *     /documents/content/myprojec/news/article or http://www.example.com/foo/bar, or a RDBMS id, etc
     *     It is not allowed for any implementation to return <code>null</code>
     * </p>
     * <p>
     *     Since the return value for this method is used as the index document identifier, it must
     *     be unique for every bean that must be indexed
     * </p>
     * @return the identifier for this {@link IdentifiableContentBean}
     */
    // the identifier is used as index id, hence add name="id"
    @IndexField(name="id", ignoreInCompound = true)
    String getIdentifier();

    /**
     * @param identifier sets the identifier for this {@link IdentifiableContentBean}
     * @see #getIdentifier()
     */
    void setIdentifier(String identifier);
}
