/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterValueConversionException;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class ParameterUtils {

    public static final String PARAMETERS_INFO_ATTRIBUTE = ParameterUtils.class.getName() + ".parametersInfo";

    public static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new HstParameterValueConverter(){
        @Override
        public Object convert(String parameterValue, Class<?> returnType) {
            // ConvertUtils.convert cannot handle Calendar as returnType, however, we support it. 
            // that's why we first convert to Date
            try {
                if (returnType.equals(Calendar.class)) {
                    Date date = (Date) ConvertUtils.convert(parameterValue, Date.class);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal;
                }
                return ConvertUtils.convert(parameterValue, returnType);
            } catch (ConversionException e) {
                throw new HstParameterValueConversionException(e);
            }
        }
    };
    
    
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

          // first, try the new ParametersInfo annotation
        ParametersInfo annotation = component.getClass().getAnnotation(ParametersInfo.class);
        if (annotation == null) {
            return null; 
        }
        
        HstParameterInfoProxyFactory parameterInfoProxyFacotory = request.getRequestContext().getParameterInfoProxyFactory();
        parametersInfo =  (T) parameterInfoProxyFacotory.createParameterInfoProxy(annotation, componentConfig, request, DEFAULT_HST_PARAMETER_VALUE_CONVERTER);
        request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parametersInfo);
        return parametersInfo;            
        
    }
    
}
