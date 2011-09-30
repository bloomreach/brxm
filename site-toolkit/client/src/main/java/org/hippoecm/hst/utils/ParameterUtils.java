/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.utils;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.proxy.Invoker;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.EmptyPropertyEditor;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.proxy.ProxyFactory;

public class ParameterUtils {

    public static final String PARAMETERS_INFO_ATTRIBUTE = ParameterUtils.class.getName() + ".parametersInfo";

    /**
     * Returns a proxy ParametersInfo object for the component class which resolves parameter from
     * HstComponentConfiguration : resolved means that possible property placeholders like ${1} or ${year}, where the
     * first refers to the first wildcard matcher in a resolved sitemap item, and the latter to a resolved parameter in
     * the resolved HstSiteMapItem
     * <p/>
     * <EM>NOTE: Because the returned ParametersInfo proxy instance is bound to the current request, you MUST NOT store
     * the returned object in a member variable or session. You should retrieve that per request.</EM> </P>
     * <p/>
     * The parameter map used has inherited parameters from ancestor components, which have precedence over child
     * components)
     *
     * @param component       the HST component with a ParameterInfo annotation
     * @param componentConfig the HST component configuration
     * @param request         the HST request
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    @SuppressWarnings("unchecked")
    public static <T> T getParametersInfo(HstComponent component, final ComponentConfiguration componentConfig, final HstRequest request) {
        T parametersInfo = (T) request.getAttribute(PARAMETERS_INFO_ATTRIBUTE);

        if (parametersInfo != null) {
            return parametersInfo;
        }

        Class<?> parametersInfoType;
        Invoker invoker;

        // first, try the new ParametersInfo annotation
        ParametersInfo annotation = component.getClass().getAnnotation(ParametersInfo.class);
        if (annotation != null) {
            parametersInfoType = annotation.type();

            if (!parametersInfoType.isInterface()) {
                throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
            }

            invoker = new ParameterInfoInvoker(componentConfig, request);
        } else {
            // second, try the old ParametersInfo annotation
            org.hippoecm.hst.configuration.components.ParametersInfo oldAnnotation = component.getClass().getAnnotation(org.hippoecm.hst.configuration.components.ParametersInfo.class);
            if (oldAnnotation == null) {
                throw new IllegalArgumentException("The component does not have a ParametersInfo annotation.");
            }

            parametersInfoType = oldAnnotation.type();

            if (!parametersInfoType.isInterface()) {
                throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
            }

            invoker = new DeprecatedParameterInfoInvoker(componentConfig, request);
        }

        ProxyFactory factory = new ProxyFactory();
        parametersInfo = (T) factory.createInvokerProxy(invoker, new Class[]{parametersInfoType});

        request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parametersInfo);

        return parametersInfo;
    }

    private static class ParameterInfoInvoker implements Invoker {

        private final ComponentConfiguration componentConfig;
        private final HstRequest request;

        ParameterInfoInvoker(final ComponentConfiguration componentConfig, HstRequest request) {
            this.componentConfig = componentConfig;
            this.request = request;
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            if (isSetter(method, args)) {
                throw new UnsupportedOperationException("Setter method (" + method.getName() + ") is not supported.");
            }
 
            if (!isGetter(method, args)) {
                return null;
            }

            Parameter parameterAnnotation = method.getAnnotation(Parameter.class);
            if (parameterAnnotation == null) {
                throw new IllegalArgumentException("Component " + componentConfig.getCanonicalPath() + " uses ParametersInfo annotation, but "
                        + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " is not annotated with " + Parameter.class.getName());
            }

            String parameterName = parameterAnnotation.name();
            if (StringUtils.isBlank(parameterName)) {
                throw new IllegalArgumentException("The parameter name is empty.");
            }

            String parameterValue = null; 
            // TODO make use of a profile here instead of this hard-coded logic.
            if(request.getSession(false) != null && request.getSession(false).getAttribute("persona") != null) {
                String parameterPrefix = request.getSession(false).getAttribute("persona") + "|org.hippoecm.hst:";
                parameterValue = componentConfig.getParameter(parameterPrefix+parameterName, request.getRequestContext().getResolvedSiteMapItem());
            }
            if(parameterValue == null) {
                parameterValue = componentConfig.getParameter(parameterName, request.getRequestContext().getResolvedSiteMapItem()); 
            }
            
            if (parameterValue == null || "".equals(parameterValue)) {
                // when the parameter value is null or an empty string we return the default value from the annotation
                parameterValue = parameterAnnotation.defaultValue();
            }
            if (parameterValue == null) {
                return null;
            }

            Class<? extends PropertyEditor> customEditorType = parameterAnnotation.customEditor();
            Class<?> returnType = method.getReturnType();

            if (customEditorType == null || customEditorType == EmptyPropertyEditor.class) {
                return ConvertUtils.convert(parameterValue, returnType);
            } else {
                PropertyEditor customEditor = customEditorType.newInstance();
                customEditor.setAsText(parameterValue);
                return customEditor.getValue();
            }
        }
    }

    /**
     * @deprecated
     */
    @Deprecated
    private static class DeprecatedParameterInfoInvoker implements Invoker {

        private final ComponentConfiguration componentConfig;
        private final HstRequest request;

        /**
         * @deprecated
         */
        @Deprecated
        DeprecatedParameterInfoInvoker(final ComponentConfiguration componentConfig, HstRequest request) {
            this.componentConfig = componentConfig;
            this.request = request;
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {
            if (isSetter(method, args)) {
                throw new UnsupportedOperationException("Setter method (" + method.getName() + ") is not supported.");
            }

            if (!isGetter(method, args)) {
                return null;
            }

            org.hippoecm.hst.configuration.components.Parameter parameterAnnotation = method.getAnnotation(org.hippoecm.hst.configuration.components.Parameter.class);
            if (parameterAnnotation == null) {
                throw new IllegalArgumentException("Component " + componentConfig.getCanonicalPath() + " used deprecated ParametersInfo annotation, but "
                        + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " is not annotated with " + org.hippoecm.hst.configuration.components.Parameter.class.getName());
            }

            String parameterName = parameterAnnotation.name();
            if (StringUtils.isBlank(parameterName)) {
                throw new IllegalArgumentException("The parameter name is empty.");
            }

            String parameterValue = componentConfig.getParameter(parameterName, request.getRequestContext().getResolvedSiteMapItem());
            if (parameterValue == null || "".equals(parameterValue)) {
                // when the parameter value is null or an empty string we return the default value from the annotation
                parameterValue = parameterAnnotation.defaultValue();
            }
            if (parameterValue == null) {
                return null;
            }

            Class<? extends PropertyEditor> customEditorType = parameterAnnotation.customEditor();
            Class<?> returnType = method.getReturnType();

            if (customEditorType == null || customEditorType == org.hippoecm.hst.configuration.components.EmptyPropertyEditor.class) {
                return ConvertUtils.convert(parameterValue, returnType);
            } else {
                PropertyEditor customEditor = customEditorType.newInstance();
                customEditor.setAsText(parameterValue);
                return customEditor.getValue();
            }
        }
    }

    private static final boolean isGetter(final Method method, final Object[] args) {
        if (args == null || args.length == 0) {
            final String methodName = method.getName();
            return methodName.startsWith("get") || methodName.startsWith("is");
        }
        return false;
    }

    private static final boolean isSetter(final Method method, final Object[] args) {
        return (args != null && args.length == 1) && method.getName().startsWith("set");
    }

}
