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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
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
 * Creates runtime methods for dynamic beans
 */
public class DynamicBeanBuilder {
    private static final Logger log = LoggerFactory.getLogger(DynamicBeanBuilder.class);
    private static final String METHOD_GET_PROPERTY = "getProperty";
    private static final String METHOD_GET_CHILD_BEANS_BY_NAME = "getChildBeansByName";
    private static final String METHOD_GET_HIPPO_HTML = "getHippoHtml";
    private static final String METHOD_GET_LINKED_BEANS = "getLinkedBeans";
    private static final String METHOD_GET_LINKED_BEAN = "getLinkedBean";
    private static final String METHOD_GET_BEANS = "getBeans";
    private static final String METHOD_GET_BEAN = "getBean";

    private Builder<? extends HippoBean> builder;
    private final Class<? extends HippoBean> parentBean;

    public static class SingleDocbaseInterceptor {

        private final String propertyName;

        public SingleDocbaseInterceptor(final String propertyName) {
            this.propertyName = propertyName;
        }

        public HippoBean getDocbaseItem(@Super(proxyType = TargetType.class) Object superObject) throws Throwable {
            final HippoBean superBean = (HippoBean) superObject;
            final String item = (String) superBean.getProperty(propertyName);
            if (item == null) {
                return null;
            }
            return superBean.getBeanByUUID(item, HippoBean.class);
        }
    }

    public static class MultipleDocbaseInterceptor {

        private final String propertyName;

        public MultipleDocbaseInterceptor(final String propertyName) {
            this.propertyName = propertyName;
        }

        public List<HippoBean> getDocbaseItem(@Super(proxyType = TargetType.class) Object superObject)
                throws Throwable {
            final HippoBean superBean = (HippoBean) superObject;
            final List<HippoBean> beans = new ArrayList<>();
            final String[] items = (String[]) superBean.getProperty(propertyName);
            if (items == null) {
                return beans;
            }
            for (String item : items) {
                final HippoBean bean = superBean.getBeanByUUID(item, HippoBean.class);
                if (bean != null) {
                    beans.add(bean);
                }
            }
            return beans;
        }
    }

    public DynamicBeanBuilder(final String className, final Class<? extends HippoBean> parentBean) {
        builder = new ByteBuddy().with(new NamingStrategy.SuffixingRandom(className)).subclass(parentBean);
        this.parentBean = parentBean;
    }

    public void addBeanMethodString(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? String[].class : String.class;
        addBeanMethodProperty(methodName, propertyName, returnType);
    }

    public void addBeanMethodCalendar(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Calendar[].class : Calendar.class;
        addBeanMethodProperty(methodName, propertyName, returnType);
    }

    public void addBeanMethodBoolean(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Boolean[].class : Boolean.class;
        addBeanMethodProperty(methodName, propertyName, returnType);
    }

    public void addBeanMethodLong(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Long[].class : Long.class;
        addBeanMethodProperty(methodName, propertyName, returnType);
    }

    public void addBeanMethodDouble(final String methodName, final String propertyName, final boolean multiple) {
        final Class<?> returnType = multiple ? Double[].class : Double.class;
        addBeanMethodProperty(methodName, propertyName, returnType);
    }

    public void addBeanMethodDocbase(final String methodName, final String propertyName, final boolean multiple) {
        try {
            if (multiple) {
                builder = builder
                            .defineMethod(methodName, HippoBean.class, Modifier.PUBLIC)
                            .intercept(MethodDelegation.to(new MultipleDocbaseInterceptor(propertyName)));
            } else {
                builder = builder
                            .defineMethod(methodName, HippoBean.class, Modifier.PUBLIC)
                            .intercept(MethodDelegation.to(new SingleDocbaseInterceptor(propertyName)));
            }
        } catch (IllegalArgumentException e) {
            log.error("Cant't define method {} : {}", methodName, e);
        }
    }

