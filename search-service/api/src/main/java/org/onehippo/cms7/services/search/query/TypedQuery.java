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
package org.onehippo.cms7.services.search.query;

import org.onehippo.cms7.services.search.document.FieldsDocument;
import org.onehippo.cms7.services.search.query.steps.WhereStep;

public interface TypedQuery extends Query, WhereStep {

    /**
     * Instructs the search service to collect for each matching document the given property. These properties can be
     * read from the result set using {@link FieldsDocument#getFieldValue(java.lang.String)}. See also the note at
     * {@link Query#returnParentNode()} when used in combination with that setting.
     *
     * @param property name of property to collect
     * @return this query object
     */
    SelectClause select(String property);
}
