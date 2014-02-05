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

package org.onehippo.cms7.essentials.dashboard.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.NotImplementedException;
import org.apache.jackrabbit.value.BooleanValue;
import org.apache.jackrabbit.value.DateValue;
import org.apache.jackrabbit.value.DoubleValue;
import org.apache.jackrabbit.value.LongValue;
import org.onehippo.cms7.essentials.dashboard.config.Document;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.PersistentHandler;
import org.onehippo.cms7.essentials.dashboard.model.hst.SimplePropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Processes JCR multi property item
 *
 * @version "$Id$"
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface PersistentMultiProperty {

    String name();

    Class<?> type() default String.class;

    enum ProcessAnnotation implements PersistentHandler<PersistentMultiProperty, Property> {
        MULTI_PROPERTY {
            @Override
            public Property read(final PluginContext context, final Node parent, final String path, final PersistentMultiProperty annotation) {
                try {
                    if (parent == null) {
                        log.error("Parent node was null for path: {}", path);
                        return null;
                    }
                    if (Strings.isNullOrEmpty(path)) {
                        log.error("Path was null for parent: {}", parent.getPath());
                        return null;
                    }
                    if (!parent.hasProperty(path)) {
                        return null;
                    }
                    return parent.getProperty(path);
                } catch (RepositoryException | IllegalArgumentException e) {
                    log.error("Error loading property", e);
                }
                return null;
            }

        };
        private static final Logger log = LoggerFactory.getLogger(ProcessAnnotation.class);

        @Override
        public Property execute(final PluginContext context, final Document model, final PersistentMultiProperty annotation) {

            final SimplePropertyModel ourModel = (SimplePropertyModel) model;
            final Object value = ourModel.getValue();
            final String name = ourModel.getName();
            final Session session = context.getSession();
            try {
                final String parentPath = ourModel.getParentPath();
                if (session.itemExists(parentPath)) {
                    final Node node = session.getNode(parentPath);
                    if (value instanceof String[]) {
                        node.setProperty(name, (String[]) value);
                    } else if (value instanceof List) {
                        final Class<?> type = annotation.type();
                        if (type.equals(String.class)) {
                            @SuppressWarnings("unchecked")
                            final List<String> values = (List<String>) value;
                            final String[] stringValues = values.toArray(new String[values.size()]);
                            node.setProperty(name, stringValues);
                        } else if (type.equals(int.class) || type.equals(Integer.class)) {
                            @SuppressWarnings("unchecked")
                            final Collection<Integer> values = (Collection<Integer>) value;
                            final List<Value> jcrValues = new ArrayList<>();
                            for (Integer integer : values) {
                                final Value val = new LongValue(integer);
                                jcrValues.add(val);
                            }
                            final Value[] valArray = jcrValues.toArray(new Value[values.size()]);
                            node.setProperty(name, valArray);
                        } else if (type.equals(long.class) || type.equals(Long.class)) {
                            @SuppressWarnings("unchecked")
                            final Collection<Long> values = (Collection<Long>) value;
                            final List<Value> jcrValues = new ArrayList<>();
                            for (Long l : values) {
                                final Value val = new LongValue(l);
                                jcrValues.add(val);
                            }
                            final Value[] valArray = jcrValues.toArray(new Value[values.size()]);
                            node.setProperty(name, valArray);
                        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
                            @SuppressWarnings("unchecked")
                            final Collection<Boolean> values = (Collection<Boolean>) value;
                            final List<Value> jcrValues = new ArrayList<>();
                            for (Boolean b : values) {
                                final Value val = new BooleanValue(b);
                                jcrValues.add(val);
                            }
                            final Value[] valArray = jcrValues.toArray(new Value[values.size()]);
                            node.setProperty(name, valArray);
                        } else if (type.equals(double.class) || type.equals(Double.class)) {
                            @SuppressWarnings("unchecked")
                            final Collection<Double> values = (Collection<Double>) value;
                            final List<Value> jcrValues = new ArrayList<>();
                            for (Double d : values) {
                                final Value val = new DoubleValue(d);
                                jcrValues.add(val);
                            }
                            final Value[] valArray = jcrValues.toArray(new Value[values.size()]);
                            node.setProperty(name, valArray);
                        } else if (type.equals(Calendar.class)) {
                            @SuppressWarnings("unchecked")
                            final Collection<Calendar> values = (Collection<Calendar>) value;
                            final List<Value> jcrValues = new ArrayList<>();
                            for (Calendar c : values) {
                                final Value val = new DateValue(c);
                                jcrValues.add(val);
                            }
                            final Value[] valArray = jcrValues.toArray(new Value[values.size()]);
                            node.setProperty(name, valArray);
                        } else {
                            throw new NotImplementedException("Property writer not implemented for: " + type);
                        }
                    } else {
                        throw new NotImplementedException("Property writer not implemented for: " + value.getClass());
                    }

                } else {
                    log.error("Parent couldn't be found for path: {}", parentPath);
                }
            } catch (RepositoryException e) {
                log.error("Error writing property", e);
            }
            return null;
        }
    }
}
