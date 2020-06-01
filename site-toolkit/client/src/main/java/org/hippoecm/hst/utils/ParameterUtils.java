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

import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.util.ParametersInfoUtils;

public class ParameterUtils {

    public static final String PARAMETERS_INFO_ATTRIBUTE = ParameterUtils.class.getName() + ".parametersInfo";

    /**
     * @deprecated since 5.1.0 : use {@link DefaultHstParameterValueConverter#ISO_DATETIME_FORMAT} instead
     */
    public static final String ISO_DATETIME_FORMAT = DefaultHstParameterValueConverter.ISO_DATETIME_FORMAT;

    /**
     * @deprecated since 5.1.0 : use {@link DefaultHstParameterValueConverter#ISO_DATE_FORMAT} instead
     */
    public static final String ISO_DATE_FORMAT = DefaultHstParameterValueConverter.ISO_DATE_FORMAT;

    /**
     * @deprecated since 5.1.0 : use {@link DefaultHstParameterValueConverter#ISO_TIME_FORMAT} instead
     */
    public static final String ISO_TIME_FORMAT = DefaultHstParameterValueConverter.ISO_TIME_FORMAT;

    public static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new DefaultHstParameterValueConverter();

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

        parametersInfo =  ParametersInfoUtils.createParametersInfo(component, componentConfig, request);

        if (parametersInfo != null) {
            request.setAttribute(PARAMETERS_INFO_ATTRIBUTE, parametersInfo);
        }

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
     * @deprecated Use {@link ParametersInfoUtils#getParameterAnnotation(ParametersInfo, String, Class)}.
     */
    @Deprecated
    public static <A extends Annotation> A getParameterAnnotation(final ParametersInfo parametersInfo,
                                                                  final String parameterName,
                                                                  final Class<A> annotationClass) {
        return ParametersInfoUtils.getParameterAnnotation(parametersInfo, parameterName, annotationClass);
    }
}
