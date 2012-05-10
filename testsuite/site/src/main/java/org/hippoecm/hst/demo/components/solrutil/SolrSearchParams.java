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
package org.hippoecm.hst.demo.components.solrutil;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.hippoecm.hst.core.component.HstRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrSearchParams {

    private static final Logger log = LoggerFactory.getLogger(SolrSearchParams.class);

    private final static String DATE_FORMAT = "MM/dd/yyyy";

    SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
    private HstRequest request;
    private boolean showSpellCheck;
    private boolean showSuggest;
    private boolean showHighlight;
    private boolean showScore;
    private String query;
    private String operator;
    private String searchIn;
    private String searchField;
    private String[] types;
    private boolean includeSubtypes;
    private Date fromDate;
    private Date toDate;
    private String sort;
    private String sortOrder;

    public SolrSearchParams(HstRequest request) {
        this.request =  request;
        if (getPublicRequestParameter(request, "suggest") == null) {
            // default true
            showSuggest = true;
        } else {
            showSuggest = (Boolean)ConvertUtils.convert(getPublicRequestParameter(request, "suggest"), Boolean.class);
        }
        if (getPublicRequestParameter(request, "spellcheck") == null) {
            // default true
            showSpellCheck = true;
        } else {
            showSpellCheck = (Boolean)ConvertUtils.convert(getPublicRequestParameter(request, "spellcheck"), Boolean.class);
        }
        if (getPublicRequestParameter(request, "highlight") == null) {
            // default true
            showHighlight = true;
        } else {
            showHighlight = (Boolean)ConvertUtils.convert(getPublicRequestParameter(request, "highlight"), Boolean.class);
        }
        if (getPublicRequestParameter(request, "score") == null) {
            // default true
            showScore = true;
        } else {
            showScore = (Boolean)ConvertUtils.convert(getPublicRequestParameter(request, "score"), Boolean.class);
        }

        query = StringUtils.strip(getPublicRequestParameter(request, "query"));
        if(StringUtils.isBlank(query)) {
            query = null;
        }
        if (getPublicRequestParameter(request, "operator") == null) {
            // default OR-ed
            operator = "or_ed";
        } else {
            operator = getPublicRequestParameter(request, "operator");
        }

        searchIn =  getPublicRequestParameter(request, "searchin");
        if (StringUtils.isBlank(searchIn)) {
            searchIn = "all";
        }

        searchField=  getPublicRequestParameter(request, "searchfield");
        if (StringUtils.isBlank(searchField)) {
            searchField = "all";
        }

        types =  request.getParameterMap("").get("type");
        if (types == null) {
            types = new String[]{"all"};
        }
        includeSubtypes = (Boolean)ConvertUtils.convert(getPublicRequestParameter(request, "includeSubtypes"), Boolean.class);

        String fromDateString = getPublicRequestParameter(request, "fromdate");
        if (StringUtils.isNotBlank(fromDateString)) {
            try {
                fromDate = formatter.parse(fromDateString);
            } catch (ParseException e) {
                log.warn("Skip invalid fromDate", fromDateString);
            }
        }

        String toDateString = getPublicRequestParameter(request, "todate");
        if (StringUtils.isNotBlank(toDateString)) {
            try {
                toDate = formatter.parse(toDateString);
            } catch (ParseException e) {
                log.warn("Skip invalid toDate", toDateString);
            }
        }

        sort =  getPublicRequestParameter(request, "sort");
        if (StringUtils.isBlank(sort)) {
            sort = "score";
        }
        sortOrder =  getPublicRequestParameter(request, "order");
        if (StringUtils.isBlank(sortOrder)) {
            sortOrder = "desc";
        }
    }

    public boolean isShowSpellCheck() {
        return showSpellCheck;
    }

    public boolean isShowSuggest() {
        return showSuggest;
    }

    public boolean isShowHighlight() {
        return showHighlight;
    }

    public boolean isShowScore() {
        return showScore;
    }

    public String getQuery() {
        return query;
    }

    public boolean isOperatorAnded() {
        return (operator == null) ? false : "and_ed".equals(operator);
    }

    public String getSearchIn() {
        return searchIn;
    }

    public String getSearchField() {
        return searchField;
    }

    public String[] getTypes() {
        return types;
    }

    public boolean isIncludeSubtypes() {
        return includeSubtypes;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public String getSort() {
        return sort;
    }
    public SolrQuery.ORDER getSortOrder() {
        if ("asc".equals(sortOrder)) {
            return SolrQuery.ORDER.asc;
        }
        return  SolrQuery.ORDER.desc;
    }

    public void setParamsOnRequestAttr() {
        request.setAttribute("spellcheck", showSpellCheck);
        request.setAttribute("suggest", showSuggest);
        request.setAttribute("highlight", showHighlight);
        request.setAttribute("score", showScore);
        request.setAttribute("query", query);
        if ("and_ed".equals(operator)) {
            request.setAttribute("operator", "and_ed");
        } else {
            request.setAttribute("operator", "or_ed");
        }
        request.setAttribute("searchin", searchIn);
        request.setAttribute("searchfield", searchField);

        Map<String, String> typesMap = new HashMap<String, String>();
        for (String type : types) {
            typesMap.put(type,type);
        }
        request.setAttribute("types", typesMap);
        request.setAttribute("includeSubtypes",includeSubtypes);
        if (fromDate != null) {
            request.setAttribute("fromdate",formatter.format(fromDate));
        }
        if (toDate != null) {
            request.setAttribute("todate", formatter.format(toDate));
        }
        request.setAttribute("sort", sort);
        request.setAttribute("order", sortOrder);
    }

    private String getPublicRequestParameter(HstRequest request, String parameterName) {
        String contextNamespaceReference = request.getRequestContext().getContextNamespace();
        if (contextNamespaceReference == null) {
            contextNamespaceReference = "";
        }
        Map<String, String []> namespaceLessParameters = request.getParameterMap(contextNamespaceReference);
        String [] paramValues = namespaceLessParameters.get(parameterName);

        if (paramValues != null && paramValues.length > 0) {
            return paramValues[0];
        }
        return null;
    }
}
