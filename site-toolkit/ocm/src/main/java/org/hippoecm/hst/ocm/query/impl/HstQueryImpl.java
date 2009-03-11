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
package org.hippoecm.hst.ocm.query.impl;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;

public class HstQueryImpl implements Query, Cloneable {

    private Filter filter;  

    ClassDescriptor classDescriptor;

    private final static String ORDER_BY_STRING = "order by ";

    private String jcrExpression = "";

    /**
     * Constructor
     *
     * @param filter
     * @param mapper
     */
    public HstQueryImpl(Filter filter, Mapper mapper) {
        this.filter = filter;
        classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());
    }

    private HstQueryImpl() {

    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Query#setFilter(org.apache.jackrabbit.ocm.query.Filter)
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * @see org.apache.jackrabbit.ocm.query.Query#getFilter()
     */
    public Filter getFilter() {
        return this.filter;
    }

    public void addOrderByDescending(String fieldNameAttribute) {
        //Changes made to maintain the query state updated with every addition
        //@author Shrirang Edgaonkar
        addExpression("@" + this.getJcrFieldName(fieldNameAttribute) + " descending");
    }

    /**
     *
     * @see org.apache.jackrabbit.ocm.query.Query#addOrderByAscending(java.lang.String)
     */
    public void addOrderByAscending(String fieldNameAttribute) {
        addExpression("@" + this.getJcrFieldName(fieldNameAttribute) + " ascending");
    }

    public void addJCRExpression(String jcrExpression) {
        addExpression(jcrExpression);
    }

    private void addExpression(String jcrExpression) {
        //@author Shrirang Edgaonkar
        // First time comma is not required
        if (this.jcrExpression.equals("")) {
            this.jcrExpression += jcrExpression;
        } else
            this.jcrExpression += (" , " + jcrExpression);
    }

    public String getOrderByExpression() {
        if (jcrExpression.equals(""))
            return "";
        else {
            //@author Shrirang Edgaonkar
            //Ensure that the OrderBy string is added only once
            if (this.jcrExpression.contains(ORDER_BY_STRING))
                return this.jcrExpression;
            else
                return (ORDER_BY_STRING + this.jcrExpression);
        }
    }

    @Override
    public Object clone() {
        HstQueryImpl cloned = new HstQueryImpl();
        
        if (this.filter != null) {
            cloned.filter = (Filter) ((HstFilterImpl) this.filter).clone();
        }
        
        cloned.classDescriptor = this.classDescriptor;
        cloned.jcrExpression = this.jcrExpression;
        
        return cloned;
    }

    private String getJcrFieldName(String fieldAttribute) {

        return classDescriptor.getJcrName(fieldAttribute);

    }

}
