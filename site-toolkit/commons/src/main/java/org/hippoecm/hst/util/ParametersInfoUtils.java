/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;

public class ParametersInfoUtils {

    private static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new DefaultHstParameterValueConverter();

    /**
     * Creates a proxy ParametersInfo object for the component class which resolves parameter from
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
     * @param request         the HttpServletRequest
     * @return the resolved parameter value for this name, or <code>null</null> if not present
     */
    public static <T> T createParametersInfo(HstComponent component, final ComponentConfiguration componentConfig,
            final HttpServletRequest request) {
        // first, try the new ParametersInfo annotation
        ParametersInfo annotation = ParametersInfoAnnotationUtils.getParametersInfoAnnotation(component,
                componentConfig);

        if (annotation == null) {
            return null;
        }

        HstParameterInfoProxyFactory parameterInfoProxyFacotory = RequestContextProvider.get()
                .getParameterInfoProxyFactory();
        T parametersInfo = parameterInfoProxyFacotory.createParameterInfoProxy(annotation, componentConfig, request,
                DEFAULT_HST_PARAMETER_VALUE_CONVERTER);

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
            final String parameterName, final Class<A> annotationClass) {
        if (parametersInfo == null || parameterName == null || annotationClass == null) {
            return null;
        }

        final Class<?> paramsInfoClass = parametersInfo.type();

        for (Method method : paramsInfoClass.getMethods()) {
            final Parameter parameter = method.getAnnotation(Parameter.class);

            if (parameter != null && parameter.name().equals(parameterName)) {
                return method.getAnnotation(annotationClass);
            }
        }

        return null;
    }
}
