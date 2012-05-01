/*
 *  Copyright 2012 Hippo.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.demo.beans.GoGreenProductBean;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.solr.HippoSolrManager;
import org.hippoecm.hst.solr.content.beans.query.HippoQuery;
import org.hippoecm.hst.solr.content.beans.query.HippoQueryResult;

public class SolrSearch extends AbstractSearchComponent {

    public static final String SOLR_MODULE_NAME = "org.hippoecm.hst.solr";

    public static final int DEFAULT_PAGE_SIZE = 5;


    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        HippoSolrManager solrManager = HstServices.getComponentManager().getComponent(HippoSolrManager.class.getName(), SOLR_MODULE_NAME);

        String query = getPublicRequestParameter(request, "query");
        if (query == null || "".equals(query)) {
            query = request.getParameter("query");
        }

        if (query == null) {
            return;
        }

        int pageSize = DEFAULT_PAGE_SIZE;
        String pageParam = request.getParameter("page");
        if (pageParam == null) {
            pageParam = getPublicRequestParameter(request, "page");
        }
        int page = getIntValue(pageParam, 1);

        try {

            // query can be just Solr syntax, for example :
            //
            // q = title:hippo AND date:[2008-01-08T04:38:24.512Z TO *]

            HippoQuery hippoQuery = solrManager.createQuery(query);
            hippoQuery.setLimit(pageSize);
            int offset = (page - 1) * pageSize;
            hippoQuery.setOffset(offset);

            // include scoring
            hippoQuery.getSolrQuery().setIncludeScore(true);

            hippoQuery.getSolrQuery().setHighlight(true);
            hippoQuery.getSolrQuery().setHighlightFragsize(200);
            hippoQuery.getSolrQuery().setHighlightSimplePre("<b style=\"color:blue\">");
            hippoQuery.getSolrQuery().setHighlightSimplePost("</b>");
            hippoQuery.getSolrQuery().addHighlightField("title");
            hippoQuery.getSolrQuery().addHighlightField("summary");
            hippoQuery.getSolrQuery().addHighlightField("htmlContent");

            // enable spelling : note that at least once 'spellcheck.build=true' should be added : for performance
            // reasons this should not be done every time!!
            // q=AcademyAwards&spellcheck=true&spellcheck.collate=true&spellcheck.build=true&qt=/spell

            //hippoQuery.getSolrQuery().setQueryType("/spell");

           // hippoQuery.getSolrQuery().addSortField("name", org.apache.solr.client.solrj.SolrQuery.ORDER.asc);

            Long start = System.currentTimeMillis();
            HippoQueryResult result = hippoQuery.execute(true);

            request.setAttribute("result", result);
            request.setAttribute("query", query);

           // System.out.println("TOOK " + (System.currentTimeMillis() - start));

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
            e.printStackTrace();
            throw new HstComponentException(e);
        }
    }


}

