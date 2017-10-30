/*
 *  Copyright 2008-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterValueConversionException;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;

public class ParameterUtils {

    public static final String PARAMETERS_INFO_ATTRIBUTE = ParameterUtils.class.getName() + ".parametersInfo";

    /**
     * ISO8601 formatter for date-time without time zone.
     * The format used is <tt>yyyy-MM-dd'T'HH:mm:ss</tt>.
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * ISO8601 formatter for date without time zone.
     * The format used is <tt>yyyy-MM-dd</tt>.
     */
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * ISO8601 formatter for time without time zone.
     * The format used is <tt>'T'HH:mm:ss</tt>.
     */
    public static final String ISO_TIME_FORMAT = "'T'HH:mm:ss";

    /**
     * All supported date string formats.
     */
    private static final String [] ISO8601_DATETIME_PATTERNS = {
        ISO_DATETIME_FORMAT,
        ISO_DATE_FORMAT,
        ISO_TIME_FORMAT,
    };

    public static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new HstParameterValueConverter(){
        @Override
        public Object convert(String parameterValue, Class<?> returnType) {
            try {
                if (returnType == Date.class) {
                    if (StringUtils.isBlank(parameterValue)) {
                        return null;
                    }
                    return DateUtils.parseDate(parameterValue, ISO8601_DATETIME_PATTERNS);
                } else if (returnType == Calendar.class) {
                    if (StringUtils.isBlank(parameterValue)) {
                        return null;
                    }
                    final Date date = DateUtils.parseDate(parameterValue, ISO8601_DATETIME_PATTERNS);
                    final Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    return cal;
                }

                return ConvertUtils.convert(parameterValue, returnType);
            } catch (ParseException e) {
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
        ParametersInfo annotation = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(component, componentConfig);

        if (annotation == null) {
            return null; 
        }

        HstParameterInfoProxyFactory parameterInfoProxyFacotory = request.getRequestContext().getParameterInfoProxyFactory();
        parametersInfo =  (T) parameterInfoProxyFacotory.createParameterInfoProxy(annotation, componentConfig, request, DEFAULT_HST_PARAMETER_VALUE_CONVERTER);
        request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parametersInfo);

        return parametersInfo;
    }

    /**
     * Returns an annotation on a 'parameter method' in a parameters info class, i.e. a method that is annotated
     * with {@link @Parameter}.
     *
     * @param parametersInfo the parameters info class to analyze
     * @param parameterName the name of the parameter as returned by {@link Parameter#name()}
     * @param annotationClass the class of the annotation to find
     * @param <A> the annotation, or null if the annotation could not be found
     * @return
     */
    public static <A extends Annotation> A getParameterAnnotation(final ParametersInfo parametersInfo,
                                                                  final String parameterName,
                                                                  final Class<A> annotationClass) {
        if (parametersInfo == null || parameterName == null || annotationClass == null) {
            return null;
        }

        final Class<?> paramsInfoClass = parametersInfo.type();
        for (Method method: paramsInfoClass.getMethods()) {
            final Parameter parameter = method.getAnnotation(Parameter.class);
            if (parameter != null && parameter.name().equals(parameterName)) {
                return method.getAnnotation(annotationClass);
            }
        }

        return null;
    }
}
