/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.query.lucene;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.PropertyTypeRegistry;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.SynonymProvider;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.lucene.analysis.Analyzer;

/**
 * Implements a query builder that takes an abstract query tree and creates
 * a lucene {@link org.apache.lucene.search.Query} tree that can be executed
 * on an index.
 * todo introduce a node type hierarchy for efficient translation of NodeTypeQueryNode
 */
public class ServicingLuceneQueryBuilder {

    public ServicingLuceneQueryBuilder(QueryRootNode root, SessionImpl session, ItemStateManager sharedItemMgr,
            HierarchyManager hmgr, NamespaceMappings nsMappings, Analyzer analyzer, PropertyTypeRegistry propReg,
            SynonymProvider synonymProvider) { // TODO
        // super(root, session, sharedItemMgr, hmgr, nsMappings, analyzer, propReg, synonymProvider);
    }
}
