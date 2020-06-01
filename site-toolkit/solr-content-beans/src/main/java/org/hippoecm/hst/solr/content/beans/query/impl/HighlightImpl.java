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
package org.hippoecm.hst.solr.content.beans.query.impl;

import java.util.List;

import org.hippoecm.hst.solr.content.beans.query.Highlight;

public class HighlightImpl implements Highlight {

    private final List<String> excerpts;
    private final String field;
    public HighlightImpl(final String field, final List<String> excerpts) {
        this.field = field;
        this.excerpts = excerpts;

    }

    @Override
    public String getField() {
        return field;
    }

    @Override
    public List<String> getExcerpts() {
        return excerpts;
    }
}
