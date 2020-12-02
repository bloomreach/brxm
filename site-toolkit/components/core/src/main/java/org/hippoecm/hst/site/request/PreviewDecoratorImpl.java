/*
 *  Copyright 2011-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.configuration.hosting.MatchException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.HstComponentRegistry;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.internal.PreviewDecorator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewDecoratorImpl implements PreviewDecorator {

    protected final static Logger log = LoggerFactory.getLogger(PreviewDecoratorImpl.class);
    @Override
    public Mount decorateMountAsPreview(final Mount mount) {
        if (mount instanceof PreviewDecoratedMount) {
            log.debug("Already preview decorated mount '{}'. Return", mount.toString());
            return mount;
        }
        if(mount.isPreview()) {
            log.debug("Mount {} is already a preview mount. Still decorate the backing virtualhosts to preview", mount.toString());
        }
        return new PreviewDecoratedMount(this, mount);
    }

    @Override
    public VirtualHosts decorateVirtualHostsAsPreview(final VirtualHosts virtualHosts) {
        if (virtualHosts instanceof PreviewDecoratedVirtualHosts) {
            log.debug("Already preview");
            return virtualHosts;
        }
        return new PreviewDecoratedVirtualHosts(this, virtualHosts);
    }

    @Override
    public VirtualHost decorateVirtualHostAsPreview(final VirtualHost virtualHost) {
        if (virtualHost instanceof PreviewDecoratedVirtualHost) {
            log.debug("Already preview");
            return virtualHost;
        }
        return new PreviewDecoratedVirtualHost(this, virtualHost);
    }


    private ResolvedMount decoratedResolvedMount(final ResolvedMount resolvedMount) {
        if (resolvedMount == null) {
            return null;
        }
        if (!(resolvedMount instanceof MutableResolvedMount)) {
            String msg = String.format("Resolved mount '%s' expected to be a MutableResolvedMount but was not.",
                    resolvedMount.getMount().toString());
            throw new IllegalStateException(msg);
        }
        ((MutableResolvedMount)resolvedMount).setMount(decorateMountAsPreview(resolvedMount.getMount()));
        return resolvedMount;
    }

    static class PreviewDecoratedMount implements Mount {

        private PreviewDecoratorImpl previewDecorator;
        private PreviewDecoratedVirtualHost previewDecoratedVirtualHost;
        private Mount delegatee;
        private Mount previewDecoratedParent;
        private Map<String, Mount> previewDecoratedChildren = new HashMap<>();

        public PreviewDecoratedMount(final PreviewDecoratorImpl previewDecorator, final Mount delegatee) {
            this.previewDecorator = previewDecorator;
            this.delegatee = delegatee;
        }

        @Override
        public HstSite getHstSite() {
            if (delegatee.isPreview() || !(delegatee instanceof ContextualizableMount)) {
                return delegatee.getHstSite();
            }
            return ((ContextualizableMount)delegatee).getPreviewHstSite();
        }

        @Override
        public String getMountPoint() {
            return delegatee.getMountPoint();
        }

        @Override
        public boolean hasNoChannelInfo() {
            return delegatee.hasNoChannelInfo();
        }

        @Override
        public String getContentPath() {
            return delegatee.getContentPath();
        }

        @Override
        public boolean isPreview() {
            return true;
        }

        @Override
        public Mount getParent() {
            if (delegatee.getParent() == null) {
                return null;
            }
            if (previewDecoratedParent != null) {
                return previewDecoratedParent;
            }
            previewDecoratedParent = previewDecorator.decorateMountAsPreview(delegatee.getParent());
            // set this decorated child as decorated child of the decorated parent, otherwise
            // decoratedChild.getParent().getChildMount("child") == decoratedChild does not hold
            ((PreviewDecoratedMount)previewDecoratedParent).previewDecoratedChildren.put(this.getName(), this);

            return previewDecoratedParent;
        }

        @Override
        public Mount getChildMount(String name) {
            Mount child = previewDecoratedChildren.get(name);
            if (child != null) {
                return child;
            }
            if (delegatee.getChildMount(name) == null) {
                return null;
            }
            child = previewDecorator.decorateMountAsPreview(delegatee.getChildMount(name));
            previewDecoratedChildren.put(name, child);
            return child;
        }

        @Override
        public List<Mount> getChildMounts() {
            List<Mount> childMounts = delegatee.getChildMounts();
            List<Mount> previewChildren = new ArrayList<>();
            for(Mount child : childMounts) {
                previewChildren.add(getChildMount(child.getName()));
            }
            return Collections.unmodifiableList(previewChildren);
        }

        @Override
        public String getType() {
            if (delegatee.isPreview()) {
                return delegatee.getType();
            }
            if (Mount.LIVE_NAME.equals(delegatee.getType())) {
                return Mount.PREVIEW_NAME;
            } else {
                // not 'live' : just return same type
                return delegatee.getType();
            }
        }

        @Override
        public List<String> getTypes() {
            if (delegatee.isPreview()) {
                return delegatee.getTypes();
            }
            // immutable list
            List<String> types = delegatee.getTypes();
            String decoratedType = getType();
            String unDecoratedType = delegatee.getType();
            // postfix all delegate types with '-preview'
            List<String> decoratedTypes = new ArrayList<>();
            for (String type : types) {
                if (type.equals(unDecoratedType)) {
                    decoratedTypes.add(decoratedType);
                } else {
                    decoratedTypes.add(type + "-preview");
                }
            }
            return Collections.unmodifiableList(decoratedTypes);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            if(delegatee instanceof ContextualizableMount) {
                return (T) ((ContextualizableMount)delegatee).getPreviewChannelInfo();
            }
            return delegatee.getChannelInfo();
        }

        /**
         * @return the repository path to the channel configuration node and <code>null</code> if not configured
         */
        @Override
        public Channel getChannel() {
            if(delegatee instanceof ContextualizableMount) {
                return  ((ContextualizableMount)delegatee).getPreviewChannel();
            }
            return delegatee.getChannel();
        }

        @Override
        public String getAlias() {
            return delegatee.getAlias();
        }

        @Override
        public String[] getDefaultSiteMapItemHandlerIds() {
            return delegatee.getDefaultSiteMapItemHandlerIds();
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

        @Override
        public String [] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public String getFormLoginPage() {
            return delegatee.getFormLoginPage();
        }

        @Override
        public String getHomePage() {
            return delegatee.getHomePage();
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return delegatee.getHstSiteMapMatcher();
        }

        @Override
        public String getIdentifier() {
            return delegatee.getIdentifier();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public String getMountPath() {
            return delegatee.getMountPath();
        }

        @Override
        public Map<String, String> getMountProperties() {
            return delegatee.getMountProperties();
        }

        @Override
        public String getName() {
            return delegatee.getName();
        }

        @Override
        public String getNamedPipeline() {
            return delegatee.getNamedPipeline();
        }

        @Override
        public boolean isFinalPipeline() {
            return delegatee.isFinalPipeline();
        }

        @Override
        public String getPageNotFound() {
            return delegatee.getPageNotFound();
        }

        @Override
        public int getPort() {
            return delegatee.getPort();
        }

        @Override
        public String getProperty(String name) {
            return delegatee.getProperty(name);
        }

        @Override
        public List<String> getPropertyNames() {
            return delegatee.getPropertyNames();
        }

        @Override
        public String getParameter(String name) {
            return delegatee.getParameter(name);
        }

        @Override
        public Map<String, String> getParameters() {
            return delegatee.getParameters();
        }

        @Override
        public Set<String> getRoles() {
            return delegatee.getRoles();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
        }

        @Override
        public String getHstLinkUrlPrefix() {
            return delegatee.getHstLinkUrlPrefix();
        }

        @Override
        public boolean isSchemeAgnostic() {
            return true;
        }

        @Override
        public boolean containsMultipleSchemes() {
            return false;
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            // TODO Test this, I think preview just return ok
            return HttpServletResponse.SC_OK;
        }

        @Override
        public Set<String> getUsers() {
            return delegatee.getUsers();
        }

        @Override
        public VirtualHost getVirtualHost() {
            // as long as there is a parent, get to the virtualhost via the parent since otherwise
            // child.getVirtualHost() == child.getParent().getVirtualHost() won't hold true
            final Mount decoratedParentMount = getParent();
            if (decoratedParentMount != null) {
                return decoratedParentMount.getVirtualHost();
            }

            if (previewDecoratedVirtualHost != null) {
                return previewDecoratedVirtualHost;
            }

            VirtualHost virtualHost = delegatee.getVirtualHost();

            previewDecoratedVirtualHost = new PreviewDecoratedVirtualHost(previewDecorator, virtualHost);

            // trigger fetching the preview decorated portmount on port 0 to attach current perview decorated mount to

            PortMount portMount = previewDecoratedVirtualHost.getPortMount(0);

            if (portMount instanceof PreviewDecoratedPortMount) {
                ((PreviewDecoratedPortMount)portMount).previewDecoratedRootMount = this;
            } else {
                // unexpected, log error to trace if it ever happens
                log.error("Unexpected type of preview decorated portmount for : {}", portMount);
            }

            return previewDecoratedVirtualHost;
        }

        @Override
        public boolean isAuthenticated() {
            return delegatee.isAuthenticated();
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public boolean isMapped() {
            return delegatee.isMapped();
        }

        @Override
        public boolean isOfType(String type) {
            return delegatee.isOfType(type);
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public boolean isSessionStateful() {
            return delegatee.isSessionStateful();
        }

        @Override
        public boolean isSubjectBasedSession() {
            return delegatee.isSubjectBasedSession();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return delegatee.isVersionInPreviewHeader();
        }

        @Override
        public String getContextPath() {
            return delegatee.getContextPath();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("MountAsPreviewDecorator for Mount [");
            builder.append(delegatee.toString());
            builder.append("]");
            return  builder.toString();
        }

        @Override
        public Map<String, String> getResponseHeaders() {
            return delegatee.getResponseHeaders();
        }

        @Override
        public boolean isExplicit() {
            // although you might argue that a preview decorated mount is an implicit mount, we need to know in the
            // channel mngr when we are dealing with an explicit configured Mount or not, hence we request the delegatee
            return delegatee.isExplicit();
        }

        @Override
        public String getPageModelApi() {
            return delegatee.getPageModelApi();
        }
    }

    static class PreviewDecoratedVirtualHost implements VirtualHost {

        private PreviewDecoratorImpl previewDecorator;
        private VirtualHost delegatee;
        private VirtualHosts previewDecoratedVirtualHosts;
        private Map<String, VirtualHost> previewDecoratedChildren = new HashMap<>();
        private PortMount previewDecoratedPortMount;


        private PreviewDecoratedVirtualHost(final PreviewDecoratorImpl previewDecorator, final VirtualHost delegatee) {
            this.previewDecorator = previewDecorator;
            this.delegatee = delegatee;
        }

        @Override
        public String getHostName() {
            return delegatee.getHostName();
        }

        @Override
        public String getName() {
            return delegatee.getName();
        }

        @Override
        public String getHostGroupName() {
            return delegatee.getHostGroupName();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public VirtualHost getChildHost(final String name) {
            VirtualHost child = delegatee.getChildHost(name);
            if (child == null) {
                return null;
            }
            if (previewDecoratedChildren.containsKey(name)) {
                return previewDecoratedChildren.get(name);
            }

            PreviewDecoratedVirtualHost previewDecoratedVirtualHost = new PreviewDecoratedVirtualHost(previewDecorator, child);
            previewDecoratedChildren.put(name, previewDecoratedVirtualHost);
            return previewDecoratedVirtualHost;
        }

        @Override
        public List<VirtualHost> getChildHosts() {
            final List<VirtualHost> childHosts = delegatee.getChildHosts();
            if (childHosts == null) {
                return null;
            }
            List<VirtualHost> decoratedChildren = new ArrayList<>();
            for (VirtualHost childHost : childHosts) {
                decoratedChildren.add(getChildHost(childHost.getName()));
            }
            return Collections.unmodifiableList(decoratedChildren);
        }


        @Override
        public PortMount getPortMount(final int portNumber) {
            PortMount portMount = delegatee.getPortMount(portNumber);
            if (portMount == null) {
                return null;
            }
            if (previewDecoratedPortMount != null) {
                return previewDecoratedPortMount;
            }
            previewDecoratedPortMount = new PreviewDecoratedPortMount(previewDecorator, portMount);
            return previewDecoratedPortMount;
        }

        @Override
        public VirtualHosts getVirtualHosts() {
            if (previewDecoratedVirtualHosts != null) {
                return previewDecoratedVirtualHosts;
            }
            previewDecoratedVirtualHosts = new PreviewDecoratedVirtualHosts(previewDecorator, delegatee.getVirtualHosts());
            return previewDecoratedVirtualHosts;
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public String getContextPath() {
            return delegatee.getContextPath();
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
        }

        @Override
        public boolean isSchemeAgnostic() {
            return delegatee.isSchemeAgnostic();
        }

        @Override
        public int getSchemeNotMatchingResponseCode() {
            return delegatee.getSchemeNotMatchingResponseCode();
        }

        @Override
        public String getHomePage() {
            return delegatee.getHomePage();
        }

        @Override
        public String getBaseURL(final HttpServletRequest request) {
            return delegatee.getBaseURL(request);
        }

        @Override
        public String getPageNotFound() {
            return delegatee.getPageNotFound();
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return delegatee.isVersionInPreviewHeader();
        }

        @Override
        public boolean isCacheable() {
            return delegatee.isCacheable();
        }

        @Override
        public String[] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public String getCdnHost() {
            return delegatee.getCdnHost();
        }

        @Override
        public String getHstLinkUrlPrefix() {
            return delegatee.getHstLinkUrlPrefix();
        }

        @Override
        public boolean isCustomHttpsSupported() {
            return delegatee.isCustomHttpsSupported();
        }

        @Override
        public Map<String, String> getResponseHeaders() {
            return delegatee.getResponseHeaders();
        }

        @Override
        public Collection<String> getAllowedOrigins() {
            return delegatee.getAllowedOrigins();
        }
    }

    static class PreviewDecoratedPortMount implements PortMount {

        private PreviewDecoratorImpl previewDecorator;
        private PortMount delegatee;
        private Mount previewDecoratedRootMount;

        private PreviewDecoratedPortMount(final PreviewDecoratorImpl previewDecorator, final PortMount delegatee) {
            this.previewDecorator = previewDecorator;
            this.delegatee = delegatee;
        }
        @Override
        public int getPortNumber() {
            return delegatee.getPortNumber();
        }

        @Override
        public Mount getRootMount() {
            if (previewDecoratedRootMount != null) {
                return previewDecoratedRootMount;
            }
            final Mount rootMount = delegatee.getRootMount();
            previewDecoratedRootMount = previewDecorator.decorateMountAsPreview(rootMount);
            return previewDecoratedRootMount;
        }
    }

    static class PreviewDecoratedVirtualHosts implements VirtualHosts {
        private PreviewDecoratorImpl previewDecorator;
        private VirtualHosts delegatee;
        private ResolvedMount previewDecoratedResolvedMount;
        private Map<String, Mount> previewDecoratedMounts = new HashMap<>();
        private ResolvedVirtualHost previewDecoratedResolvedVirtualHost;

        private PreviewDecoratedVirtualHosts(final PreviewDecoratorImpl previewDecorator, final VirtualHosts delegatee) {
            this.previewDecorator = previewDecorator;
            this.delegatee = delegatee;
        }

        @Override
        public boolean isHstFilterExcludedPath(final String pathInfo) {
            return delegatee.isHstFilterExcludedPath(pathInfo);
        }

        @Deprecated
        @Override
        public ResolvedMount matchMount(final String hostName, final String contextPath, final String requestPath) throws MatchException {
            return matchMount(hostName, requestPath);
        }

        @Override
        public ResolvedMount matchMount(final String hostName, final String requestPath) throws MatchException {
            if (previewDecoratedResolvedMount != null) {
                return previewDecoratedResolvedMount;
            }
            final ResolvedMount resolvedMount = delegatee.matchMount(hostName, requestPath);
            previewDecoratedResolvedMount = previewDecorator.decoratedResolvedMount(resolvedMount);
            return previewDecoratedResolvedMount;
        }

        @Override
        public ResolvedVirtualHost matchVirtualHost(final String hostName) throws MatchException {
            if (previewDecoratedResolvedVirtualHost != null) {
                return previewDecoratedResolvedVirtualHost;
            }
            final ResolvedVirtualHost undecorated = delegatee.matchVirtualHost(hostName);
            previewDecoratedResolvedVirtualHost = new PreviewDecoratedResolvedVirtualHost(previewDecorator, undecorated);
            return previewDecoratedResolvedVirtualHost;

        }

        @Override
        @Deprecated
        public String getDefaultHostName() {
            return delegatee.getDefaultHostName();
        }

        @Override
        public boolean isContextPathInUrl() {
            return delegatee.isContextPathInUrl();
        }

        @Override
        public String getContextPath() {
            return delegatee.getContextPath();
        }

        @Override
        public boolean isPortInUrl() {
            return delegatee.isPortInUrl();
        }

        @Override
        public String getLocale() {
            return delegatee.getLocale();
        }

        @Override
        public Mount getMountByGroupAliasAndType(final String hostGroupName, final String alias, final String type) {
            // check whether there is a 'live' variant. If so, return that one decorated as preview mount.
            final Mount liveMount = delegatee.getMountByGroupAliasAndType(hostGroupName, alias, Mount.LIVE_NAME);
            if (liveMount == null) {
                log.debug("No preview or live mount found for alias '{}' in host group '{}'. Return null",
                         alias, hostGroupName);
                return null;
            }
            log.debug("Found live mount '{}' for alias '{}' in host group '{}'. Return preview decorated version,",
                    liveMount, alias, hostGroupName);
            if (previewDecoratedMounts.containsKey(liveMount.getIdentifier())) {
                return previewDecoratedMounts.get(liveMount.getIdentifier());
            }
            final Mount decorateMountAsPreview = previewDecorator.decorateMountAsPreview(liveMount);
            previewDecoratedMounts.put(liveMount.getIdentifier(), decorateMountAsPreview);
            return decorateMountAsPreview;
        }

        @Override
        public List<Mount> getMountsByHostGroup(final String hostGroupName) {
            final List<Mount> mountsByHostGroup = delegatee.getMountsByHostGroup(hostGroupName);
            List<Mount> previewMounts = new ArrayList<>();
            for (Mount mount : mountsByHostGroup) {
                if (mount.isPreview() && RequestContextProvider.get().isChannelManagerPreviewRequest()) {
                    log.debug("Skipping *explicit* preview mounts for cms requests since they cannot be used in channel " +
                            "manager.");
                    continue;
                }
                if (previewDecoratedMounts.containsKey(mount.getIdentifier())) {
                    previewMounts.add(previewDecoratedMounts.get(mount.getIdentifier()));
                } else {
                    final Mount decorateMountAsPreview = previewDecorator.decorateMountAsPreview(mount);
                    previewDecoratedMounts.put(mount.getIdentifier(), decorateMountAsPreview);
                    previewMounts.add(decorateMountAsPreview);
                }
            }
            return previewMounts;
        }

        @Override
        public List<String> getHostGroupNames() {
            return delegatee.getHostGroupNames();
        }

        @Override
        public Mount getMountByIdentifier(final String uuid) {
            if (previewDecoratedMounts.containsKey(uuid)) {
                return previewDecoratedMounts.get(uuid);
            }
            final Mount mountByIdentifier = delegatee.getMountByIdentifier(uuid);
            if (mountByIdentifier == null) {
                log.info("Cannot find a mount for uuid '{}'. Most likely just removed.", uuid);
                return null;
            }
            final Mount decorateMountAsPreview = previewDecorator.decorateMountAsPreview(mountByIdentifier);
            previewDecoratedMounts.put(mountByIdentifier.getIdentifier(), decorateMountAsPreview);
            return decorateMountAsPreview;
        }


        // TODO always get the cms preview prefix via HstManager instead of via VirtualHosts model!!
        @Override
        public String getCmsPreviewPrefix() {
            return delegatee.getCmsPreviewPrefix();
        }

        @Override
        public String getChannelManagerSitesName() {
            return delegatee.getChannelManagerSitesName();
        }

        @Override
        public boolean isDiagnosticsEnabled(final String ip) {
            return delegatee.isDiagnosticsEnabled(ip);
        }

        @Override
        public int getDiagnosticsDepth() {
            return delegatee.getDiagnosticsDepth();
        }

        @Override
        public long getDiagnosticsThresholdMillis() {
            return delegatee.getDiagnosticsThresholdMillis();
        }

        @Override
        public long getDiagnosticsUnitThresholdMillis() {
            return delegatee.getDiagnosticsUnitThresholdMillis();
        }

        @Override
        public String[] getDefaultResourceBundleIds() {
            return delegatee.getDefaultResourceBundleIds();
        }

        @Override
        public boolean isChannelMngrSiteAuthenticationSkipped() {
            return delegatee.isChannelMngrSiteAuthenticationSkipped();
        }

        @Override
        public Map<String, Channel> getChannels(final String hostGroup) {
            return delegatee.getChannels(hostGroup);
        }

        @Override
        public Map<String, Map<String, Channel>> getChannels() {
            return delegatee.getChannels();
        }

        @Override
        public Channel getChannelByJcrPath(final String hostGroup, final String channelPath) {
            return delegatee.getChannelByJcrPath(hostGroup, channelPath);
        }

        @Override
        public Channel getChannelById(final String hostGroup, final String id) {
            return delegatee.getChannelById(hostGroup, id);
        }

        @Override
        public List<Blueprint> getBlueprints() {
            return delegatee.getBlueprints();
        }

        @Override
        public Blueprint getBlueprint(final String id) {
            return delegatee.getBlueprint(id);
        }

        @Override
        public Class<? extends ChannelInfo> getChannelInfoClass(final Channel channel) throws ChannelException {
            return delegatee.getChannelInfoClass(channel);
        }

        @Override
        public Class<? extends ChannelInfo> getChannelInfoClass(final String hostGroup, final String id) throws ChannelException {
            return delegatee.getChannelInfoClass(hostGroup, id);
        }

        @Override
        public <T extends ChannelInfo> T getChannelInfo(final Channel channel) throws ChannelException {
            return delegatee.getChannelInfo(channel);
        }

        @Override
        public List<Class<? extends ChannelInfo>> getChannelInfoMixins(Channel channel) throws ChannelException {
            return delegatee.getChannelInfoMixins(channel);
        }

        @Override
        public List<Class<? extends ChannelInfo>> getChannelInfoMixins(String hostGroup, String id)
                throws ChannelException {
            return delegatee.getChannelInfoMixins(hostGroup, id);
        }

        @Override
        public ResourceBundle getResourceBundle(final Channel channel, final Locale locale) {
            return delegatee.getResourceBundle(channel, locale);
        }

        @Override
        public List<HstPropertyDefinition> getPropertyDefinitions(final Channel channel) {
            return delegatee.getPropertyDefinitions(channel);
        }

        @Override
        public List<HstPropertyDefinition> getPropertyDefinitions(final String hostGroup, final String channelId) {
            return delegatee.getPropertyDefinitions(hostGroup, channelId);
        }

        @Override
        public HstComponentRegistry getComponentRegistry() {
            return delegatee.getComponentRegistry();
        }
    }

    private static class PreviewDecoratedResolvedVirtualHost implements ResolvedVirtualHost {

        private PreviewDecoratorImpl previewDecorator;
        private final ResolvedVirtualHost delegatee;
        private VirtualHost previewDecoratedVirtualHost;
        private Map<String, ResolvedMount> previewDecoratedResolvedMounts = new HashMap<>();

        public PreviewDecoratedResolvedVirtualHost(final PreviewDecoratorImpl previewDecorator, final ResolvedVirtualHost delegatee) {
            this.previewDecorator = previewDecorator;
            this.delegatee = delegatee;
        }

        @Override
        public VirtualHost getVirtualHost() {
            if (previewDecoratedVirtualHost != null ) {
                return previewDecoratedVirtualHost;
            }
            previewDecoratedVirtualHost = new PreviewDecoratedVirtualHost(previewDecorator, delegatee.getVirtualHost());
            return previewDecoratedVirtualHost;
        }

        @Deprecated
        @Override
        public ResolvedMount matchMount(final String contextPath, final String requestPath) throws MatchException {
            return matchMount(requestPath);
        }

        @Override
        public ResolvedMount matchMount(final String requestPath) throws MatchException {
            if (previewDecoratedResolvedMounts.containsKey(requestPath)) {
                return previewDecoratedResolvedMounts.get(requestPath);
            }
            final ResolvedMount resolvedMount = delegatee.matchMount(requestPath);
            ResolvedMount decoratedResolvedMount = previewDecorator.decoratedResolvedMount(resolvedMount);
            previewDecoratedResolvedMounts.put(requestPath, decoratedResolvedMount);
            return decoratedResolvedMount;
        }
    }


}
