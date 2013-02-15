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

import org.apache.commons.beanutils.PropertyUtils;
import org.onehippo.cms7.services.search.annotation.Field;
import org.onehippo.cms7.services.search.document.FieldsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FieldsDocumentPropertyValueProvider extends AbstractPropertyValueProvider {

    private static Logger log = LoggerFactory.getLogger(FieldsDocumentPropertyValueProvider.class);

    private Object contentBean;
    private FieldsDocument fieldsDocument;

    public FieldsDocumentPropertyValueProvider(Object contentBean, FieldsDocument fieldsDocument) {
        this.contentBean = contentBean;
        this.fieldsDocument = fieldsDocument;
    }

    @Override
    public boolean hasProperty(String propertyName) {
        PropertyDescriptor desc = null;

        try {
            desc = PropertyUtils.getPropertyDescriptor(contentBean, propertyName);
        } catch (Exception e) {
            log.warn("Failed to read property descriptor.", e);
        }

        if (desc == null) {
            return false;
        }

        Method readMethod = PropertyUtils.getReadMethod(desc);

        if (readMethod == null) {
            return false;
        }

        Field field = readMethod.getAnnotation(Field.class);

        if (field == null) {
            return false;
        }

        return fieldsDocument.hasField(field.name());
    }

    @Override
    public Object getValue(String propertyName) {
        PropertyDescriptor desc = null;

        try {
            desc = PropertyUtils.getPropertyDescriptor(contentBean, propertyName);
        } catch (Exception e) {
            log.warn("Failed to read property descriptor.", e);
        }

        if (desc == null) {
            return null;
        }

        Method readMethod = PropertyUtils.getReadMethod(desc);

        if (readMethod == null) {
            return null;
        }

        Field field = readMethod.getAnnotation(Field.class);

        if (field == null) {
            return null;
        }

        return fieldsDocument.getFieldValue(field.name());
    }
}
