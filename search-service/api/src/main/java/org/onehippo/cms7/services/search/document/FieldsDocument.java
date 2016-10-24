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
package org.onehippo.cms7.services.search.document;

import java.util.Collection;

import org.onehippo.cms7.services.search.content.ContentId;

/**
 * Representation of a document from the search engine.
 */
public interface FieldsDocument {

    /**
     * Return type name
     * @return
     */
    String getPrimaryTypeName();

    /**
     * Returns content ID
     * @return
     */
    ContentId getContentId();

    /**
     * Checks whether field with the name exists
     * @param name
     * @return
     */
    boolean hasField(String name);

    /**
     * @return a list of fields defined in this document
     */
    Collection<String> getFieldNames();

    /**
     * Get the value or collection of values for a given field.  
     */
    Object getFieldValue(String name);

    /**
     * Get a collection of values for a given field name
     */
    Collection<Object> getFieldValues(String name);

    /**
     * returns the first value for a field
     */
    Object getFirstFieldValue(String name);

}
