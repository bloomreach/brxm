/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.binder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.onehippo.cms7.services.search.annotation.Content;

public class SimpleContentBeanTypeMapper implements ContentBeanTypeMapper {

    private Map<String, Class<?>> contentBeanTypeMapByPrimaryType = Collections.synchronizedMap(new HashMap<String, Class<?>>());
    private Map<Class<?>, String> primaryTypeNameMapByContentBeanType = Collections.synchronizedMap(new HashMap<Class<?>, String>());

    @Override
    public Class<?> getContentBeanType(String primaryTypeName) {
        return contentBeanTypeMapByPrimaryType.get(primaryTypeName);
    }

    @Override
    public String getPrimaryTypeName(Class<?> contentBeanType) {
        return primaryTypeNameMapByContentBeanType.get(contentBeanType);
    }

    public void addContentBeanType(Class<?> contentBeanType) {
        Content content = contentBeanType.getAnnotation(Content.class);

        if (content == null) {
            throw new IllegalArgumentException("The contentBeanType must have implemented Content annotation.");
        }

        String primaryTypeName = content.primaryTypeName();
        contentBeanTypeMapByPrimaryType.put(primaryTypeName, contentBeanType);
        primaryTypeNameMapByContentBeanType.put(contentBeanType, primaryTypeName);
    }

    public void removeContentBeanType(Class<?> contentBeanType) {
        Content content = contentBeanType.getAnnotation(Content.class);

        if (content == null) {
            throw new IllegalArgumentException("The contentBeanType must have implemented Content annotation.");
        }

        String primaryTypeName = content.primaryTypeName();
        contentBeanTypeMapByPrimaryType.remove(primaryTypeName);
        primaryTypeNameMapByContentBeanType.remove(contentBeanType);
    }

}
