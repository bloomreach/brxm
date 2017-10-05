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

            if (this.resolvedSiteMapItem != null) {
                this.resolvedSiteMapItem.getParameters().keySet().forEach(k -> {
                    paramNames.add((String) k);
                });
            } else if (this.resolvedMount != null) {
                this.resolvedMount.getMount().getParameters().keySet().forEach(k -> {
                    paramNames.add(k);
                });
            }

            parameterNames = (paramNames.isEmpty()) ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(paramNames));
        }

        @Override
        public List<String> getParameterNames() {
            return parameterNames;
        }

        @Override
        public String getParameter(String name, ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);

            if (resolvedSiteMapItem != null) {
                return resolvedSiteMapItem.getParameter(name);
            } else if (resolvedMount != null) {
                return resolvedMount.getMount().getParameter(name);
            }

            return null;
        }

        @Override
        public Map<String, String> getParameters(ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);

            Map<String, String> parameters = new HashMap<String, String>();

            if (resolvedSiteMapItem != null) {
                resolvedSiteMapItem.getParameters().forEach((k, v) -> {
                    parameters.put((String) k, (String) v);
                });
            } else if (resolvedMount != null) {
                resolvedMount.getMount().getParameters().forEach((k, v) -> {
                    parameters.put((String) k, (String) v);
                });
            }

            return parameters;
        }

        @Override
        public String getLocalParameter(String name, ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);

            if (resolvedSiteMapItem != null) {
                return resolvedSiteMapItem.getLocalParameter(name);
            } else if (resolvedMount != null) {
                return resolvedMount.getMount().getParameter(name);
            }

            return null;
        }

        @Override
        public Map<String, String> getLocalParameters(ResolvedSiteMapItem resolvedSiteMapItemArg) {
            // NOTE: the internal resolvedSiteMapItem member must be the same as the argument!
            checkIfSameResolvedSiteMapItemArg(resolvedSiteMapItemArg);

            Map<String, String> parameters = new HashMap<String, String>();

            if (resolvedSiteMapItem != null) {
                resolvedSiteMapItem.getLocalParameters().forEach((k, v) -> {
                    parameters.put((String) k, (String) v);
                });
            } else if (resolvedMount != null) {
                resolvedMount.getMount().getParameters().forEach((k, v) -> {
                    parameters.put((String) k, (String) v);
                });
            }

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

        private void checkIfSameResolvedSiteMapItemArg(final ResolvedSiteMapItem resolvedSiteMapItemArg) {
            if (resolvedSiteMapItemArg != resolvedSiteMapItem) {
                // NOTE: o.h.hst.core.component.HstParameterInfoProxyFactoryImpl.ParameterInfoInvocationHandler.getParameterValue#(...)
                //       is supposed to pass HstRequestContext#getResolvedSiteMapItem() in this call.
                //       Therefore, if the internal resolvedSiteMapItem is different from the caller's hstResolvedSiteMapItem,
                //       it means an error or requires an explanation why for later potential use cases.
                //       So, let's throw an exception here.
                throw new IllegalStateException(
                        "The passed resolved sitemap item is different from the internal resolved sitemap item. "
                                + "Doesn't it use the same HstRequestContext instance?");
            }
        }
    }
}
