/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.solr;


import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.slf4j.LoggerFactory;

/**
 * A class to map objects to and from solr documents.
 *
 */
public class DocumentObjectBinder extends org.apache.solr.client.solrj.beans.DocumentObjectBinder {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DocumentObjectBinder.class);

    private final Map<Class, List<DocField>> infocache = new ConcurrentHashMap<Class, List<DocField>>();

    public final static String HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME = "hippo_content_bean_fqn_clazz_name";
    public final static String HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY = "hippo_content_bean_fqn_clazz_hierarchy";
    public final static String HIPPO_CONTENT_BEAN_PATH_HIERARCHY = "hippo_path_hierarchy";
    public final static String HIPPO_CONTENT_BEAN_PATH_DEPTH = "hippo_path_depth";

    public DocumentObjectBinder() {
    }

    public <T> T getBean(Class<T> clazz, SolrDocument sdoc) {
        String beanClazz = (String) sdoc.get(HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME);
        if (beanClazz == null) {
            // we cannot return beans that do not have a 'clazz'
            return null;
        }

        Object obj = null;
        try {
            Class c = Thread.currentThread().getContextClassLoader().loadClass(beanClazz);
            if (!clazz.isAssignableFrom(c)) {
                // log warning
                return null;
            }
            obj = c.newInstance();
            List<DocField> fields = getDocFields(c);
            for (int i = 0; i < fields.size(); i++) {
                DocField docField = fields.get(i);
                docField.inject(obj, sdoc);
            }
            return (T) obj;
        } catch (Exception e) {
            log.warn("Could not instantiate object of class " + beanClazz, e);
            return null;
        }
    }

    @Override
    public <T> List<T> getBeans(Class<T> clazz, SolrDocumentList solrDocList) {

        List<T> result = new ArrayList<T>();
        for (int j = 0; j < solrDocList.size(); j++) {
            SolrDocument sdoc = solrDocList.get(j);
            T bean = (T) getBean(clazz, sdoc);
            result.add(bean);
        }
        return result;
    }


    @Override
    public SolrInputDocument toSolrInputDocument(Object obj) {
        List<DocField> fields = getDocFields(obj.getClass());
        if (fields.isEmpty()) {
            throw new RuntimeException("class: " + obj.getClass() + " does not define any fields.");
        }

        SolrInputDocument doc = new SolrInputDocument();
        for (DocField field : fields) {
            doc.setField(field.name, field.get(obj));
        }
        // default field name we always add:
        doc.setField(HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME, obj.getClass().getName());

        setClassHierarchyField(doc, obj.getClass());
        
        // we index the path and depth of a HippoBean extra : This 
        // is not the nicest way to do it here : It could also be achieved by just using the 
        // solr path field, and use copyField to hippo_path_hierarchy and hippo_path_depth
        // and two custom tokenizers. For now, this is a shortcut
        if (obj instanceof ContentBean) {
            String path = ((ContentBean)obj).getPath();
            boolean startedWithSlash = false;
            if (path.startsWith("/")) {
                path = path.substring(1);
                startedWithSlash = true;
            }
            String[] split = path.split("/");
            doc.setField(HIPPO_CONTENT_BEAN_PATH_DEPTH, split.length);
            StringBuilder builder = new StringBuilder();
            for (String segment : split) {
                if (builder.length() == 0) {
                    if (startedWithSlash) {
                        builder.append("/");
                    }
                    builder.append(segment);
                    doc.addField(HIPPO_CONTENT_BEAN_PATH_HIERARCHY, builder.toString());
                } else {
                    builder.append("/").append(segment);
                    doc.addField(HIPPO_CONTENT_BEAN_PATH_HIERARCHY, builder.toString());
                }
            }
        }
        return doc;
    }


    private List<DocField> getDocFields(Class clazz) {
        boolean caching = true;
        if (caching) {
            List<DocField> fields = infocache.get(clazz);
            if (fields == null) {
                synchronized (infocache) {
                    infocache.put(clazz, fields = collectInfo(clazz));
                }
            }
            return fields;
        } else {
            return collectInfo(clazz);
        }

    }

    private List<DocField> collectInfo(Class clazz) {
        List<DocField> fields = new ArrayList<DocField>();
        // SEARCH for public getter annotated with IndexField
        for (Method method : clazz.getMethods()) {
            Method annotatedMethod = doGetAnnotatedMethod(method, IndexField.class);
            if (annotatedMethod != null) {
                if (Modifier.isPublic(method.getModifiers())) {
                    fields.add(new DocField(annotatedMethod));
                } else {
                    log.warn("Skipping member '{}' which does have an 'IndexField' annotation but it is not accessible.", method.toString());
                }
            }
        }
        return fields;
    }


    /**
     * returns the annotated method with annotation clazz and null if the clazz annotation is not present
     * @param m
     * @param clazz the annotation to look for
     * @return the {@link Method} that contains the annotation <code>clazz</code> and <code>null</code> if none found
     */
    private static Method doGetAnnotatedMethod(final Method m, Class<IndexField> clazz) {

        if (m == null) {
            return m;
        }

        IndexField annotation = m.getAnnotation(clazz);
        if(annotation != null ) {
            // found Subscribe annotation
            return m;
        }

        Class<?> superC = m.getDeclaringClass().getSuperclass();
        if (superC != null && Object.class != superC) {
            try {
                Method method = doGetAnnotatedMethod(superC.getMethod(m.getName(), m.getParameterTypes()), clazz);
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        for (Class<?> i : m.getDeclaringClass().getInterfaces()) {
            try {
                Method method = doGetAnnotatedMethod(i.getMethod(m.getName(), m.getParameterTypes()), clazz);
                if (method != null) {
                    return method;
                }
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }

        return null;
    }

    
    private static void setClassHierarchyField(SolrInputDocument doc, Class<?> clazz) {

        doc.addField(HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY, clazz.getName());

        Class<?> superC = clazz.getSuperclass();
        if (superC != null && Object.class != superC) {
            setClassHierarchyField(doc, superC);
        }
        for (Class<?> i : clazz.getInterfaces()) {
            setClassHierarchyField(doc, i);
        }
    }
    
    private static class DocField {
        private String name;
        private Method getter;
        private Method setter;
        private Class type;
        private boolean isArray = false, isList = false;

        /*
        * dynamic fields may use a Map based data structure to bind a given field.
        * if a mapping is done using, "Map<String, List<String>> foo", <code>isContainedInMap</code>
        * is set to <code>TRUE</code> as well as <code>isList</code> is set to <code>TRUE</code>
        */
        boolean isContainedInMap = false;
        private Pattern dynamicFieldNamePatternMatcher;

        public DocField(Method method) {
            getter = method;
            IndexField annotation = method.getAnnotation(IndexField.class);
            storeName(annotation);
            storeType();

            // Look for a matching getter for either field or setter.

            String gname = null;
            Class<?> declaringClass = null;

            String setterName = null;
            if (getter.getName().startsWith("get")) {
                setterName = "set" + getter.getName().substring(3);
            } else if (getter.getName().startsWith("is")) {
                setterName = "set" + getter.getName().substring(2);
            }
            if (setterName != null) {
                try {
                    // gets the public setter and exception if none available
                    setter = method.getDeclaringClass().getMethod(setterName, getter.getReturnType());
                } catch (NoSuchMethodException e) {
                    log.debug("There is no public setter for '{}' so that field cannot be populated", getter.getName());
                } catch (SecurityException e) {
                    log.debug("There is no public setter for '{}' so that field cannot be populated", getter.getName());
                }
            }
        }

        private void storeName(IndexField annotation) {
            if (annotation.name().equals(IndexField.DEFAULT)) {
                String getterName = getter.getName();
                if (getterName.startsWith("get") && getterName.length() > 3) {
                    name = getterName.substring(3, 4).toLowerCase() + getterName.substring(4);
                } else {
                    name = getter.getName();
                } 
            }
            //dynamic fields are annotated as @Field("categories_*")
            else if (annotation.name().indexOf('*') >= 0) {
                //if the field was annotated as a dynamic field, convert the name into a pattern
                //the wildcard (*) is supposed to be either a prefix or a suffix, hence the use of replaceFirst
                name = annotation.name().replaceFirst("\\*", "\\.*");
                dynamicFieldNamePatternMatcher = Pattern.compile("^" + name + "$");
            } else {
                name = annotation.name();
            }
        }

        private void storeType() {
            type = getter.getReturnType();
            if (type == Collection.class || type == List.class || type == ArrayList.class) {
                type = Object.class;
                isList = true;
            } else if (type == byte[].class) {
                //no op
            } else if (type.isArray()) {
                isArray = true;
                type = type.getComponentType();
            }
            //corresponding to the support for dynamicFields
            else if (type == Map.class || type == HashMap.class) {
                isContainedInMap = true;
                //assigned a default type
                type = Object.class;
                    if (getter.getGenericReturnType() instanceof ParameterizedType) {
                        //check what are the generic values
                    ParameterizedType parameterizedType = (ParameterizedType) getter.getGenericReturnType();
                    Type[] types = parameterizedType.getActualTypeArguments();
                    if (types != null && types.length == 2 && types[0] == String.class) {
                        //the key should always be String
                        //Raw and primitive types
                        if (types[1] instanceof Class) {
                            //the value could be multivalued then it is a List ,Collection,ArrayList
                            if (types[1] == Collection.class || types[1] == List.class || types[1] == ArrayList.class) {
                                type = Object.class;
                                isList = true;
                            } else {
                                //else assume it is a primitive and put in the source type itself
                                type = (Class) types[1];
                            }
                        }
                        //Of all the Parameterized types, only List is supported
                        else if (types[1] instanceof ParameterizedType) {
                            Type rawType = ((ParameterizedType) types[1]).getRawType();
                            if (rawType == Collection.class || rawType == List.class || rawType == ArrayList.class) {
                                type = Object.class;
                                isList = true;
                            }
                        }
                        //Array types
                        else if (types[1] instanceof GenericArrayType) {
                            type = (Class) ((GenericArrayType) types[1]).getGenericComponentType();
                            isArray = true;
                        }
                        //Throw an Exception if types are not known
                        else {
                            throw new RuntimeException("Allowed type for values of mapping a dynamicField are : " +
                                    "Object, Object[] and List");
                        }
                    }
                } 
            }
        }

        /**
         * Called by the {@link #inject} method to read the value(s) for a field This method supports
         * reading of all "matching" fieldName's in the <code>SolrDocument</code>
         * <p/>
         * Returns <code>SolrDocument.getFieldValue</code> for regular fields, and <code>Map<String,
         * List<Object>></code> for a dynamic field. The key is all matching fieldName's.
         */
        @SuppressWarnings("unchecked")
        private Object getFieldValue(SolrDocument sdoc) {
            Object fieldValue = sdoc.getFieldValue(name);
            if (fieldValue != null) {
                //this is not a dynamic field. so return te value
                return fieldValue;
            }
            //reading dynamic field values
            if (dynamicFieldNamePatternMatcher != null) {
                Map<String, Object> allValuesMap = null;
                ArrayList allValuesList = null;
                if (isContainedInMap) {
                    allValuesMap = new HashMap<String, Object>();
                } else {
                    allValuesList = new ArrayList();
                }
                for (String field : sdoc.getFieldNames()) {
                    if (dynamicFieldNamePatternMatcher.matcher(field).find()) {
                        Object val = sdoc.getFieldValue(field);
                        if (val == null) {
                            continue;
                        }
                        if (isContainedInMap) {
                            if (isList) {
                                if (!(val instanceof List)) {
                                    ArrayList al = new ArrayList();
                                    al.add(val);
                                    val = al;
                                }
                            } else if (isArray) {
                                if (!(val instanceof List)) {
                                    Object[] arr = (Object[]) Array.newInstance(type, 1);
                                    arr[0] = val;
                                    val = arr;
                                } else {
                                    val = Array.newInstance(type, ((List) val).size());
                                }
                            }
                            allValuesMap.put(field, val);
                        } else {
                            if (val instanceof Collection) {
                                allValuesList.addAll((Collection) val);
                            } else {
                                allValuesList.add(val);
                            }
                        }
                    }
                }
                if (isContainedInMap) {
                    return allValuesMap.isEmpty() ? null : allValuesMap;
                } else {
                    return allValuesList.isEmpty() ? null : allValuesList;
                }
            }
            return null;
        }

        <T> void inject(T obj, SolrDocument sdoc) {
            Object val = getFieldValue(sdoc);
            if (val == null) {
                return;
            }
            if (isArray && !isContainedInMap) {
                List list = null;
                if (val.getClass().isArray()) {
                    set(obj, val);
                    return;
                } else if (val instanceof List) {
                    list = (List) val;
                } else {
                    list = new ArrayList();
                    list.add(val);
                }
                set(obj, list.toArray((Object[]) Array.newInstance(type, list.size())));
            } else if (isList && !isContainedInMap) {
                if (!(val instanceof List)) {
                    ArrayList list = new ArrayList();
                    list.add(val);
                    val = list;
                }
                set(obj, val);
            } else if (isContainedInMap) {
                if (val instanceof Map) {
                    set(obj, val);
                }
            } else {
                set(obj, val);
            }

        }


        private void set(Object obj, Object v) {
            if (v != null && type == ByteBuffer.class && v.getClass() == byte[].class) {
                v = ByteBuffer.wrap((byte[]) v);
            }
            try {
                if (setter != null) {
                    Object formatted = formatValueToField(setter, v);
                    setter.invoke(obj, formatted);
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception while setting value : " + v + " on " + setter, e);
            }
        }


        private Object formatValueToField(Method setter, Object v) {
            if (setter.getParameterTypes().length != 1 ) {
                throw new IllegalArgumentException("Setter method '"+setter+"' is supposed to have a single argument");
            }
            if (setter.getParameterTypes()[0].equals(Calendar.class) && (v instanceof Date)) {
                Calendar cal = Calendar.getInstance();
                cal.setTime((Date) v);
                return cal;
            }
            return v;
        }

        public Object get(final Object obj) {

            if (getter != null) {
                try {
                    return format(getter.invoke(obj, (Object[]) null));
                } catch (Exception e) {
                    throw new RuntimeException("Exception while getting value: " + getter, e);
                }
            }

            return null;
        }

        private Object format(Object val) {
            if (val instanceof Calendar) {
                return DateUtil.getThreadLocalDateFormat().format(((Calendar) val).getTime());
            }if (val instanceof Date) {
                return DateUtil.getThreadLocalDateFormat().format((Date)val);
            }
            return val;
        }
    }
}
