/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.dynamic;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.content.beans.standard.HippoHtml;
import org.hippoecm.hst.content.beans.standard.HippoResourceBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * Creates methods for dynamic beans on the fly.
 */
public class DynamicBeanBuilder {
    private static final Logger log = LoggerFactory.getLogger(DynamicBeanBuilder.class);

    /**
     * The method names below belong to the HippoBean and are used to generate dynamic bean
     * definitions by using byte buddy.
     */
    private static final String METHOD_GET_PROPERTY = "getProperty";
    private static final String METHOD_GET_CHILD_BEANS_BY_NAME = "getChildBeansByName";
    private static final String METHOD_GET_HIPPO_HTML = "getHippoHtml";
    private static final String METHOD_GET_LINKED_BEANS = "getLinkedBeans";
    private static final String METHOD_GET_LINKED_BEAN = "getLinkedBean";
    private static final String METHOD_GET_BEANS = "getBeans";
    private static final String METHOD_GET_BEAN = "getBean";

    private Builder<? extends HippoBean> builder;
    private final Class<? extends HippoBean> parentBean;
    private boolean methodAdded = false;

    boolean isMethodAdded() {
        return methodAdded;
    }

    /**
     * Byte buddy doesn't support automated if else blocks, so an interceptor must
     * be used to handle if else. The whole logic is copied from the essentials bean
     * generation tool.
     *
     */
    public static class SingleDocbaseInterceptor {

        private final String propertyName;

        SingleDocbaseInterceptor(final String propertyName) {
            this.propertyName = propertyName;
        }

        public HippoBean getDocbaseItem(@Super(proxyType = TargetType.class) Object superObject) {
            final HippoBean superBean = (HippoBean) superObject;

            final String item = superBean.getProperty(propertyName);
            if (item == null) {
                return null;
            }

            return superBean.getBeanByUUID(item, HippoBean.class);
        }
    }

    /**
     * Byte buddy doesn't support automated if else blocks, so an interceptor must
     * be used to handle if else. The whole logic is copied from the essentials bean
     * generation tool. 
     *
     */
    public static class MultipleDocbaseInterceptor {

        private final String propertyName;

        MultipleDocbaseInterceptor(final String propertyName) {
            this.propertyName = propertyName;
        }

        public List<HippoBean> getDocbaseItem(@Super(proxyType = TargetType.class) Object superObject) {
            final HippoBean superBean = (HippoBean) superObject;

            final String[] items = superBean.getProperty(propertyName);
            if (items == null) {
                return Collections.emptyList();
            }

            return Arrays.stream(items)
                         .map(item -> superBean.getBeanByUUID(item, HippoBean.class))
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
        }
    }

    /**
     * Creates a class definition regarding of a given name and a parent bean. The created
     * bean is extended from the parent bean.
     * 
     * @param className the name of the class (eg: NewsDocument)
     * @param parentBean the parent bean (eg: BaseDocument)
     */
    DynamicBeanBuilder(final String className, final Class<? extends HippoBean> parentBean) {
        builder = new ByteBuddy().with(new NamingStrategy.SuffixingRandom(className)).subclass(parentBean);
        this.parentBean = parentBean;
    }

    void addBeanMethodString(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? String[].class : String.class;
        addBeanMethodPrimitive(methodName, returnType, propertyName);
    }

    void addBeanMethodCalendar(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Calendar[].class : Calendar.class;
        addBeanMethodPrimitive(methodName, returnType, propertyName);
    }

    void addBeanMethodBoolean(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Boolean[].class : Boolean.class;
        addBeanMethodPrimitive(methodName, returnType, propertyName);
    }

    void addBeanMethodLong(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Long[].class : Long.class;
        addBeanMethodPrimitive(methodName, returnType, propertyName);
    }

    void addBeanMethodDouble(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Double[].class : Double.class;
        addBeanMethodPrimitive(methodName, returnType, propertyName);
    }

    void addBeanMethodDocbase(final String methodName, final String propertyName, final boolean multiple) {
        try {
            builder = builder
                    .defineMethod(methodName, HippoBean.class, Modifier.PUBLIC)
                    .intercept(MethodDelegation.to(
                            multiple ? new MultipleDocbaseInterceptor(propertyName) : new SingleDocbaseInterceptor(propertyName)));
            methodAdded = true;
        } catch (IllegalArgumentException e) {
            log.error("Cant't define method {} : {}", methodName, e);
        }
    }

