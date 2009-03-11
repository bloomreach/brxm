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

import java.util.Iterator;
import java.util.Map;

import javax.jcr.ValueFactory;

import org.apache.jackrabbit.ocm.manager.ManagerConstant;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.query.Filter;
import org.apache.jackrabbit.ocm.query.Query;
import org.apache.jackrabbit.ocm.query.impl.QueryManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstQueryManagerImpl extends QueryManagerImpl {

    private final static Logger log = LoggerFactory.getLogger(HstQueryManagerImpl.class);

    private Mapper mapper;
    private Map atomicTypeConverters;
    private ValueFactory valueFactory;

    public HstQueryManagerImpl(Mapper mapper, Map atomicTypeConverters, ValueFactory valueFactory) {
        super(mapper, atomicTypeConverters, valueFactory);

        this.mapper = mapper;
        this.atomicTypeConverters = atomicTypeConverters;
        this.valueFactory = valueFactory;
    }

    public Filter createFilter(Class classQuery) {
        return new HstFilterImpl(this.mapper.getClassDescriptorByClass(classQuery), this.atomicTypeConverters,
                classQuery, this.valueFactory);
    }

    public Query createQuery(Filter filter) {
        return new HstQueryImpl(filter, this.mapper);
    }

    public String buildJCRExpression(Query queryInput) {
        Query query = (Query) ((HstQueryImpl) queryInput).clone();
        
        // TODO: Ard, you can add filter(s) to the cloned query.. ;-)
        // 
        // 

        Filter filter = query.getFilter();

        // Check if the class has  an inheritance discriminator field
        ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());
        if (classDescriptor.hasDiscriminator()) {
            Filter discrininatorFilter = buildDiscriminatorFilter(query, classDescriptor);
            filter = filter.addAndFilter(discrininatorFilter);
        }

        String jcrExp = "";

        // Add scope & node name
        if (((filter.getScope() != null) && (!filter.getScope().equals("")))) {
            jcrExp += "/jcr:root" + filter.getScope() + "element(" + filter.getNodeName() + ", ";
        }
        else {
            jcrExp += "//element(" + filter.getNodeName() + ", ";
        }

        // Add node type
        jcrExp += this.getNodeType(filter) + ") ";

        // Add filter criteria
        String filterExp = ((HstFilterImpl) filter).getJcrExpression();

        // Build the jcr filter
        if ((filterExp != null) && (!filterExp.equals(""))) {
            jcrExp += "[" + filterExp + "]";
        }

        // Add order by
        jcrExp += ((HstQueryImpl) query).getOrderByExpression();
        log.debug(jcrExp);

        return jcrExp;

    }

    private Filter buildDiscriminatorFilter(Query query, ClassDescriptor classDescriptor) {
        Filter discriminatorFilter = this.createFilter(query.getFilter().getFilterClass());
        if (!classDescriptor.isAbstract() && (! classDescriptor.isInterface()) ) {
            discriminatorFilter.addJCRExpression("@" + ManagerConstant.DISCRIMINATOR_CLASS_NAME_PROPERTY + "='" +    classDescriptor.getClassName() + "'");
        }

        if (classDescriptor.hasDescendants()) {
            Iterator descendantDescriptorIterator = classDescriptor.getDescendantClassDescriptors().iterator();

            while (descendantDescriptorIterator.hasNext()) {
                ClassDescriptor descendantClassDescriptor = (ClassDescriptor) descendantDescriptorIterator.next();

                //Add subdescendant discriminator value
                discriminatorFilter = discriminatorFilter.addOrFilter(
                        this.buildDiscriminatorFilter(query, descendantClassDescriptor));
            }

        }

        return discriminatorFilter;
    }

    private String getNodeType(Filter filter) {
        ClassDescriptor classDescriptor = mapper.getClassDescriptorByClass(filter.getFilterClass());

        String jcrNodeType = classDescriptor.getJcrType();
        if (jcrNodeType == null || jcrNodeType.equals(""))
            {
           return ManagerConstant.NT_UNSTRUCTURED;
            }
        else
        {
           return jcrNodeType;
        }
    }

}
