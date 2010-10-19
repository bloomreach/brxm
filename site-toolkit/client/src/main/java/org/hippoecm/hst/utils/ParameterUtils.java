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
import org.hippoecm.hst.configuration.components.EmptyPropertyEditor;
import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.proxy.ProxyFactory;

public class ParameterUtils {
    
    public static final String PARAMETERS_INFO_ATTRIBUTE = ParameterUtils.class.getName() + ".parametersInfo";
    
    /**
     * Returns a proxy ParametersInfo object for the component class which resolves parameter from HstComponentConfiguration : resolved means that possible property placeholders like
     * ${1} or ${year}, where the first refers to the first wildcard matcher in a resolved sitemap item, and the latter
     * to a resolved parameter in the resolved HstSiteMapItem
     * <P>
     * <EM>NOTE: Because the returned ParametersInfo proxy instance is bound to the current request,
     * you MUST NOT store the returned object in a member variable or session. You should retrieve that per request.</EM>
     * </P>
     * 
     * The parameter map used has inherited parameters from ancestor components, which have precedence over child components) 
     * 
     * @param name
     * @param request
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    @SuppressWarnings("unchecked")
    public static <T> T getParametersInfo(HstComponent component, final ComponentConfiguration componentConfig, final HstRequest request) {
        T parametersInfo = (T) request.getAttribute(PARAMETERS_INFO_ATTRIBUTE);
        
        if (parametersInfo != null) {
            return parametersInfo;
        }
        
        ParametersInfo anno = component.getClass().getAnnotation(ParametersInfo.class);
        
        if (anno == null) {
            throw new IllegalArgumentException("The component does not have ParametersInfo annotation.");
        }
        
        Class<?> parametersInfoType = anno.type();
        
        if (!parametersInfoType.isInterface()) {
            throw new IllegalArgumentException("The ParametersInfo annotation type must be an interface.");
        }
        
        ProxyFactory factory = new ProxyFactory();
        
        Invoker invoker = new Invoker() {
            
            public Object invoke(Object object, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();
                
                boolean isGetter = false;
                boolean isSetter = false;
                
                if (methodName.startsWith("get") && (args == null || args.length == 0)) {
                    isGetter = true;
                } else if (methodName.startsWith("is") && (args == null || args.length == 0)) {
                    isGetter = true;
                } else if (methodName.startsWith("set") && (args != null || args.length == 1)) {
                    isSetter = true;
                } else {
                    return null;
                }
                
                Parameter panno = method.getAnnotation(Parameter.class);
                String parameterName = panno.name();
                
                if (StringUtils.isBlank(parameterName)) {
                    throw new IllegalArgumentException("The parameter name is empty.");
                }
                
                Class<?> returnType = method.getReturnType();
                Class<? extends PropertyEditor> customEditorType = panno.customEditor();
                
                if (isSetter) {
                    throw new UnsupportedOperationException("Setter method is not supported.");
                } else if (isGetter) {
                    String parameterValue = componentConfig.getParameter(parameterName, request.getRequestContext().getResolvedSiteMapItem());
                    
                    if (parameterValue == null) {
                        parameterValue = panno.defaultValue();
                    }
                    
                    if (parameterValue == null) {
                        return null;
                    }
                    
                    if (customEditorType == null || customEditorType == EmptyPropertyEditor.class) {
                        return ConvertUtils.convert(parameterValue, returnType);
                    } else {
                        PropertyEditor customEditor = customEditorType.newInstance();
                        customEditor.setAsText(parameterValue);
                        return customEditor.getValue();
                    }
                }
                
                return null;
            }
        };
        
        parametersInfo = (T) factory.createInvokerProxy(invoker, new Class [] { parametersInfoType });
        
        request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parametersInfo);
        
        return parametersInfo;
    }
    
}
