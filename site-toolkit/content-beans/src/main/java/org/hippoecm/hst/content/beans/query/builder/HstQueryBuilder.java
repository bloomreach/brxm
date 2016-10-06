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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.exceptions.RuntimeQueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.DateTools;

public abstract class HstQueryBuilder {

    /*
     * Static methods to create a query builder or filter builder.
     */

    public static HstQueryBuilder create(final HippoBean ... scopeBeans) {
        DefaultHstQueryBuilder defaultHstQueryBuilder = new DefaultHstQueryBuilder();
        defaultHstQueryBuilder.scopes(scopeBeans);
        return defaultHstQueryBuilder;
    }

    public static HstQueryBuilder create(final Node ... scopeNodes) {
        DefaultHstQueryBuilder defaultHstQueryBuilder = new DefaultHstQueryBuilder();
        defaultHstQueryBuilder.scopes(scopeNodes);
        return defaultHstQueryBuilder;
    }

    /*
     * Members of a builder instance.
     */

    private DateTools.Resolution defaultResolution;
    private List<Node> scopes = new ArrayList<>();
    private List<Node> excludeScopes = new ArrayList<>();

    private List<String> ofTypes;
    private List<Class<? extends HippoBean>> ofTypeClazzes;
    private List<String> primaryNodeTypes;
    private List<Class<? extends HippoBean>> primaryNodeTypeClazzes;

    private ConstraintBuilder constraintBuilder;
    private List<OrderByConstruct> orderByConstructs;
    private Integer offset;
    private Integer limit;

    protected HstQueryBuilder() {
    }

