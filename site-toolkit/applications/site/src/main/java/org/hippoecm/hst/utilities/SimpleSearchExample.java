package org.hippoecm.hst.utilities;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.beans.NewsPage;
import org.hippoecm.hst.beans.TextPage;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.query.filter.PrimaryNodeTypeFilterImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;

public class SimpleSearchExample {

    public SimpleSearchResultExample doSearch(HstRequest request, HstResponse response, BaseHstComponent base) {
        
        SimpleSearchResultExample result = new SimpleSearchResultExample();
        
        String query = base.getPublicRequestParameter(request, "query");
        if(query != null && !"".equals(query)) {
            // do the search
            try {
                HstQuery hstQuery = base.getQueryManager().createQuery(request.getRequestContext(), base.getSiteContentBaseBean(request), NewsPage.class);
               
                hstQuery.addOrderByDescending("testproject:title");
                hstQuery.addOrderByAscending("testproject:date");
                
                Filter filtera = new FilterImpl();
                filtera.addContains(".",  query);
                
                // example of chaining filters
                /*
                Filter filterb = new FilterImpl();
                filterb.addContains(".",  "Day5Article ");
               
                Filter filter2 = new FilterImpl();
                
                filter2.addAndFilter(filtera.addOrFilter(filterb));
                */
                
                hstQuery.setFilter(filtera);
                
                // example how you could add a filter based on primary type of a node
                //    BaseFilter nt = new PrimaryNodeTypeFilterImpl("testproject:newspage");
                //    filtera.addAndFilter(nt);
                
                
                HstQueryResult queryResult = hstQuery.execute();
                
                HippoBeanIterator beans = queryResult.getHippoBeans();
                List<HippoBean> hits = new ArrayList<HippoBean>();
                while(beans.hasNext()) {
                    HippoBean bean = beans.nextHippoBean();
                    if(bean != null) {
                        hits.add(bean);
                    } else {
                       // disregard empty bean
                    }
                    
                }
                
                result.setQuery(query);
                result.setSize(queryResult.getSize());
                result.setHits(hits);
            } catch (QueryException e) {
               throw new HstComponentException("Execption happened for query : " + e.getMessage() , e);
            }
        }
        
        return result;
    }
}
