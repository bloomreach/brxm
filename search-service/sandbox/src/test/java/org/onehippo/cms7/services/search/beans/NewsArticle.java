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
package org.onehippo.cms7.services.search.beans;

import org.onehippo.cms7.services.search.annotation.Content;
import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.annotation.Identifier;
import org.onehippo.cms7.services.search.binder.ContentBeanBinder;

/**
 * Content bean example with document type name (or alias).
 * And, contains fields annotated on member fields or getter method.
 * {@link ContentBeanBinder} will, somehow, be able to map search engine document object
 * into this annotated beans or vice versa.
 * Note: we might need to enable proxying to enable lazy-loading on each getters.
 */
@Content(primaryTypeName="newsarticle")
public class NewsArticle {

    private String id;

    private String title;

    private String content;

    @Identifier
    @Field(name="id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Field(name="title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Field(name="content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
