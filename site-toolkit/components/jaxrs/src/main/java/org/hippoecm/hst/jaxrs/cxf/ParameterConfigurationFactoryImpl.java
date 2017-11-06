package org.hippoecm.hst.jaxrs.cxf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import org.apache.cxf.message.Message;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ParameterConfiguration;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * Default {@link ParameterConfigurationFactory} implementation which can read parameters from the underlying resolved
 * sitemap item or resolved mount for JAX-RS resource services annotated with {@link ParametersInfo}.
 */
public class ParameterConfigurationFactoryImpl implements ParameterConfigurationFactory {

    @Override
    public ParameterConfiguration create(Message message) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final ParameterConfiguration parameterConfiguration = new ResolvedParameterConfiguration(
                requestContext.getResolvedSiteMapItem(), requestContext.getResolvedMount());
        return parameterConfiguration;
    }

    /**
     * {@link ComponentConfiguration} implementation to read parameters from either the resolved sitemap item or resolved
     * mount.
     */
    protected static class ResolvedParameterConfiguration implements ParameterConfiguration {

        private final ResolvedSiteMapItem resolvedSiteMapItem;
        private ResolvedMount resolvedMount;
        private final Map<String, String> parameters;
        private final List<String> parameterNames;


        public ResolvedParameterConfiguration(
                final ResolvedSiteMapItem resolvedSiteMapItem, final ResolvedMount resolvedMount) {
            this.resolvedSiteMapItem = resolvedSiteMapItem;
            this.resolvedMount = resolvedMount;
            Map<String, String> modifiableParameters = new HashMap<>();
            if (resolvedSiteMapItem != null) {
                resolvedSiteMapItem.getParameters().forEach((k, v) -> {
                    modifiableParameters.put(k.toString(), v.toString());
                });
            } else if (resolvedMount != null) {
                resolvedMount.getMount().getParameters().forEach((k, v) -> {
                    modifiableParameters.put(k, v);
                });
            }
            parameters = Collections.unmodifiableMap(modifiableParameters);
            parameterNames = (parameters.isEmpty()) ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(parameters.keySet()));
        }

        @Override
        public List<String> getParameterNames() {
            return parameterNames;
        }

        @Override
        public String getParameter(String name, ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);
            return parameters.get(name);
        }

        @Override
        public Map<String, String> getParameters(ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);
            return parameters;
        }

        private void checkIfSameResolvedSiteMapItemArg(final ResolvedSiteMapItem resolvedSiteMapItemArg) {
            if (resolvedSiteMapItemArg != resolvedSiteMapItem) {
                // NOTE: o.h.hst.core.component.HstParameterInfoProxyFactoryImpl.ParameterInfoInvocationHandler.getParameterValue#(...)
                //       is supposed to pass HstRequestContext#getResolvedSiteMapItem() in this call.
                //       Therefore, if the internal resolvedSiteMapItem is different from the caller's hstResolvedSiteMapItem,
                //       it means an error or requires an explanation why for later potential use cases.
                //       So, let's throw an exception here.
                throw new IllegalArgumentException(
                        "The passed resolved sitemap item is different from the internal resolved sitemap item. "
                                + "Doesn't it use the same HstRequestContext instance?");
            }
        }

        @Override
        public String toString() {
            return "ResolvedParameterConfiguration{" +
                    "mount=" + resolvedMount.getMount()+
                    ",siteMapItem=" + resolvedSiteMapItem == null ? "null" : resolvedSiteMapItem.getHstSiteMapItem() +
                    ", parameters=" + parameters +
                    '}';
        }
    }
}
