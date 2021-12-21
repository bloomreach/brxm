/*
 *  Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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

import static org.hippoecm.hst.util.HstRequestUtils.isComponentRenderingPreviewRequest;

import java.beans.PropertyEditor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.EmptyPropertyEditor;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {

    private static final Logger log = LoggerFactory.getLogger(HstParameterInfoProxyFactoryImpl.class);

    public static final String TEMPLATE_PARAM_NAME = "org.hippoecm.hst.core.component.template";
    public static final TemplateParameterInfoHolder TEMPLATE_PARAMETER_INFO_HOLDER = new TemplateParameterInfoHolder();
    public static final String GET_RESIDUAL_PARAMETER_VALUES_METHOD = "getResidualParameterValues";
    public static final String GET_COMPONENT_PARAMETERS_METHOD = "getDynamicComponentParameters";

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

            if (GET_RESIDUAL_PARAMETER_VALUES_METHOD.equals(methodName)) {
                final HashMap<String, Object> componentParameters = new HashMap<>();
                if (parameterConfiguration instanceof ComponentConfiguration) {
                    final ComponentConfiguration componentConfiguration = (ComponentConfiguration) parameterConfiguration;
                    final List<DynamicParameter> dynamicComponentParameters = componentConfiguration
                            .getDynamicComponentParameters();
                    for (final DynamicParameter hstComponentParameter : dynamicComponentParameters) {
                        if (!hstComponentParameter.isResidual()
                                || isOverriddenParameter(hstComponentParameter, object)) {
                            continue;
                        }
                        String parameterValue = getParameterValue(hstComponentParameter.getName(),
                                parameterConfiguration, request);
                        if (StringUtils.isEmpty(parameterValue)) {
                            parameterValue = hstComponentParameter.getDefaultValue();
                        }

                        componentParameters.put(hstComponentParameter.getName(), converter.convert(hstComponentParameter.getName(), parameterValue, parameterConfiguration,
                                hstComponentParameter.getValueType().getDefaultReturnType()));
                    }
                }
                return Collections.unmodifiableMap(componentParameters);

            } else if (GET_COMPONENT_PARAMETERS_METHOD.equals(methodName)) {
                if (parameterConfiguration instanceof ComponentConfiguration) {
                    final ComponentConfiguration componentConfiguration = (ComponentConfiguration) parameterConfiguration;
                    return Collections.unmodifiableList(componentConfiguration.getDynamicComponentParameters());
                }
                return Collections.emptyList();
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

            //If the parameter is overridden by residual parameter, then return residual parameter value
            if (parameterConfiguration instanceof ComponentConfiguration) {
                final ComponentConfiguration componentConfiguration = (ComponentConfiguration) parameterConfiguration;
                final List<DynamicParameter> dynamicComponentParameters = componentConfiguration
                        .getDynamicComponentParameters();
                if (dynamicComponentParameters != null) {
                    final Optional<DynamicParameter> hstComponentParameter = dynamicComponentParameters.stream()
                            .filter(param -> param.isResidual() && param.getName().equals(parameterName)).findFirst();
                    if (hstComponentParameter.isPresent()) {
                        String parameterValue = getParameterValue(hstComponentParameter.get().getName(),
                                parameterConfiguration, request);
                        if (StringUtils.isEmpty(parameterValue)) {
                            parameterValue = hstComponentParameter.get().getDefaultValue();
                        }
                        return converter.convert(parameterName, parameterValue, parameterConfiguration,
                                method.getReturnType());
                    }
                }
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
                    return converter.convert(parameterName, parameterValue, parameterConfiguration, returnType);
                } catch (HstParameterValueConversionException e) {
                    log.warn("Could not convert '"+parameterValue+"' to returnType "+returnType.getName()+ ".. Try to return default value", e.toString());
                    if(defaultValue == null) {
                        log.warn("Could not convert '"+parameterValue+"' to returnType "+returnType.getName()+ " and there is no default value configured");
                        return null;
                    } else {
                        // if default value is incorrect, the runtime exception HstParameterValueConversionException is just thrown
                        return converter.convert(parameterName, defaultValue, parameterConfiguration, returnType);
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
            return parameterName;
        }

        private boolean isOverriddenParameter(final DynamicParameter hstComponentParameter, final Object object) {
            if (hstComponentParameter.isResidual()) {
                final String methodName = "get" + StringUtils.capitalize(hstComponentParameter.getName());
                return Arrays.stream(object.getClass().getMethods()).anyMatch(mt -> mt.getName().equals(methodName));
            }
            return false;
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
