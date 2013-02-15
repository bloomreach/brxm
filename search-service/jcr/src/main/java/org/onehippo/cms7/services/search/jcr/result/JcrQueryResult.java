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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.RowIterator;

import org.hippoecm.repository.api.HippoNodeIterator;
import org.onehippo.cms7.services.search.result.HitIterator;
import org.onehippo.cms7.services.search.result.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrQueryResult implements QueryResult {

    private static final Logger log = LoggerFactory.getLogger(JcrQueryResult.class);

    private final RowIterator rowIterator;
    private final NodeIterator nodeIterator;
    private final Set<String> columnNames;

    public JcrQueryResult(final javax.jcr.query.QueryResult jcrQueryResult) throws RepositoryException {
        rowIterator = jcrQueryResult.getRows();
        // we also need the nodeIterator as from HippoNodeIterator we can get an efficient total hit size
        // without requiring extra access manager authorization for the hits. Note that the call below
        // does imply no extra overhead (order of 1/100 of a millis)
        nodeIterator = jcrQueryResult.getNodes();

        Set<String> columnNamesAsSet = new HashSet<String>();
        columnNamesAsSet.addAll(Arrays.asList(jcrQueryResult.getColumnNames()));
        columnNamesAsSet.remove("jcr:score");
        columnNamesAsSet.remove("jcr:path");
        this.columnNames = Collections.unmodifiableSet(columnNamesAsSet);
    }


    @Override
    public HitIterator getHits() {
        return new JcrHitIterator(rowIterator, columnNames);
    }

    @Override
    public long getTotalHitCount() {
        if (nodeIterator instanceof HippoNodeIterator) {
            return ((HippoNodeIterator)nodeIterator).getTotalSize();
        } else {
            log.warn("Expected a HippoNodeIterator which can efficiently get the total number of hits. Instead, fallback " +
                    "to slower Jackrabbit NodeIterator.getSize");
           return nodeIterator.getSize();
        }
    }

}
