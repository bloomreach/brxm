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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
import org.hippoecm.hst.core.parameters.DefaultHstParameterValueConverter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.parameters.ParametersInfoProvider;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

@Provider
public class DefaultParametersInfoProviderContextProvider implements ContextProvider<ParametersInfoProvider> {

    private static final HstParameterValueConverter DEFAULT_HST_PARAMETER_VALUE_CONVERTER = new DefaultHstParameterValueConverter();

    @Override
    public ParametersInfoProvider createContext(Message message) {
        final OperationResourceInfo operationResourceInfo = message.getExchange().get(OperationResourceInfo.class);
        final Class<?> resourceCls = operationResourceInfo.getClassResourceInfo().getResourceClass();
        return new ParametersInfoProviderImpl(message, resourceCls.getAnnotation(ParametersInfo.class));
    }

    protected ComponentConfiguration getComponentConfiguration(final Message message) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final ComponentConfiguration componentConfig = new ResolvedSiteMapItemOrResolvedMountBasedComponentConfiguration(
                requestContext.getResolvedSiteMapItem(), requestContext.getResolvedMount());
        return componentConfig;
    }

    private class ParametersInfoProviderImpl implements ParametersInfoProvider {

        private final Message message;
        private final ParametersInfo paramsInfoAnno;

        public ParametersInfoProviderImpl(final Message message, final ParametersInfo paramsInfoAnno) {
            this.message = message;
            this.paramsInfoAnno = paramsInfoAnno;
        }

        @Override
        public <T> T getParametersInfo() {
            if (paramsInfoAnno == null) {
                return null;
            }

            final HstRequestContext requestContext = RequestContextProvider.get();
            final HstParameterInfoProxyFactory parameterInfoProxyFacotory = requestContext
                    .getParameterInfoProxyFactory();
            final ComponentConfiguration componentConfig = getComponentConfiguration(message);
            return parameterInfoProxyFacotory.createParameterInfoProxy(paramsInfoAnno, componentConfig,
                    requestContext.getServletRequest(), DEFAULT_HST_PARAMETER_VALUE_CONVERTER);
        }
    }

    private static class ResolvedSiteMapItemOrResolvedMountBasedComponentConfiguration
            implements ComponentConfiguration {

        private final ResolvedSiteMapItem resolvedSiteMapItem;
        private final ResolvedMount resolvedMount;
        private final List<String> parameterNames;

        public ResolvedSiteMapItemOrResolvedMountBasedComponentConfiguration(
                final ResolvedSiteMapItem resolvedSiteMapItem, final ResolvedMount resolvedMount) {
            this.resolvedSiteMapItem = resolvedSiteMapItem;
            this.resolvedMount = resolvedMount;

            Set<String> paramNames = new HashSet<>();

            if (resolvedSiteMapItem != null) {
                resolvedSiteMapItem.getParameters().keySet().forEach(k -> {
                    paramNames.add((String) k);
                });
            }

            if (resolvedMount != null) {
                resolvedMount.getMount().getParameters().keySet().forEach(k -> {
                    paramNames.add(k);
                });
            }

            parameterNames = (paramNames.isEmpty()) ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(paramNames));
        }

        @Override
        public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
            return hstResolvedSiteMapItem.getParameter(name);
        }

        @Override
        public List<String> getParameterNames() {
            return parameterNames;
        }

        @Override
        public Map<String, String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
            Map<String, String> parameters = new HashMap<String, String>();
            hstResolvedSiteMapItem.getParameters().forEach((k, v) -> {
                parameters.put((String) k, (String) v);
            });
            return parameters;
        }

        @Override
        public String getLocalParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
            return hstResolvedSiteMapItem.getLocalParameter(name);
        }

        @Override
        public Map<String, String> getLocalParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
            Map<String, String> parameters = new HashMap<String, String>();
            hstResolvedSiteMapItem.getLocalParameters().forEach((k, v) -> {
                parameters.put((String) k, (String) v);
            });
            return parameters;
        }

        @Override
        public Map<String, String> getRawParameters() {
            Map<String, String> parameters = new HashMap<String, String>();

            if (resolvedSiteMapItem != null) {
                resolvedSiteMapItem.getHstSiteMapItem().getParameters().forEach((k, v) -> {
                    parameters.put((String) k, (String) v);
                });
            }

            if (resolvedMount != null) {
                resolvedMount.getMount().getParameters().forEach((k, v) -> {
                    parameters.put(k, v);
                });
            }

            return parameters;
        }

        @Override
        public Map<String, String> getRawLocalParameters() {
            return getRawParameters();
        }

        @Override
        public String getRenderPath() {
            return null;
        }

        @Override
        public String getServeResourcePath() {
            return null;
        }

        @Override
        public String getCanonicalPath() {
            return null;
        }

        @Override
        public String getCanonicalIdentifier() {
            return null;
        }

        @Override
        public String getXType() {
            return null;
        }

        @Override
        public Type getComponentType() {
            return null;
        }

    }
}
