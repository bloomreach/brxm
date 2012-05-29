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


import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.DateUtil;
import org.hippoecm.hst.content.beans.index.IgnoreForCompoundBean;
import org.hippoecm.hst.content.beans.index.IndexField;
import org.hippoecm.hst.content.beans.standard.ContentBean;
import org.hippoecm.hst.content.beans.standard.IdentifiableContentBean;
import org.slf4j.LoggerFactory;

/**
 * A class to map objects to and from solr documents.
 *
 */
public class DocumentObjectBinder extends org.apache.solr.client.solrj.beans.DocumentObjectBinder {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DocumentObjectBinder.class);

    private final Map<Class, List<DocField>> infocache = new ConcurrentHashMap<Class, List<DocField>>();
    private final Map<Class, Map<String,DocField>> infocacheMap = new ConcurrentHashMap<Class, Map<String, DocField>>();

    public final static String HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME = "hippo_content_bean_fqn_clazz_name";
    public final static String HIPPO_CONTENT_BEAN_FQN_CLAZZ_HIERARCHY = "hippo_content_bean_fqn_clazz_hierarchy";
    public final static String HIPPO_CONTENT_BEAN_PATH_HIERARCHY = "hippo_path_hierarchy";
    public final static String HIPPO_CONTENT_BEAN_PATH_DEPTH = "hippo_path_depth";

    private final static Pattern compoundFieldNamePatternMatcher = Pattern.compile("._[a-zA-Z]");
    private final static Map<Class<?>, String> supportedTypePatterns = new HashMap<Class<?>, String>();
    static {
        supportedTypePatterns.put(String.class, "_compound_t");
        supportedTypePatterns.put(Date.class, "_compound_dt");
        supportedTypePatterns.put(Calendar.class, "_compound_dt");
        supportedTypePatterns.put(Boolean.class, "_compound_b");
        supportedTypePatterns.put(boolean.class, "_compound_b");
        supportedTypePatterns.put(Integer.class, "_compound_i");
        supportedTypePatterns.put(int.class, "_compound_i");
        supportedTypePatterns.put(Long.class, "_compound_l");
        supportedTypePatterns.put(long.class, "_compound_l");
        supportedTypePatterns.put(Double.class, "_compound_d");
        supportedTypePatterns.put(double.class, "_compound_d");
        supportedTypePatterns.put(Float.class, "_compound_f");
        supportedTypePatterns.put(float.class, "_compound_f");
    }

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
                // log warningis
                return null;
            }
            obj = c.newInstance();
            List<DocField> fields = getDocFields(c);
            for (int i = 0; i < fields.size(); i++) {
                DocField docField = fields.get(i);
                docField.inject(obj, sdoc);
            }
            return (T) obj;
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not instantiate object of class " + beanClazz +". There must be a public no-args constructor", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not instantiate object of class " + beanClazz +".", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate object of class " + beanClazz +".", e);
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
    public SolrInputDocument toSolrInputDocument(Object obj) throws IllegalArgumentException, IllegalStateException {
        SolrInputDocument doc =  createSolrInputDocument(obj, false);

        if (doc.getFieldValue("id") == null) {
            throw new IllegalStateException("Cannot create SolrInputDocument for object '"+obj.toString()+"' because the 'id' field" +
                    " is missing.");
        }

        // default field name we always add:
        doc.setField(HIPPO_CONTENT_BEAN_FQN_CLAZZ_NAME, obj.getClass().getName());
        setClassHierarchyField(doc, obj.getClass());

        // we index the path and depth of a IdentifiableContentBean extra :
        if (obj instanceof IdentifiableContentBean) {
            String path = ((IdentifiableContentBean)obj).getPath();
            if (StringUtils.isBlank(path)) {
                // the path is the identifier for the index. Cannot index this object because it has an illegal state
                throw new IllegalStateException("IdentifiableContentBean is not allowed to have a #getPath to be null, empty or blank. Cannot index bean");
            }
            boolean startedWithSlash = false;
            if (path.startsWith("/")) {
                path = path.substring(1);
                startedWithSlash = true;
            }
            String[] split = path.split("/");
            doc.setField(HIPPO_CONTENT_BEAN_PATH_DEPTH, new Integer(split.length));
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

    /**
     * This method recursively creates a flattened Solr input document from a
     * possibly hierarchical IdentifiableContentBean (with sub beans in it) object
     * @param obj the obj to get the solr document from
     * @return
     */
    private SolrInputDocument createSolrInputDocument(final Object obj, boolean isCompound) {
        List<DocField> fields = getDocFields(obj.getClass());
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("class: " + obj.getClass() + " does not define any indexable fields.");
        }

        SolrInputDocument doc = new SolrInputDocument();
        for (DocField field : fields) {
            if (isCompound && field.ignoreForCompoundBean) {
                // we are indexing a compound bean, that has indication for the current
                // DocField that is should ignore the field for indexing the compound
                continue;
            }
            Object value = field.get(obj);
            if (value == null) {
                // do not index null values
                continue;
            }
            boolean processed = false;
            if (value instanceof Collection) {
                // check whether the items are a ContentBean
                Collection<Object> vals =  (Collection<Object>) value;
                if (!vals.isEmpty()) {
                    if (vals.iterator().next() instanceof ContentBean) {
                        // the @IndexField was added to a method that returns a collection of
                        // ContentBean's. Index all the annotated fields from this
                        // content bean
                        processed = true;
                        for (Object o : vals) {
                            try {
                                SolrInputDocument compound = createSolrInputDocument(o, true);
                                // now add all fields to the current SolrInputDocument fields
                                appendCompoundFields(doc, field.name, compound, o.getClass(), true);
                            } catch (IllegalArgumentException e) {
                                log.warn("Cannot index compound ContentBean : {}", e.toString());
                            }
                        }
                    }
                }
            } else if (value instanceof Object[]) {
                // check whether the items are a ContentBean
                Object[] vals =  (Object[]) value;
                if (vals.length > 0) {
                    if (vals[0] instanceof ContentBean) {
                        // the @IndexField was added to a method that returns a collection of
                        // ContentBean's. Index all the annotated fields from this
                        // content bean
                        processed = true;
                        for (Object o : vals) {
                            try {
                                SolrInputDocument compound = createSolrInputDocument(o, true);
                                // now add all fields to the current SolrInputDocument fields
                                appendCompoundFields(doc, field.name, compound, o.getClass(), true);
                            } catch (IllegalArgumentException e) {
                                log.warn("Cannot index compound ContentBean : {}", e.toString());
                            }
                        }
                    }
                }
            }else  if (value instanceof ContentBean) {
                try {
                    // the @IndexField was added to a method that returns a
                    // ContentBean. Index all the annotated fields from this
                    // content bean
                    processed = true;
                    SolrInputDocument compound = createSolrInputDocument(value, true);
                    // now add all fields to the current SolrInputDocument fields
                    appendCompoundFields(doc, field.name, compound, value.getClass(),  false);
                } catch (IllegalArgumentException e) {
                    log.warn("Cannot index compound ContentBean : {}", e.toString());
                }
            }
            if (!processed) {
                doc.setField(field.name, format(value));
            }
        }
        return doc;
    }

    private void appendCompoundFields(final SolrInputDocument doc, final String docFieldName, final SolrInputDocument compound,
                                      final Class<? extends Object> clazz, final boolean multiValued) {

        for (String fieldName : compound.getFieldNames()) {
            Collection<Object> values = compound.getFieldValues(fieldName);

            // check whether fieldName OWN its own extension ( _txt, _dt, etc)
            String compoundFieldName = docFieldName + "_" + fieldName;
            if (multiValued) {
                compoundFieldName = compoundFieldName + "_multiple";
            }

            // If the fieldName already matches the '._[a-zA-Z]' pattern, then, we do not append an extension
            // Otherwise, we need to append the fieldName with some extension (for example _dt, _l, _b etc) to
            // indicate some Solr field
            // For this, we need the return type of the DocField for 'fieldName' : We cannot use the value from 'values'
            // because for example Calendar or Date have already been formatted into their String representation
            if (compoundFieldNamePatternMatcher.matcher(fieldName).find()) {
                // does already have a _xxx pattern
                for (Object o : values) {
                    doc.addField(compoundFieldName, format(o));
                }
            } else {
                // does not yet have a _xxx pattern
                if (!values.isEmpty()) {
                    DocField docField = infocacheMap.get(clazz).get(fieldName);
                    if (docField == null) {
                        throw new IllegalStateException("docField for '"+clazz.getName()+"' was not expected to be null");
                    }
                    if (supportedTypePatterns.containsKey(docField.type)) {
                         compoundFieldName = compoundFieldName + supportedTypePatterns.get(docField.type);

                        for (Object o : values) {
                            doc.addField(compoundFieldName, format(o));
                        }
                    } else {
                        log.warn("Cannot index unsupported return type '{}' in compound bean. Supported types are '{}' ", docField.type.getName(), supportedTypePatterns.keySet());
                    }
                }
            }

        }
    }


    private List<DocField> getDocFields(Class clazz) {
       List<DocField> fields = infocache.get(clazz);
        if (fields == null) {
            synchronized (infocache) {
                fields = collectInfo(clazz);
                infocache.put(clazz, fields);

                HashMap<String, DocField> byMap = new HashMap<String, DocField>(fields.size());
                for (DocField f : fields) {
                    byMap.put(f.name, f);
                }
                infocacheMap.put(clazz, byMap);
            }
        }
        return fields;
    }

    private List<DocField> collectInfo(Class clazz) {
        List<DocField> fields = new ArrayList<DocField>();
        // SEARCH for public getter annotated with IndexField
        for (Method method : clazz.getMethods()) {
            Method annotatedIndexFieldMethod = doGetAnnotatedMethod(method, IndexField.class);
            if (annotatedIndexFieldMethod != null) {
                if (Modifier.isPublic(method.getModifiers())) {
                    // check whether there is somewhere in the class hierarchy also for the method
                    // an indication that it should be ignored for compound beans
                    boolean ignoreForCompoundBean =  false;
                    if (doGetAnnotatedMethod(method, IgnoreForCompoundBean.class) != null) {
                        // there is an annotation that indicates that compound beans should ignore the method for indexing
                        ignoreForCompoundBean = true;
                    }
                    fields.add(new DocField(annotatedIndexFieldMethod, ignoreForCompoundBean));
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
    private static Method doGetAnnotatedMethod(final Method m, Class<? extends Annotation> clazz) {

        if (m == null) {
            return m;
        }

        Annotation annotation = m.getAnnotation(clazz);
        if(annotation != null ) {
            // found annotation
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


    /*
     * Formats object val to String in case of val is a Date or Calendar object. When <code>val</code> is an 
     * array or collection of Date's or Calendar's, all values will be formatted
     */
    private static Object format(Object val) {
        if( val instanceof Collection ) {
            Collection<Object> vals = (Collection<Object>)val;
            if (!vals.isEmpty()) {
                Object first = vals.iterator().next();
                if (first instanceof Date) {
                    // assume now all items of this type
                    Collection<String> strings = new ArrayList<String>(vals.size());
                    for (Object o : vals) {
                        strings.add(DateUtil.getThreadLocalDateFormat().format((Date)o));
                    }
                    return strings;
                } else if (first instanceof Calendar) {
                    // assume now all items of this type
                    Collection<String> strings = new ArrayList<String>(vals.size());
                    for (Object o : vals) {
                        strings.add(DateUtil.getThreadLocalDateFormat().format((Calendar)o));
                    }
                    return strings;
                }
            } 
        } else if (val instanceof Object[]) {
            Object[] vals = (Object[])val;
            if (vals.length > 0) {
                if (vals[0] instanceof Date) {
                    // assume now all items of this type
                    Collection<String> strings = new ArrayList<String>(vals.length);
                    for (Object o : vals) {
                        strings.add(DateUtil.getThreadLocalDateFormat().format((Date) o));
                    }
                    return strings;
                } else if (vals[0] instanceof Calendar) {
                    // assume now all items of this type
                    Collection<String> strings = new ArrayList<String>(vals.length);
                    for (Object o : vals) {
                        strings.add(DateUtil.getThreadLocalDateFormat().format((Calendar)o));
                    }
                    return strings;
                }
            }
        } else if (val instanceof Calendar) {
            return DateUtil.getThreadLocalDateFormat().format(((Calendar) val).getTime());
        } else if (val instanceof Date) {
            return DateUtil.getThreadLocalDateFormat().format((Date)val);
        }
        return val;
    }
    
    private static class DocField {
        private String name;
        private Method getter;
        private Method setter;
        private Class type;
        private boolean ignoreForCompoundBean = false;
        private boolean isArray = false, isList = false;

        /*
        * dynamic fields may use a Map based data structure to bind a given field.
        * if a mapping is done using, "Map<String, List<String>> foo", <code>isContainedInMap</code>
        * is set to <code>TRUE</code> as well as <code>isList</code> is set to <code>TRUE</code>
        */
        boolean isContainedInMap = false;

        public DocField(Method method, final boolean ignoreForCompoundBean) {
            getter = method;
            this.ignoreForCompoundBean = ignoreForCompoundBean;
            IndexField annotation = method.getAnnotation(IndexField.class);
            storeName(annotation);
            storeType();

            // Look for a matching getter for either field or setter.
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
                    log.debug("There is no public setter for '{}' so that field will never be populated from a solr response.", getter.getName());
                } catch (SecurityException e) {
                    log.debug("There is no public setter for '{}' so that field will never be populated from a solr response.", getter.getName());
                }
            }
        }

        private void storeName(IndexField annotation) {
            if (annotation.name().equals(IndexField.DEFAULT)) {
                String getterName = getter.getName();
                if (getterName.startsWith("get") && getterName.length() > 3) {
                    name = getterName.substring(3, 4).toLowerCase() + getterName.substring(4);
                } else if (getterName.startsWith("is") && getterName.length() > 2) {
                    name = getterName.substring(2, 3).toLowerCase() + getterName.substring(3);
                } else {
                    name = getter.getName();
                } 
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
                    if (formatted != null) {
                        setter.invoke(obj, formatted);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Exception while setting value : " + v + " on " + setter, e);
            }
        }


        private Object formatValueToField(Method setter, Object v) {
            if (setter.getParameterTypes().length != 1 ) {
                throw new IllegalArgumentException("Setter method '"+setter+"' is supposed to have a single argument");
            }
            try {
                if (setter.getParameterTypes()[0].equals(Calendar.class)) {
                    // try to convert v to calendar
                    Calendar cal = Calendar.getInstance();
                    if (v instanceof String) {
                        Date date = DateUtil.getThreadLocalDateFormat().parse((String) v);
                        cal.setTime(date);
                    } else if (v instanceof Date) {
                        cal.setTime((Date) v);
                    }
                    return cal;
                }
                if (setter.getParameterTypes()[0].equals(Date.class)) {
                    // try to convert v to date
                    Calendar cal = null;
                    if (v instanceof String) {
                        return DateUtil.getThreadLocalDateFormat().parse((String) v);
                    } else if (v instanceof Calendar) {
                        cal = (Calendar) v;
                        return cal.getTime();
                    }
    
                }
            } catch (ParseException e) {
                log.warn("Could not parse value '' to Date. Return null", v.toString());
            }
            return v;
        }

        public Object get(final Object obj) {

            if (getter != null) {
                try {
                    return getter.invoke(obj, (Object[]) null);
                } catch (Exception e) {
                    throw new RuntimeException("Exception while getting value: " + getter, e);
                }
            }

            return null;
        }

    }
}
