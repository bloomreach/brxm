/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.ocm.query;

import org.apache.jackrabbit.ocm.query.Filter;

public class HippoStdFilter {

    private Filter filter;
    
    public HippoStdFilter(Filter filter) {
        this.filter = filter;
    }

    /*
     * Protected because we do not want clients to directly hook into the jackrabbit ocm filter
     */
    protected Filter getFilter(){
        return this.filter;
    }
    
    public Class getFilterClass() {
        return this.filter.getFilterClass();
    }
    
    public void addContains(String scope, String fullTextSearch){
        this.filter.addContains(scope, fullTextSearch);
    }
    
    public Filter addAndFilter(Filter filter) {
        return filter.addAndFilter(filter);
    }

    public Filter addBetween(String fieldAttributeName, Object value1, Object value2) {
        return filter.addBetween(fieldAttributeName, value1, value2);
    }

    public Filter addEqualTo(String fieldAttributeName, Object value) {
         return filter.addEqualTo(fieldAttributeName, value);
    }

    public Filter addGreaterOrEqualThan(String fieldAttributeName, Object value) {
        return filter.addGreaterOrEqualThan(fieldAttributeName, value);
    }

    public Filter addGreaterThan(String fieldAttributeName, Object value) {
        return filter.addGreaterThan(fieldAttributeName, value);
    }

    public Filter addIsNull(String fieldAttributeName) {
        return filter.addIsNull(fieldAttributeName);
    }

 
    public Filter addLessOrEqualThan(String fieldAttributeName, Object value) {
        return filter.addLessOrEqualThan(fieldAttributeName, value);
    }

    public Filter addLessThan(String fieldAttributeName, Object value) {
        return filter.addLessThan(fieldAttributeName, value);
    }

    public Filter addLike(String fieldAttributeName, Object value) {
        return filter.addLike(fieldAttributeName, value);
    }

    public Filter addNotEqualTo(String fieldAttributeName, Object value) {
        return filter.addNotEqualTo(fieldAttributeName, value);
    }

    public Filter addNotNull(String fieldAttributeName) {
        return this.filter.addNotNull(fieldAttributeName);
    }

    public Filter addOrFilter(Filter filter) {
        return this.filter.addOrFilter(filter);
    }

}