    void addBeanMethodHippoHtml(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_CHILD_BEANS_BY_NAME, HippoHtml.class, propertyName);
        } else {
            addSimpleGetMethod(methodName, HippoHtml.class, METHOD_GET_HIPPO_HTML, propertyName);
        }
    }

    void addBeanMethodImageLink(final String methodName, final String propertyName, final Class<? extends HippoBean> galleryImageSetType, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_LINKED_BEANS, galleryImageSetType, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_LINKED_BEAN, galleryImageSetType, propertyName);
        }
    }

    void addBeanMethodInternalImageSet(final String methodName, final Class<?> returnType, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_LINKED_BEANS, returnType, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_LINKED_BEAN, returnType, propertyName);
        }
    }

    void addBeanMethodHippoMirror(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_LINKED_BEANS, HippoBean.class, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_LINKED_BEAN, HippoBean.class, propertyName);
        }
    }

    void addBeanMethodHippoImage(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_BEANS, HippoGalleryImageBean.class, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_BEAN, HippoGalleryImageBean.class, propertyName);
        }
    }

    void addBeanMethodHippoResource(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_CHILD_BEANS_BY_NAME, HippoResourceBean.class, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_BEAN, HippoResourceBean.class, propertyName);
        }
    }

    void addBeanMethodInternalType(final String methodName, final Class<?> returnType, final String propertyName, final boolean multiple) {
        if (multiple) {
            addCollectionGetMethod(methodName, METHOD_GET_CHILD_BEANS_BY_NAME, returnType, propertyName);
        } else {
            addHippoTypeGetMethod(methodName, METHOD_GET_BEAN, returnType, propertyName);
        }
    }

    private void addBeanMethodPrimitive(final String methodName, final Class<?> returnType, final String propertyName) {
        addSimpleGetMethod(methodName, returnType, METHOD_GET_PROPERTY, propertyName);
    }

    /**
     * Invokes the super method from the parent bean with the given property name as a parameter.
     * 
     * <pre>
     * public returnType methodName() {  
     *     return HippoBean.superMethodName(superMethodParameter);
     * }
     * </pre>
     */
    private void addSimpleGetMethod(final String methodName, final Class<?> returnType, final String superMethodName,
            final String propertyName) {
        try {
            final Class<?> delegateeClass = (METHOD_GET_HIPPO_HTML.equals(superMethodName))
                    ? ((parentBean.isAssignableFrom(HippoCompound.class) || parentBean.getSuperclass().isAssignableFrom(HippoCompound.class)) ? HippoCompound.class : HippoDocument.class)
                    : HippoBean.class;

            builder = builder
                        .defineMethod(methodName, returnType, Modifier.PUBLIC)
                        .intercept(
                            MethodCall
                                .invoke(delegateeClass.getDeclaredMethod(superMethodName, String.class))
                                .onSuper()
                                .with(propertyName)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            methodAdded = true;
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            log.error("Can't define method {} with delegate method {} with return type {} : {}", methodName, superMethodName, returnType, e);
        }
    }

    private void addCollectionGetMethod(final String methodName, final String superMethodName,
            final Class<?> superMethodReturnType, final String propertyName) {
        addMultiParamGetMethod(methodName, List.class, superMethodName, superMethodReturnType, propertyName);
    }

    private void addHippoTypeGetMethod(final String methodName, final String superMethodName,
            final Class<?> superMethodReturnType, final String propertyName) {
        addMultiParamGetMethod(methodName, superMethodReturnType, superMethodName, superMethodReturnType, propertyName);
    }

    /**
     * Invokes the super method from the parent bean with the given document type name as a parameter.
     * 
     * <pre>
     * public returnType methodName() {  
     *     return HippoBean.superMethodName(propertyName, superMethodReturnType);
     * }
     * </pre>
     */
    private void addMultiParamGetMethod(final String methodName, final Class<?> returnType,
            final String superMethodName, final Class<?> superMethodReturnType, final String propertyName) {
        try {
            builder = builder
                        .defineMethod(methodName, returnType, Modifier.PUBLIC)
                        .intercept(
                            MethodCall
                                .invoke(HippoBean.class.getDeclaredMethod(superMethodName, String.class, Class.class))
                                .onSuper()
                                .with(propertyName, superMethodReturnType)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
            methodAdded = true;
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            log.error("Can't define method {} with delegate method {} with return type {} : {}", methodName, superMethodName, superMethodReturnType, e);
        }
    }
    
    Class<? extends HippoBean> getParentBeanClass(){
        return parentBean;
    }

    public Class<? extends HippoBean> create() {
        return builder.make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
    }

}
