/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.core.component;

import java.beans.PropertyEditor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.beanutils.ConvertUtils;
import org.hippoecm.hst.core.parameters.EmptyPropertyEditor;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {

    @Override
    public <T> T createParameterInfoProxy(ParametersInfo parametersInfo, ComponentConfiguration componentConfig,
            HstRequest request) {

        Class<?> parametersInfoType = parametersInfo.type();

        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }
       

        InvocationHandler parameterInfoHandler =  createHstParameterInfoInvocationHandler(componentConfig, request);
        
        T parametersInfoInterface = (T) Proxy.newProxyInstance(parametersInfoType.getClassLoader(),
                new Class[] { parametersInfoType }, parameterInfoHandler);
        return parametersInfoInterface;
    }
    
    /**
     * Override this method if a custom parameterInfoHandler is needed
     * @param componentConfig
     * @param request
     * @param parameterValueConverter
     * @return the {@link HstParameterInfoInvocationHandler} used in the created proxy to handle the invocations
     */
    protected InvocationHandler createHstParameterInfoInvocationHandler(final ComponentConfiguration componentConfig,final HstRequest request) {
        return new ParameterInfoInvocationHandler(componentConfig, request);
    }

    protected static class ParameterInfoInvocationHandler implements InvocationHandler {

        private final ComponentConfiguration componentConfig;
        private final HstRequest request;

        public ParameterInfoInvocationHandler(final ComponentConfiguration componentConfig,final HstRequest request) {
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
            if (parameterName == null || "".equals(parameterName)) {
                throw new IllegalArgumentException("The parameter name is empty.");
            }

            String parameterValue = getParameterValue(parameterName, componentConfig, request);
            
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

        public String getParameterValue (final String parameterName, final ComponentConfiguration config, final HstRequest req) {
            String prefixedParameterName = getPrefixedParameterName(parameterName, config, req);
            String parameterValue = config.getParameter(prefixedParameterName, req.getRequestContext().getResolvedSiteMapItem());
            if ((parameterValue == null || parameterValue.isEmpty()) && !parameterName.equals(prefixedParameterName)) {
                // fallback semantics should be the same as fallback to annotated value:
                // if prefixed value is null or empty then use the default value
                parameterValue = config.getParameter(parameterName, req.getRequestContext().getResolvedSiteMapItem());
            }
            return parameterValue;
        }
        
        /**
         * This method can be overridden by subclasses of the {@link ParameterInfoInvocationHandler} to return 
         * a prefixed value
         * @param parameterName the <code>parameterName</code> that can be prefixed
         * @param conf the <code>ComponentConfiguration</code>
         * @param req the <code>HstRequest</code> 
         * @return the parameterName from <code>parameterName</code> possibly prefixed by some value 
         */
        protected String getPrefixedParameterName(final String parameterName, final ComponentConfiguration conf, final HstRequest req) {
            return parameterName;
        }
    }
    
    public static final boolean isGetter(final Method method, final Object[] args) {
        if (args == null || args.length == 0) {
            final String methodName = method.getName();
            return methodName.startsWith("get") || methodName.startsWith("is");
        }
        return false;
    }

    public static final boolean isSetter(final Method method, final Object[] args) {
        return (args != null && args.length == 1) && method.getName().startsWith("set");
    }


}
