/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.DateUtil;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.GoGreenProductBean;
import org.hippoecm.hst.demo.beans.NewsBean;
import org.hippoecm.hst.demo.beans.ProductBean;
import org.hippoecm.hst.demo.beans.TextBean;
import org.hippoecm.hst.demo.beans.WikiBean;
import org.hippoecm.hst.demo.components.solrutil.SolrSearchParams;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrClient;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;

public class SolrSearch extends AbstractSearchComponent {

    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";

    public static final int DEFAULT_PAGE_SIZE = 5;

    private static final Map<String, Class<?>> typeClassMapping = new HashMap<String, Class<?>>();
    static {
        typeClassMapping.put(TextBean.class.getSimpleName(), TextBean.class);
        typeClassMapping.put(NewsBean.class.getSimpleName(), NewsBean.class);
        typeClassMapping.put(ProductBean.class.getSimpleName(), ProductBean.class);
        typeClassMapping.put(WikiBean.class.getSimpleName(), WikiBean.class);
        typeClassMapping.put(HippoBean.class.getSimpleName(), HippoBean.class);
        typeClassMapping.put(GoGreenProductBean.class.getSimpleName(), GoGreenProductBean.class);
    }
    
    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        SolrSearchParams params = new SolrSearchParams(request);

        if (params.getQuery() == null && request.getRequestContext().getResolvedSiteMapItem().getParameter("facetpath") == null) {
            // no query
            return;
        }

        HippoSolrClient solrClient = HstServices.getComponentManager().getComponent(HippoSolrClient.class.getName(), SOLR_MODULE_NAME);

        String query = params.getQuery();
        if (query == null) {
            // faceted path is not null
            query = "*:*";
            request.setAttribute("query", "*");
        } else {
            request.setAttribute("query", params.getQuery());
        }

        int pageSize = DEFAULT_PAGE_SIZE;
        String pageParam = request.getParameter("page");
        if (pageParam == null) {
            pageParam = getPublicRequestParameter(request, "page");
        }
        int page = getIntValue(pageParam, 1);

