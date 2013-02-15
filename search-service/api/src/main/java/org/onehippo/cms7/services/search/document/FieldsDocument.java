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
    public String getPrimaryTypeName();

    /**
     * Returns content ID
     * @return
     */
    public ContentId getContentId();

    /**
     * Checks whether field with the name exists
     * @param name
     * @return
     */
    public boolean hasField(String name);

    /**
     * @return a list of fields defined in this document
     */
    public Collection<String> getFieldNames();

    /**
     * Get the value or collection of values for a given field.  
     */
    public Object getFieldValue(String name);

    /**
     * Get a collection of values for a given field name
     */
    public Collection<Object> getFieldValues(String name);

    /**
     * returns the first value for a field
     */
    public Object getFirstFieldValue(String name);

}
