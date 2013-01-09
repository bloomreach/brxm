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
package org.hippoecm.hst.jackrabbit.ocm.query.impl;

import java.util.Map;

import javax.jcr.ValueFactory;

import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.query.Filter;
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

  

}
