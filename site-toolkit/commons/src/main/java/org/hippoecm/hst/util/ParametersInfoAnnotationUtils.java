/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.lang.annotation.Annotation;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to find or create a <code>ParametersInfo</code> annotation by reading the annotation of a component
 * class or the configuration for the component.
 * <P>
 * Note: A component class can be annotated with {@link ParametersInfo} to specify the type of <code>ParametersInfo</code>.
 * Also, a component can be configured with the name of the specific class representing the type of <code>ParametersInfo</code>
 * in the <code>hst:parametersinfoclassname</code> directly, equivalently to {@link ParametersInfo#type()} in annotation.
 * </P>
 */
public class ParametersInfoAnnotationUtils {

    private static Logger log = LoggerFactory.getLogger(ParametersInfoAnnotationUtils.class);

    private ParametersInfoAnnotationUtils() {
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of the class of the {@code component}
     * or the {@code componentConfig} directly.
     * @param component component instance
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Object component, ComponentConfiguration componentConfig) {
        return getParametersInfoAnnotation(component.getClass(), componentConfig);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of a {@code componentClazz} or the
     * {@code componentConfig} directly.
     * @param componentClazz component class
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Class<?> componentClazz,
            ComponentConfiguration componentConfig) {
        return getParametersInfoAnnotation(componentClazz, (componentConfig != null)
                ? componentConfig.getRawLocalParameters().get(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME)
                : null);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of the class of the {@code component}
     * or the {@code componentConfig} directly.
     * @param component component instance
     * @param componentConfig HstComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Object component,
            HstComponentConfiguration componentConfig) {
        return getParametersInfoAnnotation(component.getClass(), componentConfig);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of a {@code componentClazz} or the
     * {@code componentConfig} directly.
     * @param componentClazz component class
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Class<?> componentClazz,
            HstComponentConfiguration componentConfig) {
        return getParametersInfoAnnotation(componentClazz,
                (componentConfig != null) ? componentConfig.getParametersInfoClassName() : null);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentConfig} directly.
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(HstComponentConfiguration componentConfig) {
        if (componentConfig != null) {
            Class<?> componentClazz = null;

            try {
                String componentClassName = componentConfig.getComponentClassName();
                componentClazz = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            } catch (Exception e) {
                log.warn("Component class not loadable: {}", componentClazz);
            }

            return getParametersInfoAnnotation(componentClazz, componentConfig.getParametersInfoClassName());
        }

        return null;
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentConfigNode} directly.
     * @param componentConfigNode component configuration node
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Node componentConfigNode) {
        if (componentConfigNode != null) {
            Class<?> componentClazz = null;
            String componentClassName = null;

            try {
                if (componentConfigNode.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME)) {
                    componentClassName = componentConfigNode
                            .getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString();
                    componentClazz = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
                }
            } catch (Exception e) {
                log.warn("Component class not loadable, configured by {} property: {}",
                        HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME, componentClassName);
            }

            String paramsInfoClassName = null;

            try {
                if (componentConfigNode.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME)) {
                    paramsInfoClassName = componentConfigNode
                            .getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME).getString();
                }
            } catch (Exception e) {
                log.warn("Failed to load class configured by {} property: {}",
                        HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME, paramsInfoClassName, e);
            }

            return getParametersInfoAnnotation(componentClazz, paramsInfoClassName);
        }

        return null;
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentClazz} or create one from {@code parametersInfoClassName}
     * if specified.
     * @param componentClazzName component class name
     * @param parametersInfoClassName class name for <code>ParametersInfo</code> type
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(String componentClazzName,
            String parametersInfoClassName) {
        Class<?> componentClazz = null;

        if (componentClazzName != null && !componentClazzName.isEmpty()) {
            try {
                componentClazz = Thread.currentThread().getContextClassLoader().loadClass(componentClazzName);
            } catch (Exception e) {
                log.warn("Component class not loadable: {}", componentClazzName);
            }
        }

        return getParametersInfoAnnotation(componentClazz, parametersInfoClassName);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentClazz} or create one from {@code parametersInfoClassName}
     * if specified.
     * @param componentClazz component class
     * @param parametersInfoClassName class name for <code>ParametersInfo</code> type
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(Class<?> componentClazz, String parametersInfoClassName) {
        if (parametersInfoClassName != null && !parametersInfoClassName.isEmpty()) {
            try {
                Class<?> paramsInfoType = Thread.currentThread().getContextClassLoader()
                        .loadClass(parametersInfoClassName);
                return createDynamicParametersInfo(paramsInfoType);
            } catch (ClassNotFoundException e) {
                log.warn("Cannot load parametersInfo class: {}", parametersInfoClassName, e);
            }
        }

        if (componentClazz != null && componentClazz.isAnnotationPresent(ParametersInfo.class)) {
            return componentClazz.getAnnotation(ParametersInfo.class);
        }

        return null;
    }

    private static ParametersInfo createDynamicParametersInfo(final Class<?> type) {
        return new ParametersInfo() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ParametersInfo.class;
            }

            @Override
            public Class<?> type() {
                return type;
            }
        };
    }
}
