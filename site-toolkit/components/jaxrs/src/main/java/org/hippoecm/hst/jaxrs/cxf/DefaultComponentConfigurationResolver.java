package org.hippoecm.hst.jaxrs.cxf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.message.Message;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

/**
 * Default {@link ComponentConfigurationResolver} implementation which can read parameters from the underlying resolved
 * sitemap item or resolved mount for JAX-RS resource services annotated with {@link ParametersInfo}.
 */
public class DefaultComponentConfigurationResolver implements ComponentConfigurationResolver {

    @Override
    public ComponentConfiguration resolve(Message message) {
        final HstRequestContext requestContext = RequestContextProvider.get();
        final ComponentConfiguration componentConfig = new ResolvedSiteMapItemOrResolvedMountBasedComponentConfiguration(
                requestContext.getResolvedSiteMapItem(), requestContext.getResolvedMount());
        return componentConfig;
    }

    /**
     * {@link ComponentConfiguration} implementation to read parameters from either the resolved sitemap item or resolved
     * mount.
     */
    protected static class ResolvedSiteMapItemOrResolvedMountBasedComponentConfiguration
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

        public String getParametersInfoClassName() {
            return null;
        }
    }
}
