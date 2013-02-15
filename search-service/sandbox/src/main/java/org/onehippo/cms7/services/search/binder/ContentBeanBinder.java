/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.search.binder;

import java.util.Set;

import org.onehippo.cms7.services.search.annotation.Content;
import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.document.SearchDocument;
import org.onehippo.cms7.services.search.service.SearchService;

/**
 * ContentBeanBinder which maps search document object from search engine
 * to content bean and vice versa.
 * {@link SearchService} implementation needs to have at least one {@link ContentBeanBinder}
 * in order to convert objects between search documents and content beans.
 */
public interface ContentBeanBinder {

    /**
     * Returns a mapped content bean object which can be understood by the caller
     * from search document object which was from the underlying search engine.
     * The returned content bean object must be resolved by annotated bean types by {@link Content} and {@link Field}.
     * @param searchDocument
     * @return
     */
    public Object toContentBean(SearchDocument searchDocument) throws ContentBindingException;

    /**
     * Returns field names of search document by specified primary type name
     * @param primaryTypeName
     * @return
     * @throws ContentBindingException
     */
    public Set<String> getFieldNames(String primaryTypeName) throws ContentBindingException;

}
