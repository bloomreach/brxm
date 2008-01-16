/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.plugins.search;

import org.hippoecm.cmsprototype.frontend.model.content.Document;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.JcrNodeModel;

public class SearchDocument extends Document{

    private static final long serialVersionUID = 1L;
    
    private String excerpt = "";
    private String similar = "";
    private String similarLink = "";
    
    public String getSimilarLink() {
        return similarLink;
    }

    public void setSimilarLink(String similarLink) {
        this.similarLink = similarLink;
    }


    public SearchDocument(JcrNodeModel nodeModel) throws ModelWrapException {
        super(nodeModel);
    }

    
    public String getSimilar() {
        return similar;
    }

    public void setSimilar(String similar) {
        this.similar = similar;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }
    
    public String getExcerpt() {
        return excerpt ; 
    }
    
}
