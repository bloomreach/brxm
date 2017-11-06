/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.parameters.EmptyPropertyEditor;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.util.HstRequestUtils.isComponentRenderingPreviewRequest;

public class HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {

    private static final Logger log = LoggerFactory.getLogger(HstParameterInfoProxyFactoryImpl.class);

    public final static String TEMPLATE_PARAM_NAME = "org.hippoecm.hst.core.component.template";
    public final static TemplateParameterInfoHolder TEMPLATE_PARAMETER_INFO_HOLDER = new TemplateParameterInfoHolder();

    @ParametersInfo(type = TemplateParameterInfo.class)
    public static class TemplateParameterInfoHolder {
        public ParametersInfo getParametersInfo() {
            return this.getClass().getAnnotation(ParametersInfo.class);
        }
    }

    public interface TemplateParameterInfo {
        @Parameter(name = TEMPLATE_PARAM_NAME)
        public String getTemplateParameter();
    }

    /**
     * @deprecated Use {@link #createParameterInfoProxy(ParametersInfo, ParameterConfiguration, HttpServletRequest,
     * HstParameterValueConverter)} instead
     */
    @Deprecated
    @Override
    public <T> T createParameterInfoProxy(final ParametersInfo parametersInfo, final ComponentConfiguration componentConfig,
                                          final HstRequest request, final HstParameterValueConverter converter) {
        return createParameterInfoProxy(parametersInfo, componentConfig, (HttpServletRequest) request, converter);
    }

    @Override
    public <T> T createParameterInfoProxy(final ParametersInfo parametersInfo,final ParameterConfiguration parameterConfiguration,
            final HttpServletRequest request, final HstParameterValueConverter converter) {

        Class<?> parametersInfoType = parametersInfo.type();

        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }

        InvocationHandler parameterInfoHandler =  createHstParameterInfoInvocationHandler(parameterConfiguration, request, converter, parametersInfoType);

        @SuppressWarnings("unchecked")
        T parametersInfoInterface = (T) Proxy.newProxyInstance(parametersInfoType.getClassLoader(),
                new Class[] { parametersInfoType }, parameterInfoHandler);

