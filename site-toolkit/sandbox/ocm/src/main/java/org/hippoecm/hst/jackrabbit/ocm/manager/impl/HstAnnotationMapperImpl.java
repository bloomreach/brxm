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
package org.hippoecm.hst.jackrabbit.ocm.manager.impl;

import java.util.List;

import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.AnnotationMapperImpl;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;

public class HstAnnotationMapperImpl extends AnnotationMapperImpl {
    
    protected String[] fallBackJcrNodeTypes;
    
    public HstAnnotationMapperImpl(List<Class> annotatedClassNames) {
        super(annotatedClassNames);
    }
    
    public HstAnnotationMapperImpl(List<Class> annotatedClassNames, String... fallBackJcrNodeTypes) {
        super(annotatedClassNames);
        this.fallBackJcrNodeTypes = fallBackJcrNodeTypes;
    }

    @Override
    public ClassDescriptor getClassDescriptorByNodeType(String jcrNodeType) {
        ClassDescriptor descriptor = mappingDescriptor.getClassDescriptorByNodeType(jcrNodeType);

        if (descriptor == null) {
            if (this.fallBackJcrNodeTypes != null) {
                for (String fallBackJcrNodeType : this.fallBackJcrNodeTypes) {
                    descriptor = mappingDescriptor.getClassDescriptorByNodeType(fallBackJcrNodeType);
                    
                    if (descriptor != null) {
                        break;
                    }
                }
            }
            
            if (descriptor == null) {
                throw new IncorrectPersistentClassException("Node type: " + jcrNodeType + " has no descriptor.");
            }
        }

        return descriptor;
    }

}
