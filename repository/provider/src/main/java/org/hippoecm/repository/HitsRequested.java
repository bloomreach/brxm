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
package org.hippoecm.repository;

import java.util.ArrayList;
import java.util.List;

public class HitsRequested {

    /**
     * Wether results should be returned at all.
     */
    private boolean resultRequested;

    /**
     * How many results should be returned.  Defaults to 10, large values imply slow responses.
     */
    private int limit = 10;

    /**
     * The offset in the resultset to start from.
     */
    private int offset = 0;
    
    private boolean fixedDrillPath = true;
    
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

    public boolean isFixedDrillPath() {
        return fixedDrillPath;
    }

    public void setFixedDrillPath(boolean fixedDrillPath) {
        this.fixedDrillPath = fixedDrillPath;
    }
    
}

