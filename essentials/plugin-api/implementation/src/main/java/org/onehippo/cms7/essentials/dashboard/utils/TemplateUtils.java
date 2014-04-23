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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.WordUtils;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;


/**
 * @version "$Id$"
 */
public final class TemplateUtils {

    private static Logger log = LoggerFactory.getLogger(TemplateUtils.class);

    private TemplateUtils() {
    }

    public static List<PropertyWrapper> parseBeanProperties(final Path beanPath) {
        // TODO implement

        List<PropertyWrapper> collection = new ArrayList<>();
        final ExistingMethodsVisitor methodCollection = JavaSourceUtils.getMethodCollection(beanPath);
        final List<MethodDeclaration> methods = methodCollection.getMethods();
        for (MethodDeclaration method : methods) {
            final SimpleName name = method.getName();
            final String identifier = name.getIdentifier();
            // skip all not getter methods:
            if (!validGetter(identifier)) {
                log.info("@Skipping method (not starting with get/is): {}", identifier);
                continue;
            }
            final Type returnType2 = method.getReturnType2();
            final boolean primitiveType = returnType2.isPrimitiveType();
            if (primitiveType) {
                // check if void:
                final String primitiveName = ((PrimitiveType) returnType2).getPrimitiveTypeCode().toString();
                if (primitiveName.equals("void")) {
                    // skip void types
                    log.info("@Skipping *void* method: {}", identifier);
                    continue;
                }
                // add type:
                final PropertyWrapper propertyWrapper = new PropertyWrapper(extractPropertyName(identifier), primitiveName);
                log.info("primitive type {}", identifier);
                propertyWrapper.setPrimitiveType(true);
                collection.add(propertyWrapper);
                continue;
            }

            //############################################
            // SIMPLE OBJECT TYPES
            //############################################
            if (returnType2.isSimpleType()) {

                final SimpleType myType = (SimpleType) returnType2;
                log.info("myType {}", myType);
                final String fullyQualifiedName = myType.getName().getFullyQualifiedName();
                collection.add(new PropertyWrapper(extractPropertyName(identifier), fullyQualifiedName));
                continue;
            }
            //############################################
            // ARRAY/COLLECTION  TYPES
            //############################################

            // TODO implement
            log.info("returnType2 {}", returnType2);
            log.info("name {}", name);
        }
        return collection;


    }

    private static String extractPropertyName(final String identifier) {
        if (identifier.startsWith("get")) {
            final String methodName = identifier.substring(3);
            if (methodName.length() > 0) {
                return WordUtils.uncapitalize(methodName);
            }
        }


        return null;
    }

    private static boolean validGetter(final String identifier) {
        return identifier.startsWith("get") || identifier.startsWith("is");
    }

    public static String replaceTemplateData(final String content, final Map<String, Object> data) {
        if (Strings.isNullOrEmpty(content)) {
            return content;
        }

        try {
            final Writer writer = new StringWriter();
            final MustacheFactory mf = new DefaultMustacheFactory();

            final StringReader reader = new StringReader(content);
            final Mustache mustache = mf.compile(reader, content);

            mustache.execute(writer, data);
            writer.flush();
            return writer.toString();
        } catch (IOException e) {
            log.error("Error flushing template", e);
        }
        return content;
    }

    public static String injectTemplate(final String templateName, final Map<String, Object> data, final Class<?> clazz) {
        final InputStream stream = clazz.getClassLoader().getResourceAsStream(templateName);
        if (stream == null) {
            return null;
        }
        try {
            final String content = GlobalUtils.readStreamAsText(stream);
            return replaceTemplateData(content, data);
        } catch (Exception e) {
            log.error("Error processing template", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
        return null;

    }

    public static class PropertyWrapper implements Serializable {

        private static final Set<String> BASE_TYPES = new ImmutableSet.Builder<String>()
                .add("String")
                .add("Double")
                .add("Long")
                .add("Float")
                .build();
        /*  private static final Set<String> HIPPO_TYPES = new ImmutableSet.Builder<String>()
                  .add("HippoHtml")
                  .build();*/
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final String returnType;
        private boolean collectionType;
        private boolean primitiveType;

        public PropertyWrapper(final String propertyName, final String returnType) {
            this.propertyName = propertyName;
            this.returnType = returnType;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getReturnType() {
            return returnType;
        }

        public boolean isPrimitiveType() {
            return primitiveType;
        }

        public void setPrimitiveType(final boolean primitiveType) {
            this.primitiveType = primitiveType;
        }

        public boolean isCollectionType() {
            return collectionType;
        }

        public void setCollectionType(final boolean collectionType) {
            this.collectionType = collectionType;
        }

        public boolean isDateCalendarType() {
            return returnType.equals("Calendar") || returnType.equals("Date");
        }

        public String getFormattedJspProperty(final String baseName) {
            // get supertype:
            if (primitiveType) {
                return "${" + baseName + '.' + propertyName + '}';
            } else if (BASE_TYPES.contains(returnType)) {
                return "${" + baseName + '.' + propertyName + '}';
            } else if (isDateCalendarType()) {
                if (returnType.equals("Calendar")) {
                    return "<fmt:formatDate value=\"${" + baseName + '.' + propertyName + ".time}\" type=\"both\" dateStyle=\"medium\" timeStyle=\"short\"/>";
                }
                return "<fmt:formatDate value=\"${" + baseName + '.' + propertyName + "}\" type=\"both\" dateStyle=\"medium\" timeStyle=\"short\"/>";
            } else if (returnType.equals("HippoHtml")) {
                return " <hst:html hippohtml=\"${" + baseName + '.' + propertyName + "}\"/>";
            } else if (returnType.equals("HippoGalleryImageSetBean")) {
                return "<img src=\"<hst:link hippobean=\"${" + baseName + '.' + propertyName + ".original}\"/>\"\" />";
            }
            //TODO check if local bean and fetch supertype
            log.warn("Missing property mapping for: {}", this);
            return "";
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("PropertyWrapper{");
            sb.append("propertyName='").append(propertyName).append('\'');
            sb.append(", returnType='").append(returnType).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
