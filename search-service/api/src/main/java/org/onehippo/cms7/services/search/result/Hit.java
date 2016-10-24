/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.result;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.onehippo.cms7.services.search.document.SearchDocument;

public interface Hit extends Serializable {

    /**
     * Returns search document object which comes from the underlying search engine.
     * @return
     */
    SearchDocument getSearchDocument();

    /**
     * @return the score for this hit and -1 if there is no score available
     */
    float getScore();

    /**
     * @return the {@link List} of {@link Highlight}s or an empty list if there are no highlights
     */
    Map<String, Highlight> getHighlights();

}
