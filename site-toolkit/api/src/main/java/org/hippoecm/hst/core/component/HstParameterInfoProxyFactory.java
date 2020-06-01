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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ParameterConfiguration;


/**
 * HstParameterInfoProxyFactory is a factory interface for creating a proxy for an interface that is referred to by a {@link ParametersInfo} annotation.
 * The {@link ParametersInfo} annotation is used to annotate {@link HstComponent} classes. The interface referred to by the {@link ParametersInfo} annotation
 * is returned as proxy by the {@link #createParameterInfoProxy(ParametersInfo, ComponentConfiguration, HstRequest, HstParameterValueConverter)} method. The getters in the interface that are
 * annotated with the {@link Parameter} annotation  are delegated through the proxy to the backing {@link ComponentConfiguration} 
 */

public interface HstParameterInfoProxyFactory {

    /**
     * Returns a proxy instance of the interface T. The proxy delegates the {@link Parameter} annotated getters in the interface T to the backing {@link ComponentConfiguration} parameters, 
     * thus to {@link ComponentConfiguration#getParameter(String, org.hippoecm.hst.core.request.ResolvedSiteMapItem)}
     * @param <T> proxy instance of the interface T
     * @param parametersInfo the ParametersInfo annotation of the {@link HstComponent}
     * @param componentConfig the backing {@link ComponentConfiguration} of the {@link HstComponent}
     * @param request the {@link HstRequest}
     * @param converter the HstParameterValueConverter that does the actual conversion
     * @return proxy instance of the interface T
     * @throws IllegalArgumentException if {@link ParametersInfo#type()} does not return an interface or when <code>parameterValueConverter</code> is <code>null</code>
     * @deprecated Use {@link #createParameterInfoProxy(ParametersInfo, ParameterConfiguration, HttpServletRequest, HstParameterValueConverter)} instead
     */
    @Deprecated
    <T> T createParameterInfoProxy(ParametersInfo parametersInfo, ComponentConfiguration componentConfig,
            HstRequest request, HstParameterValueConverter converter);

    /**
     * Returns a proxy instance of the interface T. The proxy delegates the {@link Parameter} annotated getters in the interface T to the backing {@link ComponentConfiguration} parameters, 
     * thus to {@link ComponentConfiguration#getParameter(String, org.hippoecm.hst.core.request.ResolvedSiteMapItem)}
     * @param <T> proxy instance of the interface T
     * @param parametersInfo the ParametersInfo annotation
     * @param parameterConfiguration the backing {@link ParameterConfiguration}
     * @param request the {@link HttpServletRequest}
     * @param converter the HstParameterValueConverter that does the actual conversion
     * @return proxy instance of the interface T
     * @throws IllegalArgumentException if {@link ParametersInfo#type()} does not return an interface or when <code>parameterValueConverter</code> is <code>null</code>
     */
    <T> T createParameterInfoProxy(ParametersInfo parametersInfo, ParameterConfiguration parameterConfiguration,
            HttpServletRequest request, HstParameterValueConverter converter);

}