    public void addBeanMethodHippoHtml(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, HippoHtml.class, METHOD_GET_CHILD_BEANS_BY_NAME, propertyName);
        } else {
            addSimpleMethod(METHOD_GET_HIPPO_HTML, methodName, propertyName, HippoHtml.class);
        }
    }

    public void addBeanMethodImageLink(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, HippoGalleryImageSet.class, METHOD_GET_LINKED_BEANS, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_LINKED_BEAN, HippoGalleryImageSet.class, methodName, propertyName);
        }
    }

    public void addBeanMethodInternalImageSet(final Class<?> className, final String importPath, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, className, METHOD_GET_LINKED_BEANS, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_LINKED_BEAN, className, methodName, propertyName);
        }
    }

    public void addBeanMethodHippoMirror(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, HippoBean.class, METHOD_GET_LINKED_BEANS, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_LINKED_BEAN, HippoBean.class, methodName, propertyName);
        }
    }

    public void addBeanMethodHippoImage(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, HippoGalleryImageBean.class, METHOD_GET_BEANS, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_BEAN, HippoGalleryImageBean.class, methodName, propertyName);
        }
    }

    public void addBeanMethodHippoResource(final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, HippoResourceBean.class, METHOD_GET_CHILD_BEANS_BY_NAME, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_BEAN, HippoResourceBean.class, methodName, propertyName);
        }
    }

    public void addBeanMethodInternalType(final Class<?> returnType, final String methodName, final String propertyName, final boolean multiple) {
        if (multiple) {
            addParameterizedMethod(methodName, List.class, returnType, METHOD_GET_CHILD_BEANS_BY_NAME, propertyName);
        } else {
            addTwoArgumentsMethod(METHOD_GET_BEAN, returnType, methodName, propertyName);
        }
    }

    private void addBeanMethodProperty(final String methodName, final String propertyName, final Class<?> returnType) {
        addSimpleMethod(METHOD_GET_PROPERTY, methodName, propertyName, returnType);
    }

    private void addSimpleMethod(final String hippoMethodName, final String methodName, final String propertyName, final Class<?> returnType) {
        try {
            final Class<?> delegateeClass = (METHOD_GET_HIPPO_HTML.equals(hippoMethodName))
                    ? (parentBean.isAssignableFrom(HippoCompound.class) ? HippoCompound.class : HippoDocument.class)
                    : HippoBean.class;
            builder = builder
                        .defineMethod(methodName, returnType, Modifier.PUBLIC)
                        .intercept(
                            MethodCall
                                .invoke(delegateeClass.getDeclaredMethod(hippoMethodName, String.class))
                                .onSuper()
                                .with(propertyName)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            log.error("Cant't define method {} with deletage method {} with return type {} : {}", methodName, hippoMethodName, returnType, e);
        }
    }

    public void addParameterizedMethod(final String methodName, final Class<?> returnType, final Class<?> genericsType, final String returnMethodName, final String propertyName) {
        try {
            builder = builder
                        .defineMethod(methodName, returnType, Modifier.PUBLIC)
                        .intercept(
                            MethodCall
                                .invoke(HippoBean.class.getDeclaredMethod(returnMethodName, String.class, Class.class))
                                .onSuper()
                                .with(propertyName, genericsType)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            log.error("Cant't define method {} with deletage method {} with return type {} : {}", methodName, returnMethodName, returnType, e);
        }
    }

    public void addTwoArgumentsMethod(final String returnMethodName, final Class<?> returnType, final String methodName, final String propertyName) {
        try {
            builder = builder
                        .defineMethod(methodName, returnType, Modifier.PUBLIC)
                        .intercept(
                            MethodCall
                                .invoke(HippoBean.class.getDeclaredMethod(returnMethodName, String.class, Class.class))
                                .onSuper()
                                .with(propertyName, returnType)
                                .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
        } catch (NoSuchMethodException | IllegalArgumentException e) {
            log.error("Cant't define method {} with deletage method {} with return type {} : {}", methodName, returnMethodName, returnType, e);
        }
    }

    public Class<? extends HippoBean> create() {
        return builder.make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.INJECTION).getLoaded();
    }

}
