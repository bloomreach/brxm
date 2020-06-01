/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.query.builder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.exceptions.RuntimeQueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;

class DefaultHstQueryBuilder extends HstQueryBuilder {

    protected DefaultHstQueryBuilder() {
        super();
    }

    @Override
    public HstQuery build(final HstQueryManager queryManager) throws RuntimeQueryException {
        final HstQuery hstQuery;

        List<Node> scopes = scopes();

        if (scopes == null || scopes.size() == 0) {
            throw new RuntimeQueryException(new QueryException("Empty scopes."));
        }

        final String[] primaryNodeTypes = getPrimaryNodeTypes(queryManager);

        final String[] ofTypes = getOfTypes(queryManager);


        try {
            if (primaryNodeTypes.length > 0) {
                if (ofTypes.length > 0) {
                    throw new RuntimeQueryException(new QueryException("Unsupported to combine #ofTypes and #ofPrimaryTypes"));
                } else {
                    hstQuery = queryManager.createQuery((Node)null, false, primaryNodeTypes);
                }
            } else if (ofTypes.length > 0) {
                if (ofTypes.length == 1) {
                    hstQuery = queryManager.createQuery(null, ofTypes[0], true);
                } else {
                    hstQuery = queryManager.createQuery((Node)null, false, expand(ofTypes, queryManager.getSession()));
                }
            } else {
                hstQuery = queryManager.createQuery((Node)null);
            }

            hstQuery.addScopes(scopes.toArray(new Node[scopes.size()]));

            final Node[] excludeScopes = getExcludeScopes();

            if (excludeScopes != null && excludeScopes.length > 0) {
                hstQuery.excludeScopes(excludeScopes);
            }

            if (where() != null) {
                Filter filter = where().build(queryManager.getSession(), queryManager.getDefaultResolution());
                if (filter != null) {
                    hstQuery.setFilter(filter);
                }
            }

            if (orderByConstructs() != null) {
                for (OrderByConstruct orderBy : orderByConstructs()) {
                    if (orderBy.ascending()) {
                        if (orderBy.caseSensitive()) {
                            hstQuery.addOrderByAscending(orderBy.fieldName());
                        } else {
                            hstQuery.addOrderByAscendingCaseInsensitive(orderBy.fieldName());
                        }
                    } else {
                        if (orderBy.caseSensitive()) {
                            hstQuery.addOrderByDescending(orderBy.fieldName());
                        } else {
                            hstQuery.addOrderByDescendingCaseInsensitive(orderBy.fieldName());
                        }
                    }
                }
            }

            if (offset() != null) {
                hstQuery.setOffset(offset());
            }

            if (limit() != null) {
                hstQuery.setLimit(limit());
            }

            return hstQuery;
        } catch (QueryException e) {
            throw new RuntimeQueryException(e);
        }
    }

    private Node[] getExcludeScopes() {
        Node[] excludeScopes = null;

        List<Node> excludeScopesList = excludeScopes();

        if (excludeScopesList != null) {
            excludeScopes = excludeScopesList.toArray(new Node[excludeScopesList.size()]);
        }

        return excludeScopes;
    }

    private String[] getPrimaryNodeTypes(final HstQueryManager queryManager) {

        Set<String> primaryNodeTypeSet = combine(queryManager, primaryNodeTypes(), primaryNodeTypeClazzes());
        return primaryNodeTypeSet.toArray(new String[primaryNodeTypeSet.size()]);
    }


    private String[] getOfTypes(final HstQueryManager queryManager) {
        Set<String> types = combine(queryManager, ofTypes(), ofTypeClazzes());
        return types.toArray(new String[types.size()]);
    }

    private Set<String> combine(final HstQueryManager queryManager, final List<String> types,
                                final List<Class<? extends HippoBean>> typeClazzes) {
        Set<String> combinedSet = new LinkedHashSet<>();
        if (types != null) {
            for (String type : types) {
                if (type != null) {
                    combinedSet.add(type);
                }
            }
        }
        if (typeClazzes != null && !typeClazzes.isEmpty()) {
            ObjectConverter objectConverter = queryManager.getObjectConverter();
            for (Class<? extends HippoBean> primaryNodeTypeClazz : typeClazzes) {
                String primaryNodeType = objectConverter.getPrimaryNodeTypeNameFor(primaryNodeTypeClazz);
                if (primaryNodeType != null) {
                    combinedSet.add(primaryNodeType);
                }
            }
        }
        return combinedSet;
    }


    private String[] expand(final String[] ofTypes, final Session session) throws QueryException {
        try {
            Set<String> expanded = new LinkedHashSet<>();
            NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
            for (String ofType : ofTypes) {
                expanded.addAll(getSelfPlusSubTypes(ofType, ntMgr.getAllNodeTypes()));
            }
            return expanded.toArray(new String[expanded.size()]);
        } catch (RepositoryException e) {
            throw new QueryException("Exception while expanding node types", e);
        }
    }


    private Set<String> getSelfPlusSubTypes(final String nodeType, final NodeTypeIterator allTypes) throws RepositoryException {
        Set<String> subTypes = new LinkedHashSet<>();
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            if (nt.isNodeType(nodeType)) {
                subTypes.add(nt.getName());
            }
        }
        return subTypes;
    }

}
