/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.onehippo.cms7.essentials.dashboard.config.Document;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.AnnotationUtils;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentMultiProperty;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id$"
 */
public class JcrPersistenceReader {

    private static Logger log = LoggerFactory.getLogger(JcrPersistenceReader.class);
    private final PluginContext context;

    public JcrPersistenceReader(final PluginContext context) {
        this.context = context;
    }

    public <T extends Document> T read(final String path, final Class<T> clazz) {

        final PersistentHandler<PersistentNode, Node> nodeWriter = PersistentNode.ProcessAnnotation.NODE;
        final PersistentNode annotationNode = AnnotationUtils.getClassAnnotation(clazz, PersistentNode.class);
        final Node node = nodeWriter.read(context, null, path, annotationNode);
        if (node == null) {
            return null;
        }

        final T document = GlobalUtils.newInstance(clazz);
        if (document == null) {
            return null;
        }
        try {
            document.setParentPath(node.getParent().getPath());
            document.setName(node.getName());
        } catch (RepositoryException e) {
            log.error("Error reading path", e);
            return null;
        }
        // read properties:
        final PersistentHandler<PersistentProperty, Property> propreader = PersistentProperty.ProcessAnnotation.PROPERTY;
        final Collection<Field> fields = AnnotationUtils.getAnnotatedFields(clazz, PersistentProperty.class);
        for (Field field : fields) {
            final PersistentProperty property = field.getAnnotation(PersistentProperty.class);
            try {
                final Property myProperty = propreader.read(context, node, property.name(), property);
                if (myProperty == null) {
                    field.set(document, null);
                    continue;
                }
                final Class<?> type = field.getType();
                if (type == String.class) {
                    field.set(document, myProperty.getString());
                } else if (type == Boolean.class || type == boolean.class) {
                    field.set(document, myProperty.getBoolean());
                } else if (type == Long.class || type == long.class) {
                    field.set(document, myProperty.getLong());
                } else if (type == Calendar.class) {
                    field.set(document, myProperty.getDate());
                } else {
                    log.error("@@@ PROPERTY READING NOT IMPLEMENTED FOR TYPE: {}", type);
                }

            } catch (IllegalAccessException e) {
                log.error("Error setting field value:", e);
            } catch (RepositoryException e) {
                log.error("Error fetching property value", e);
            }

        }
        // multi properties
        final PersistentMultiProperty.ProcessAnnotation multiReader = PersistentMultiProperty.ProcessAnnotation.MULTI_PROPERTY;
        final Collection<Field> multiFields = AnnotationUtils.getAnnotatedFields(clazz, PersistentMultiProperty.class);
        for (Field field : multiFields) {
            final PersistentMultiProperty property = field.getAnnotation(PersistentMultiProperty.class);
            try {
                final Property myProperty = multiReader.read(context, node, property.name(), property);
                if (myProperty == null) {
                    field.set(document, null);
                    continue;
                }
                final ParameterizedType collectionType = (ParameterizedType) field.getGenericType();
                final Class<?> type = (Class<?>) collectionType.getActualTypeArguments()[0];
                final Value[] values = myProperty.getValues();
                if (type == String.class) {
                    field.set(document, covertValues(values, String.class));
                } else if (type == Date.class || type == Calendar.class) {
                    field.set(document, covertValues(values, Calendar.class));
                } else if (type == boolean.class || type == Boolean.class) {
                    field.set(document, myProperty.getBoolean());
                } else if (type == int.class || type == long.class || type == Long.class || type == Integer.class) {
                    field.set(document, myProperty.getLong());
                } else {
                    log.error("@@@ PROPERTY READING NOT IMPLEMENTED FOR TYPE: {}", type);
                }

            } catch (IllegalAccessException e) {
                log.error("Error setting field value:", e);
            } catch (RepositoryException e) {
                log.error("Error fetching property value", e);
            }

        }


        return document;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> covertValues(final Value[] values, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        try {
            for (Value value : values) {
                if (clazz == String.class) {
                    list.add((T) value.getString());

                } else if (clazz == boolean.class || clazz == Boolean.class) {
                    final Boolean aBoolean = value.getBoolean();
                    list.add((T) aBoolean);
                } else if (clazz == long.class || clazz == Long.class) {
                    final Long aLong = value.getLong();
                    list.add((T) aLong);
                } else {
                    log.error("Converting value not implemented: {}", clazz);
                }


            }
        } catch (RepositoryException e) {
            log.error("Error getting value", e);
        }
        return list;

    }


}