        try {

            // ************************ CHECK LOCAL PARAMS TO SET *********************************** //
            // we check whether we need AND-ed or OR-ed results because this needs to be prefixed to the query
            // Because our schema.xml has <solrQueryParser defaultOperator="OR"/>
            // we only need to do something in case of AND-ed
            StringBuilder localParams = new StringBuilder();
            if (params.isOperatorAnded()) {
                localParams.append("q.op=AND");
            }

            // by default in 'text' (all fields) is searched, unless params.getSearchField() != 'all'
            if (params.getSearchField() != null && !"all".equals(params.getSearchField())) {
                // we only need to search in some field. This can be added by LocalParam 'df'
                if (localParams.length() > 0) {
                    localParams.append(" ");
                }
                localParams.append("df="+params.getSearchField());
            }
            // ************************ END LOCAL PARAMS TO SET *********************************** //

            // ************************ CHECK DATE RANGE INCLUDED *********************************** //
            if (params.getFromDate() != null || params.getToDate() != null) {
                // date range query included. Add this to the query
                String fromDate;
                if (params.getFromDate() == null) {
                    fromDate = "*";
                } else {
                    fromDate = DateUtil.getThreadLocalDateFormat().format(params.getFromDate());
                }
                String toDate;
                if (params.getToDate() == null) {
                    toDate = "*";
                } else {
                    toDate = DateUtil.getThreadLocalDateFormat().format(params.getToDate());
                }
                query = query + " AND date:["+fromDate+" TO " + toDate + "]";
                log.debug("Date range added to query : '{}'", query);
            }

            // ************************ END CHECK DATE RANGE INCLUDED *********************************** //
            
            if (localParams.length() > 0) {
                // prepend local params
                query = "{!"+localParams.toString()+"}" + query;
            }

            HippoQuery hippoQuery = solrClient.createQuery(query);

            // ************************ CHECK SCOPED SEARCHING *********************************** //
            if ("external".equals(params.getSearchIn())){
                hippoQuery.setScopes("http:", "https:");
            } else if ("current".equals(params.getSearchIn())) {
                // for this, we first need to index the virtual locations
                String scope = request.getRequestContext().getResolvedMount().getMount().getContentPath();
                hippoQuery.setScopes(scope);
            }
            else {
                // we do not need a scope
            }
            // ************************ END SCOPED SEARCHING *********************************** //

            // ********************* CHECK SEARCH FOR SPECIFIC TYPES ONLY *********************** //
            if (params.getTypes() != null && params.getTypes().length > 0) {
               Set<String> types = new HashSet<String>();
                types.addAll(Arrays.asList(params.getTypes()));
                if (types.contains("all")) {
                    // search in 'all' is checked. Don't need to include types
                } else {
                    // get all the classes to filter on
                    Set<Class<?>> filterClasses = new HashSet<Class<?>>();
                    for (String type : types) {
                        if (typeClassMapping.containsKey(type)) {
                            filterClasses.add(typeClassMapping.get(type));
                        }
                    }
                    
                    if ( !filterClasses.isEmpty()) {
                        // WE HAVE FILTER CLASSES to apply. Also check 'INCLUDE SUBTYPES' now
                        boolean subTypes = params.isIncludeSubtypes();
                        hippoQuery.setIncludedClasses(subTypes, filterClasses.toArray(new Class[filterClasses.size()]));
                    }
                }
            }
            // ********************* END CHECK SEARCH FOR SPECIFIC TYPES ONLY ******************* //


            // ********************* ACCOUNT FOR FACETED NAVIGATION PATH IF PRESENT *********** //
            String facetPath = request.getRequestContext().getResolvedSiteMapItem().getParameter("facetpath");
            if (facetPath != null) {
                // there is a facet path : Account for the constraints
                // the facet path looks like key/value/key/value

                String[] constraints = facetPath.split("/");
                if (constraints.length % 2 != 0) {
                    log.debug("Invalid constraints because not equal number of keys and values");
                } else {
                    int i = 0;
                    while (i < constraints.length) {
                        String facetField = constraints[i];
                        String facetValue = constraints[i+1];
                        hippoQuery.getSolrQuery().addFilterQuery(facetField + ":" + solrClient.getQueryParser().escape(facetValue));
                        i+=2;
                    }
                }
                setHighlighted(hippoQuery);
                // for faceting, we want to show highlighting

            }

            // ********************* END ACCOUNT FOR FACETED NAVIGATION PATH IF PRESENT ******* //


            // Set the limit and offset
            hippoQuery.setLimit(pageSize);
            int offset = (page - 1) * pageSize;
            hippoQuery.setOffset(offset);

            // include spellcheck
            if (params.isShowSpellCheck()) {
                hippoQuery.getSolrQuery().add("spellcheck", "true");
                // we only want to spellcheck and return the query input field, not localParams add such
                hippoQuery.getSolrQuery().add("spellcheck.q", params.getQuery());
                hippoQuery.getSolrQuery().add("spellcheck.extendedResults", "true");
                hippoQuery.getSolrQuery().add("spellcheck.collateExtendedResults", "true");

            }

            // include scoring
            if (params.isShowScore()) {
                hippoQuery.getSolrQuery().setIncludeScore(true);
            }

            // OPTIONAL PERFORMANCE : we are only interested to retrieve the TITLE and SUMMARY fields.
            // If you do not specify a setFields, ALL fields are retrieved
            // hippoQuery.getSolrQuery().setFields("title", "summary", "score")

            // if highlighting is enabled
            if (params.isShowHighlight()) {
                setHighlighted(hippoQuery);
            }

            String sort = params.getSort();
            // if sort is null, default sort is by score descending
            if (sort != null) {
                if ("random".equals(sort)) {
                    // we map the sort now to <dynamicField name="random_*" type="random" />
                    // to get a pseudo random search for every new request, we need to map to a 'random' dynamic sort field : Otherwise
                    // the same random sort keeps being returned as long as the index does not change. We assume 10 random fields is enough
                    sort = "random_" + new Random().nextInt(10);
                    hippoQuery.getSolrQuery().addSortField(sort, params.getSortOrder());
                } else {
                    hippoQuery.getSolrQuery().addSortField(sort, params.getSortOrder());
                }
            }

            HippoQueryResult result = hippoQuery.execute();

            // we bind the results to their providers to make sure we have the jcr nodes attached again
            result.setContentBeanBinders();

            request.setAttribute("result", result);

            int maxPages = 20;

            // add pages
            if(result.getSize() > pageSize) {
                List<Integer> pages = new ArrayList<Integer>();
                int numberOfPages = result.getSize() / pageSize ;
                if(result.getSize() % pageSize != 0) {
                    numberOfPages++;
                }

                if (numberOfPages > maxPages) {
                    int startAt = 0;
                    if (offset > (10 * pageSize)) {
                        startAt = offset / pageSize;
                        startAt = startAt - 10;
                    }
                    for(int i = startAt; i < numberOfPages; i++) {
                        pages.add(i + 1);
                        if (i == (startAt + maxPages)) {
                            break;
                        }
                    }

                } else {
                    for(int i = 0; i < numberOfPages; i++) {
                        pages.add(i + 1);
                    }
                }

                request.setAttribute("page", page);
                request.setAttribute("pages", pages);
            }

        } catch (SolrServerException e) {
            throw new HstComponentException(e);
        }
    }

    private void setHighlighted(final HippoQuery hippoQuery) {
        hippoQuery.getSolrQuery().setHighlight(true);
        hippoQuery.getSolrQuery().setHighlightFragsize(150);
        hippoQuery.getSolrQuery().setHighlightSimplePre("<b style=\"color:blue\">");
        hippoQuery.getSolrQuery().setHighlightSimplePost("</b>");
        // hippoQuery.getSolrQuery().addHighlightField("title");
        // hippoQuery.getSolrQuery().addHighlightField("summary");
        // hippoQuery.getSolrQuery().addHighlightField("htmlContent");
        // highlight in any fetched field
        hippoQuery.getSolrQuery().addHighlightField("*");
    }


}