        return parametersInfoInterface;
    }

    /**
     * Override this method if a custom parameterInfoHandler is needed
     * @param parameterConfiguration
     * @param request
     * @param converter
     * @param parametersInfoType
     * @return the {@link InvocationHandler} used in the created proxy to handle the invocations
     */
    protected InvocationHandler createHstParameterInfoInvocationHandler(final ParameterConfiguration parameterConfiguration,
                                                                        final HttpServletRequest request,
                                                                        final HstParameterValueConverter converter,
                                                                        final Class<?> parametersInfoType) {
        return new ParameterInfoInvocationHandler(parameterConfiguration, request, converter, parametersInfoType);
    }

    /**
     * This class has visibility 'protected' to enable reuse.
     */
    protected static class ParameterInfoInvocationHandler implements InvocationHandler {

        private final ParameterConfiguration parameterConfiguration;
        private final HttpServletRequest request;
        private final HstParameterValueConverter converter;
        private final Class<?> parametersInfoType;

        public ParameterInfoInvocationHandler(final ParameterConfiguration parameterConfiguration, final HttpServletRequest request,
                final HstParameterValueConverter converter,
                final Class<?> parametersInfoType) {
            this.parameterConfiguration = parameterConfiguration;
            this.request = request;
            this.converter = converter;
            this.parametersInfoType = parametersInfoType;
        }

        @Override
        public Object invoke(Object object, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            int argCount = (args == null ? 0 : args.length);

            if ("equals".equals(methodName) && argCount == 1) {
                return super.equals(args[0]);
            }

            if ("hashCode".equals(methodName) && argCount == 0) {
                return super.hashCode();
            }

            if ("toString".equals(methodName) && argCount == 0) {
                StringBuilder builder = new StringBuilder("ParameterInfoProxy [parametersInfoType=");
                builder.append(parametersInfoType.getName()).append(", configuration=").append(parameterConfiguration.toString()).append("]");
                return  builder.toString();
            }

            if (isSetter(method, args)) {
                throw new UnsupportedOperationException("Setter method (" + method.getName() + ") is not supported.");
            }

            if (!isGetter(method, args)) {
                return null;
            }

            Parameter parameterAnnotation = method.getAnnotation(Parameter.class);
            if (parameterAnnotation == null) {
                throw new IllegalArgumentException("Component " + parameterConfiguration.toString() + " uses ParametersInfo annotation, but "
                        + method.getDeclaringClass().getSimpleName() + "#" + method.getName() + " is not annotated with " + Parameter.class.getName());
            }

            String parameterName = parameterAnnotation.name();
            if (parameterName == null || "".equals(parameterName)) {
                throw new IllegalArgumentException("The parameter name is empty.");
            }

            String parameterValue = getParameterValue(parameterName, parameterConfiguration, request);
            String defaultValue = null;
            if (parameterValue == null || "".equals(parameterValue)) {
                // when the parameter value is null or an empty string we return the default value from the annotation
                defaultValue = parameterAnnotation.defaultValue();
                parameterValue = defaultValue;
            }
            if (parameterValue == null) {
                return null;
            }

            Class<? extends PropertyEditor> customEditorType = parameterAnnotation.customEditor();
            Class<?> returnType = method.getReturnType();

            if (customEditorType == null || customEditorType == EmptyPropertyEditor.class) {
                try {
                    return converter.convert(parameterValue, returnType);
                } catch (HstParameterValueConversionException e) {
                    log.warn("Could not convert '"+parameterValue+"' to returnType "+returnType.getName()+ ".. Try to return default value", e.toString());
                    if(defaultValue == null) {
                        log.warn("Could not convert '"+parameterValue+"' to returnType "+returnType.getName()+ " and there is no default value configured");
                        return null;
                    } else {
                        // if default value is incorrect, the runtime exception HstParameterValueConversionException is just thrown
                        return converter.convert(defaultValue, returnType);
                    }
                }
            } else {
                PropertyEditor customEditor = customEditorType.newInstance();
                customEditor.setAsText(parameterValue);
                return customEditor.getValue();
            }
        }

        private String getParameterValue(final String parameterName, final ParameterConfiguration parameterConfiguration, final HttpServletRequest req) {
            final HstRequestContext requestContext = RequestContextProvider.get();
            if (isComponentRenderingPreviewRequest(requestContext) && (request instanceof HstRequest)) {
                // POST parameters in case of component rendering preview request are namespace less
                Map<String, String []> namespaceLessParameters = ((HstRequest) request).getParameterMap("");
                String [] paramValues = namespaceLessParameters.get(parameterName);
                if (paramValues != null) {
                    log.debug("For parameterName '{}' returning value '{}' as the parameter was part of the request body.",
                            parameterName, paramValues[0]);
                    return paramValues[0];
                }
            }
            String prefixedParameterName = getPrefixedParameterName(parameterName, parameterConfiguration, req);
            String parameterValue = parameterConfiguration.getParameter(prefixedParameterName, requestContext.getResolvedSiteMapItem());
            if (parameterValue == null && !parameterName.equals(prefixedParameterName)) {
                // fallback semantics should be the same as fallback to annotated value:
                // if prefixed value is null or empty then use the default value
                parameterValue = parameterConfiguration.getParameter(parameterName, requestContext.getResolvedSiteMapItem());
            }
            log.debug("For prefixedParameterName '{}'  returning value '{}'", prefixedParameterName, parameterValue);
            return parameterValue;
        }

        /**
         * This method can be overridden by subclasses of the {@link ParameterInfoInvocationHandler} to return 
         * a prefixed value
         * @param parameterName the <code>parameterName</code> that can be prefixed
         * @param parameterConfiguration the <code>ParameterConfiguration</code>
         * @param req the <code>HstRequest</code> or <code>HttpServletRequest</code>
         * @return the parameterName from <code>parameterName</code> possibly prefixed by some value 
         */
        protected String getPrefixedParameterName(final String parameterName, final ParameterConfiguration parameterConfiguration, final HttpServletRequest req) {
            final HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute(ContainerConstants.RENDER_VARIANT) != null) {
                final String prefix = session.getAttribute(ContainerConstants.RENDER_VARIANT).toString();
                if (ContainerConstants.DEFAULT_PARAMETER_PREFIX.equals(prefix)) {
                    return parameterName;
                }
                final String prefixedParameterName = prefix + HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER + parameterName;
                if (parameterConfiguration.getParameterNames().contains(prefixedParameterName)) {
                    return prefixedParameterName;
                }
            }
            return parameterName;
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