    /**
     * @return The {@code HstQuery} for this {@code HstQueryBuilder} where the backing {@code HstQueryManager} is taken
     *         from {@link HstRequestContext#getQueryManager()}
     * @throws RuntimeQueryException in case the #build results in a {@link QueryException} or {@link FilterException}
     */
    public HstQuery build() throws RuntimeQueryException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        return build(requestContext.getQueryManager());
    }

    /**
     * @return The {@code HstQuery} for this {@code HstQueryBuilder} where the backing {@code HstQueryManager} is provided
     *         by the argument {@code queryManager}
     * @throws RuntimeQueryException in case the #build results in a {@link QueryException} or {@link FilterException}
     */
    abstract public HstQuery build(final HstQueryManager queryManager) throws RuntimeQueryException;

    /**
     * @param ofTypeClazzes the result most return documents only of {@code types} where subtypes are included.
     *                      If {@code ofTypeClazzes} is {@code null} the parameter is ignored
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder ofTypes(final Class<? extends HippoBean> ... ofTypeClazzes) {
        if (ofTypeClazzes != null) {
            if (this.ofTypeClazzes == null) {
                this.ofTypeClazzes = new ArrayList<>();
            }
            for (Class<? extends HippoBean> ofTypeClazz : ofTypeClazzes) {
                this.ofTypeClazzes.add(ofTypeClazz);
            }
        }
        return this;
    }

    /**
     * @param ofTypeClazzes the result most return documents only of {@code types} where subtypes are included.
     *                      If {@code ofTypeClazzes} is {@code null} the parameter is ignored
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder ofTypes(final String ... ofTypeClazzes) {
        if (ofTypeClazzes != null) {
            if (this.ofTypes == null) {
                this.ofTypes = new ArrayList<>();
            }

            for (String ofTypeClazz : ofTypeClazzes) {
                this.ofTypes.add(ofTypeClazz);
            }
        }
        return this;
    }

    /**
     * @param primaryNodeTypes the result most return documents only of {@code primaryNodeTypeNames} where subtypes
     *                             are <strong>not</strong> included. If {@code primaryNodeTypes} is {@code null}
     *                             the parameter is ignored
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder ofPrimaryTypes(String ... primaryNodeTypes) {
        if (primaryNodeTypes != null) {
            if (this.primaryNodeTypes == null) {
                this.primaryNodeTypes = new ArrayList<>();
            }

            for (String primaryNodeType : primaryNodeTypes) {
                this.primaryNodeTypes.add(primaryNodeType);
            }
        }

        return this;
    }

    /**
     * @param primaryNodeTypeClazzes the result most return documents only of {@code primaryNodeTypeNames} where subtypes
     *                             are <strong>not</strong> included. If {@code primaryNodeTypes} is {@code null}
     *                             the parameter is ignored
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder ofPrimaryTypes(final Class<? extends HippoBean> ... primaryNodeTypeClazzes) {
        if (primaryNodeTypeClazzes != null) {
            if (this.primaryNodeTypeClazzes == null) {
                this.primaryNodeTypeClazzes = new ArrayList<>();
            }
            for (Class<? extends HippoBean> primaryNodeTypeClazz : primaryNodeTypeClazzes) {
                this.primaryNodeTypeClazzes.add(primaryNodeTypeClazz);
            }
        }
        return this;
    }


    List<String> ofTypes() {
        return ofTypes;
    }

    List<Class<? extends HippoBean>> ofTypeClazzes() {
        return ofTypeClazzes;
    }

    List<String> primaryNodeTypes() {
        return primaryNodeTypes;
    }
    List<Class<? extends HippoBean>>  primaryNodeTypeClazzes() {
        return  primaryNodeTypeClazzes;
    }

    HstQueryBuilder scopes(final Node ... scopeNodes) {
        if (scopeNodes != null) {
            for (Node scopeNode : scopeNodes) {
                scopes.add(scopeNode);
                // in case present in 'scopes', remove it from there because now added as exclusion
                excludeScopes.remove(scopeNode);
            }
        }

        return this;
    }

    HstQueryBuilder scopes(final HippoBean ... scopeBeans) {
        if (scopeBeans != null) {
            for (HippoBean scopeBean : scopeBeans) {
                scopes.add(scopeBean.getNode());
                // in case present in 'scopes', remove it from there because now added as exclusion
                excludeScopes.remove(scopeBean.getNode());
            }
        }

        return this;
    }

    List<Node> scopes() {
        return scopes;
    }

    public HstQueryBuilder excludeScopes(final Node ... excludeScopeNodes) {
        if (excludeScopeNodes != null) {
            for (Node excludeScopeNode : excludeScopeNodes) {
                excludeScopes.add(excludeScopeNode);
                // in case present in 'scopes', remove it from there because now added as exclusion
                scopes.remove(excludeScopeNode);
            }
        }

        return this;
    }

    public HstQueryBuilder excludeScopes(final HippoBean ... excludeScopeBeans) {
        if (excludeScopeBeans != null) {
            for (HippoBean excludeScopeBean : excludeScopeBeans) {
                excludeScopes.add(excludeScopeBean.getNode());
                // in case present in 'scopes', remove it from there because now added as exclusion
                scopes.remove(excludeScopeBean.getNode());
            }
        }

        return this;
    }

    protected List<Node> excludeScopes() {
        return excludeScopes;
    }

    public HstQueryBuilder where(final ConstraintBuilder constraintBuilder) {
        if (this.constraintBuilder != null){
            throw new RuntimeQueryException(new QueryException("'where' clause is allowed only once."));
        }
        this.constraintBuilder = constraintBuilder;
        return this;
    }

    protected ConstraintBuilder where() {
        return constraintBuilder;
    }

    /**
     * @param sortOrder  the order in which the results should be sorted. If it is {@code null} <strong>or</strong>
     *                   not exactly equal to 'descending' (case insensitive), the {@code sortOrder} will be <strong>
     *                   ascending</strong>. Only when {@code sortOrder.equalsIgnoreCase("descending")} returns {@code true},
     *                   the sort order will be descending.
     * @param fieldNames the {@code fieldNames} to order on in {@code sortOrder} sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortBy(final String sortOrder, final String ... fieldNames) {
        if (sortOrder == null) {
            return sortByAscending(fieldNames);
        }
        if (sortOrder.equalsIgnoreCase("descending")) {
            return sortByDescending(fieldNames);
        }
        return sortByAscending(fieldNames);
    }

    /**
     * @param sortOrder  the order in which the results should be sorted. If it is {@code null} <strong>or</strong>
     *                   not exactly equal to 'descending' (case insensitive), the {@code sortOrder} will be <strong>
     *                   ascending</strong>. Only when {@code sortOrder.equalsIgnoreCase("descending")} returns {@code true},
     *                   the sort order will be descending.
     *
     * @param fieldNames the {@code fieldNames} to order on  case insensitive in {@code sortOrder} sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortByCaseInsensitive(final String sortOrder, final String ... fieldNames) {
        if (sortOrder == null) {
            return sortByAscendingCaseInsensitive(fieldNames);
        }
        if (sortOrder.equalsIgnoreCase("descending")) {
            return sortByDescendingCaseInsensitive(fieldNames);
        }
        return sortByAscendingCaseInsensitive(fieldNames);
    }

    /**
     * @param fieldNames the {@code fieldNames} to order on in ascending sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortByAscending(final String ... fieldNames) {
        if (fieldNames != null && fieldNames.length != 0) {
            for (String fieldName : fieldNames) {
                if (fieldName != null && !fieldName.isEmpty()) {
                    OrderByConstruct orderBy = new OrderByConstruct(fieldName, true);
                    addOrderByConstruct(orderBy);
                }
            }
        }
        return this;
    }

    /**
     * @param fieldNames the {@code fieldNames} to order on case insensitive in sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortByAscendingCaseInsensitive(final String ... fieldNames) {
        if (fieldNames != null && fieldNames.length != 0) {
            for (String fieldName : fieldNames) {
                if (fieldName != null && !fieldName.isEmpty()) {
                    OrderByConstruct orderBy = new OrderByConstruct(fieldName, true).caseSensitive(false);
                    addOrderByConstruct(orderBy);
                }
            }
        }
        return this;
    }


    /**
     * @param fieldNames the {@code fieldNames} to order on in descending sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortByDescending(final String ... fieldNames) {
        if (fieldNames != null && fieldNames.length != 0) {
            for (String fieldName : fieldNames) {
                if (fieldName != null && !fieldName.isEmpty()) {
                    OrderByConstruct orderBy = new OrderByConstruct(fieldName, false);
                    addOrderByConstruct(orderBy);
                }
            }
        }
        return this;
    }

    /**
     * @param fieldNames the {@code fieldNames} to order on case insensitive in descending sort order. The {@code fieldNames} argument
     *                   can be {@code null} in which case it is just ignored. A field name in the array that is empty is
     *                   skipped.
     * @return this {@link HstQueryBuilder} instance
     */
    public HstQueryBuilder sortByDescendingCaseInsensitive(final String ... fieldNames) {
        if (fieldNames != null && fieldNames.length != 0) {
            for (String fieldName : fieldNames) {
                if (fieldName != null && !fieldName.isEmpty()) {
                    OrderByConstruct orderBy = new OrderByConstruct(fieldName, false).caseSensitive(false);
                    addOrderByConstruct(orderBy);
                }
            }
        }
        return this;
    }

    List<OrderByConstruct> orderByConstructs() {
        return orderByConstructs;
    }

    /**
     * Sets the offset to start searching from. Default offset is <code>-1</code> which means it is ignored. A negative offset will be ignored
     * @param offset the {@code offset} to use, negative values will be ignored
     */
    public HstQueryBuilder offset(final int offset) {
        if (this.offset != null) {
            throw new RuntimeQueryException(new QueryException("'offset' is allowed to be set only once."));
        }
        this.offset = offset;
        return this;
    }

    Integer offset() {
        return offset;
    }


    /**
    * Sets the limit of search results.
    * <b>Note</b> that setting this value very high might influence performance negatively
    * @param limit the {@code limit} to use, negative values will be ignored
    */
    public HstQueryBuilder limit(final int limit) {
        if (this.limit != null) {
            throw new RuntimeQueryException(new QueryException("'limit' is allowed to be set only once."));
        }
        this.limit = limit;
        return this;
    }

    Integer limit() {
        return limit;
    }

    private void addOrderByConstruct(final OrderByConstruct orderBy) {
        if (orderByConstructs == null) {
            orderByConstructs = new ArrayList<>();
        }

        orderByConstructs.add(orderBy);
    }
}
