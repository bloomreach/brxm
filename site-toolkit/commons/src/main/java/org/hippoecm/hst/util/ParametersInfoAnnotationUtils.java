/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT;

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
    public static ParametersInfo getParametersInfoAnnotation(HstComponent component, ComponentConfiguration componentConfig) {
        final Class<?> componentClazz = component.getClass();
        return getParametersInfoAnnotation(componentClazz,
                (componentConfig != null) ? componentConfig.getParametersInfoClassName() : null,
                componentClazz.getClassLoader());
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of the class of the {@code component}
     * or the {@code componentConfig} directly.
     * @param component component instance
     * @param componentConfig HstComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(final HstComponent component,
                                                             final HstComponentConfiguration componentConfig) {
        if (component == null) {
            return null;
        }
        final Class<?> componentClazz = component.getClass();
        return getParametersInfoAnnotation(componentClazz,
                (componentConfig != null) ? componentConfig.getParametersInfoClassName() : null,
                componentClazz.getClassLoader());
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from either the annotation of a {@code componentClazz} or the
     * {@code componentConfig} directly.
     * @param componentClazz component class
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(final Class<?> componentClazz,
            HstComponentConfiguration componentConfig) {
        return getParametersInfoAnnotation(componentClazz,
                (componentConfig != null) ? componentConfig.getParametersInfoClassName() : null,
                (componentClazz == null) ? Thread.currentThread().getContextClassLoader() : componentClazz.getClassLoader());
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentConfig} directly.
     * @param componentConfig ComponentConfiguration instance
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(final HstComponentConfiguration componentConfig) {
        if (componentConfig != null) {
            Class<?> componentClazz = null;

            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                String componentClassName = componentConfig.getComponentClassName();
                componentClazz = classLoader.loadClass(componentClassName);
            } catch (Exception e) {
                log.warn("Component class not loadable: {}", componentClazz);
            }

            return getParametersInfoAnnotation(componentClazz, componentConfig.getParametersInfoClassName(), classLoader);
        }

        return null;
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentItemNode} directly.The {@code componentItemNode}
     * must be of type {@code hst:containeritemcomponent} : Any other type will result in an {@link IllegalArgumentException}.
     * The reason that only type {@code hst:containeritemcomponent} is allowed is because this type does not support
     * complex inheritance and merging which nodes of type {@code hst:component} do support (in such a case the {@code hst:componentclassname}
     * or {@code hst:parametersinfoclassname} could be inherited from a different {@link Node}.
     * @param componentItemNode component configuration node
     * @return the type of <code>ParametersInfo</code>
     * @throws IllegalArgumentException if {@code componentItemNode} is not of type  {@code hst:containeritemcomponent}
     */
    public static ParametersInfo getParametersInfoAnnotation(final Node componentItemNode) throws RepositoryException {
        if (componentItemNode != null) {
            if (!componentItemNode.isNodeType(NODETYPE_HST_CONTAINERITEMCOMPONENT)) {
                final String msg =  String.format("#getParametersInfoAnnotation for a jcr Node is only allowed for node " +
                        "of type '%s' since these do not allow complex component inheritance and merging.", NODETYPE_HST_CONTAINERITEMCOMPONENT);
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }

            Class<?> componentClazz = null;
            String componentClassName = null;

            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (componentItemNode.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME)) {
                componentClassName = componentItemNode.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString();
                final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
                final ComponentManager componentManager = hstModelRegistry.getHstModel(classLoader).getComponentManager();
                final Object component = componentManager.getComponent(componentClassName);
                if (component != null) {
                    componentClazz = component.getClass();
                } else {
                    try {
                        componentClazz = classLoader.loadClass(componentClassName);
                    } catch (Exception e) {
                        log.warn("Component class not loadable, configured by {} property: {}", HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME,
                                componentClassName);
                    }
                }
            }

            String paramsInfoClassName = null;

            try {
                if (componentItemNode.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME)) {
                    paramsInfoClassName = componentItemNode
                            .getProperty(HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME).getString();
                }
            } catch (Exception e) {
                log.warn("Failed to load class configured by {} property: {}",
                        HstNodeTypes.COMPONENT_PROPERTY_PARAMETERSINFO_CLASSNAME, paramsInfoClassName, e);
            }

            return getParametersInfoAnnotation(componentClazz, paramsInfoClassName, classLoader);
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
    public static ParametersInfo getParametersInfoAnnotation(final String componentClazzName, final String parametersInfoClassName) {
        return getParametersInfoAnnotation(componentClazzName, parametersInfoClassName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentClazz} or create one from {@code parametersInfoClassName}
     * if specified.
     * @param componentClazzName component class name
     * @param parametersInfoClassName class name for <code>ParametersInfo</code> type
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(final String componentClazzName,
                                                             final String parametersInfoClassName,
                                                             final ClassLoader classLoader) {
        Class<?> componentClazz = null;

        if (componentClazzName != null && !componentClazzName.isEmpty()) {
            final HstModelRegistry hstModelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
            final ComponentManager componentManager = hstModelRegistry.getHstModel(classLoader).getComponentManager();
            final Object component = componentManager.getComponent(componentClazzName);
            if (component != null) {
                componentClazz = component.getClass();
            } else {
                try {
                    componentClazz = classLoader.loadClass(componentClazzName);
                } catch (Exception e) {
                    // parametersInfoClassName can also be defined as hst property without hst component class
                    log.info("Component class not loadable: {}", componentClazzName);
                }
            }
        }

        return getParametersInfoAnnotation(componentClazz, parametersInfoClassName, classLoader);
    }

    /**
     * Find the <code>ParametersInfo</code> annotation from the {@code componentClazz} or create one from {@code parametersInfoClassName}
     * if specified.
     * @param componentClazz component class
     * @param parametersInfoClassName class name for <code>ParametersInfo</code> type or {@code null} in which case the
     *                                componentClazz is scanned for the annotation {@link ParametersInfo}
     * @param classLoader the classLoader to load parametersInfoClassName
     * @return the type of <code>ParametersInfo</code>
     */
    public static ParametersInfo getParametersInfoAnnotation(final Class<?> componentClazz,
                                                             final String parametersInfoClassName,
                                                             final ClassLoader classLoader) {
        if (parametersInfoClassName != null && !parametersInfoClassName.isEmpty()) {
            try {
                final Class<?> paramsInfoType;
                if (classLoader == null) {
                    if (componentClazz == null) {
                        log.error("Cannot load paramsInfoType if componentClazz and classLoader are both null");
                        return null;
                    }
                    paramsInfoType = componentClazz.getClassLoader().loadClass(parametersInfoClassName);
                } else {
                    paramsInfoType = classLoader.loadClass(parametersInfoClassName);
                }

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
