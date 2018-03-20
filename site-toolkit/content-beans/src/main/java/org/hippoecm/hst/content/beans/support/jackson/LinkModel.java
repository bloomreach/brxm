/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.support.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class LinkModel {

    private final String href;

    /**
     * The type of the link, for example 'internal', 'external' or 'resource'.
     */
    private final String type;

    private final String rel;

    private final String title;

    public LinkModel(final String href) {
        this(href, null, null, null);
    }

    public LinkModel(final String href, final String type) {
        this(href, type, null, null);
    }

    public LinkModel(final String href, final String type, final String rel, final String title) {
        this.href = href;
        this.type = type;
        this.rel = rel;
        this.title = title;
    }

    @JsonInclude(Include.NON_NULL)
    public String getHref() {
        return href;
    }

    @JsonInclude(Include.NON_NULL)
    public String getType() {
        return type;
    }

    @JsonInclude(Include.NON_NULL)
    public String getRel() {
        return rel;
    }

    @JsonInclude(Include.NON_NULL)
    public String getTitle() {
        return title;
    }

}
