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
package org.onehippo.cms7.services.search.jcr.result;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;

import org.onehippo.cms7.services.search.document.SearchDocument;
import org.onehippo.cms7.services.search.jcr.document.HippoJcrSearchDocument;
import org.onehippo.cms7.services.search.result.Highlight;
import org.onehippo.cms7.services.search.result.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrHit implements Hit {

    static final Logger log = LoggerFactory.getLogger(JcrHit.class);

    private final Row row;
    private final Set<String> columnNames;

    public JcrHit(final Row row, final Set<String> columnNames) {
        this.row = row;
        this.columnNames = columnNames;
    }

    @Override
    public SearchDocument getSearchDocument() {
        final Node node;
        try {
            node = row.getNode();
            if (node == null) {
                return SearchDocument.EMPTY;
            }
        } catch (RepositoryException e) {
            return SearchDocument.EMPTY;
        }
        final HippoJcrSearchDocument document = new HippoJcrSearchDocument(node);
        document.setFieldNames(columnNames);
        return document;
    }

    @Override
    public float getScore() {
        try {
            return (float) row.getScore();
        } catch (RepositoryException e) {
            log.warn("Unable to retrieve score");
            return 0;
        }
    }

    @Override
    public Map<String, Highlight> getHighlights() {
        return Collections.emptyMap();
    }
}
