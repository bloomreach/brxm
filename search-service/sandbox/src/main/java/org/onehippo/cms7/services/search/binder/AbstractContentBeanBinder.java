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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.content.ContentId;
import org.onehippo.cms7.services.search.document.SearchDocument;
import org.onehippo.cms7.services.search.util.ContentBeanPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractContentBeanBinder implements ContentBeanBinder {

    private static Logger log = LoggerFactory.getLogger(AbstractContentBeanBinder.class);

    private SimpleContentBeanTypeMapper contentBeanTypeMapper = new SimpleContentBeanTypeMapper();

    public AbstractContentBeanBinder(Collection<Class<?>> contentBeanTypes) {
        if (contentBeanTypes != null) {
            for (Class<?> contentBeanType : contentBeanTypes) {
                contentBeanTypeMapper.addContentBeanType(contentBeanType);
            }
        }
    }

    protected ContentBeanTypeMapper getContentBeanTypeMapper() {
        return contentBeanTypeMapper;
    }

    @Override
    public Object toContentBean(SearchDocument searchDocument) throws ContentBindingException {
        String primaryTypeName = searchDocument.getPrimaryTypeName();

        if (primaryTypeName == null || "".equals(primaryTypeName)) {
            throw new IllegalArgumentException("The searchDocument doesn't contain a valid primary type name.");
        }

        Class<?> contentBeanType = contentBeanTypeMapper.getContentBeanType(primaryTypeName);

        if (contentBeanType == null) {
            throw new ContentBindingException("Content bean type not found for the primary type: " + primaryTypeName);
        }

        try {
            Object contentBean = contentBeanType.newInstance();

            ContentId contentId = searchDocument.getContentId();

            if (contentId != null) {
                setContentIdPropertyValue(contentBean, contentId);
            }

            PropertyValueProvider pvp = new FieldsDocumentPropertyValueProvider(contentBean, searchDocument);
            return LazyContentBeanProxyFactory.getProxy(contentBean, pvp, contentBeanType.getInterfaces());
        } catch (InstantiationException e) {
            throw new ContentBindingException("Failed to instantiate content bean.", e);
        } catch (IllegalAccessException e) {
            throw new ContentBindingException("Illegal access to content bean type.", e);
        }
    }

    @Override
    public Set<String> getFieldNames(String primaryTypeName) throws ContentBindingException {
        Set<String> fieldNames = new HashSet<String>();
        Class<?> contentBeanType = contentBeanTypeMapper.getContentBeanType(primaryTypeName);

        if (contentBeanType != null) {
            for (Method readMethod : ContentBeanPropertyUtils.getFieldPropertyReadMethods(contentBeanType)) {
                Field field = readMethod.getAnnotation(Field.class);
                fieldNames.add(field.name());
            }
        }

        return fieldNames;
    }

    protected void setContentIdPropertyValue(Object contentBean, ContentId contentId) {
        PropertyDescriptor identifierPropDesc = ContentBeanPropertyUtils.getIdentifierPropertyDescriptor(contentBean);

        if (identifierPropDesc != null) {
            Method writeMethod = identifierPropDesc.getWriteMethod();

            if (writeMethod != null) {
                Class<?> paramType = writeMethod.getParameterTypes()[0];

                if (ContentId.class.isAssignableFrom(paramType)) {
                    try {
                        writeMethod.invoke(contentBean, writeMethod, contentId);
                    } catch (Exception e) {
                        log.warn("Failed to write identifier field property.", e);
                    }
                } else if (String.class == paramType) {
                    try {
                        writeMethod.invoke(contentBean, contentId.toIdentifier());
                    } catch (Exception e) {
                        log.warn("Failed to write identifier field property.", e);
                    }
                } else {
                    throw new IllegalArgumentException("The identifier field must be either ContentId or String. Otherwise, you should override #setContentIdPropertyValue() of your ContentBeanBinder.");
                }
            }
        }
    }
}
