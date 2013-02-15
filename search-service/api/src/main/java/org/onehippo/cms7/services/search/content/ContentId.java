/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.content;

import java.io.Serializable;

/**
 * ID representation which can provide a stringified ID.
 * Also, search service implementation may require some specific
 * ID representation objects implementing this interface for
 * any extensible features.
 */
public interface ContentId extends Serializable {

    ContentId UNKNOWN = new ContentId() {
        @Override
        public String toIdentifier() {
            return "<unknown-content-id>";
        }
    };

    /**
     * Returns a stringified ID string
     * @return
     */
    public String toIdentifier();

}
