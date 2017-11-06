/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.cxf;

import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.parameters.ParametersInfoProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;

/**
 * Default <code>ContextProvider&lt;ParametersInfoProvider&gt;</code> implementation providing parameter resolution
 * based on the resolved {@link ParameterConfiguration} via the {@link ParameterConfigurationFactory} property.
 */
@Provider
public class ParametersInfoProviderContextProvider implements ContextProvider<ParametersInfoProvider> {

    /**
     * Default component parameter value converter.
     */
    private static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new DefaultHstParameterValueConverter();

    /**
     * {@link ParameterConfigurationFactory} instance used when resolving a {@link ParameterConfiguration}.
     */
    private ParameterConfigurationFactory parameterConfigurationFactory;

    @Override
    public ParametersInfoProvider createContext(Message message) {
        final OperationResourceInfo operationResourceInfo = message.getExchange().get(OperationResourceInfo.class);
        final Class<?> resourceCls = operationResourceInfo.getClassResourceInfo().getResourceClass();

        if (!resourceCls.isAnnotationPresent(ParametersInfo.class)) {
            throw new RuntimeException(
                    "Cannot find org.hippoecm.hst.core.parameters.ParametersInfo annotation in the resource class: "
                            + resourceCls);
        }

        return new ParametersInfoProviderImpl(message, resourceCls.getAnnotation(ParametersInfo.class));
    }

    /**
     * Return the {@link ParameterConfigurationFactory} instance.
     * @return
     */
    public ParameterConfigurationFactory getParameterConfigurationFactory() {
        return parameterConfigurationFactory;
    }

    /**
     * Set the {@link ParameterConfigurationFactory} instance.
     * @param parameterConfigurationFactory
     */
    public void setParameterConfigurationFactory(ParameterConfigurationFactory parameterConfigurationFactory) {
        this.parameterConfigurationFactory = parameterConfigurationFactory;
    }

    /**
     * {@link ParametersInfoProvider} implementation which is passed to a JAX-RS Resource class through a parameter annotated
     * with <code>@Context</code> annotation.
     */
    private class ParametersInfoProviderImpl implements ParametersInfoProvider {

        /**
         * CXF message in the JAX-RS invocation context.
         */
        private final Message message;

        /**
         * {@link ParametersInfo} annotation instance.
         */
        private final ParametersInfo paramsInfoAnno;

        public ParametersInfoProviderImpl(final Message message, final ParametersInfo paramsInfoAnno) {
            this.message = message;
            this.paramsInfoAnno = paramsInfoAnno;
        }

        @Override
        public <T> T getParametersInfo() {
            final HstRequestContext requestContext = RequestContextProvider.get();
            final HstParameterInfoProxyFactory parameterInfoProxyFacotory = requestContext
                    .getParameterInfoProxyFactory();
            final ParameterConfiguration parameterConfig = getParameterConfigurationFactory().create(message);
            return parameterInfoProxyFacotory.createParameterInfoProxy(paramsInfoAnno, parameterConfig,
                    requestContext.getServletRequest(), DEFAULT_HST_PARAMETER_VALUE_CONVERTER);
        }
    }
}
