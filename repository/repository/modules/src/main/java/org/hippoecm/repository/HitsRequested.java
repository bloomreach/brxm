package org.hippoecm.repository;

import java.util.ArrayList;
import java.util.List;

public class HitsRequested {
    /**
     * Wether results should be returned at all.
     */
    private boolean resultRequested;

    /**
     * when true, it is indicated through this boolean that only when the lucene hit has the property, it is returned in the count
     * Default is false
     */
    private boolean countOnlyForFacetExists = false;
    
    /**
     * How many results should be returned.  Defaults to 10, large values imply slow responses.
     */
    private int limit = 10;

    /**
     * The offset in the resultset to start from.
     */
    private int offset = 0;
    
    /**
     * The orderBy property when resultset is order, <code>null</code> if no ordering is needed
     */
    private List<OrderBy> orderByList = new ArrayList<OrderBy>();
    
    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public boolean isResultRequested() {
        return resultRequested;
    }

    public void setResultRequested(boolean resultRequested) {
        this.resultRequested = resultRequested;
    }

    public boolean isCountOnlyForFacetExists() {
        return countOnlyForFacetExists;
    }

    public void setCountOnlyForFacetExists(boolean countOnlyForFacetExists) {
        this.countOnlyForFacetExists = countOnlyForFacetExists;
    }

    public List<OrderBy> getOrderByList() {
        return this.orderByList;
    }

    public void addOrderBy(String name, boolean descending) {
        this.orderByList.add(new OrderBy(name, descending));
    }
    
    public void addOrderBy(List<OrderBy> addOrderByList) {
        if(addOrderByList != null) {
            this.orderByList.addAll(addOrderByList);
        }
    }
    
}

