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
package org.hippoecm.hst.pagecomposer.jaxrs.hosting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHost;
import org.hippoecm.hst.configuration.hosting.MutableVirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.HstConfigurationAugmenter;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CustomMountAndVirtualCmsHostAugmenter implements HstConfigurationAugmenter {

    private static final Logger log = LoggerFactory.getLogger(CustomMountAndVirtualCmsHostAugmenter.class);

    private static final String[] EMPTY_ARRAY = {};

    private String mountName;
    private String mountType;
    private String mountNamedPipeline;

    public void setMountName(String mountName) {
        this.mountName = mountName;
    }

    public void setMountNamedPipeline(String mountNamedPipeline) {
        this.mountNamedPipeline = mountNamedPipeline;
    }

    public void setMountType(final String mountType) {
        this.mountType = mountType;
    }

    /**
     * Below every mount of type hst:platformMount we add the mount {@code mountName} with type {@code mountType}
     * and {@code mountNamedPipeline}
     */
    @Override
    public void augment(final MutableVirtualHosts hosts) throws ContainerException {
        if (!validateState()) {
            return;
        }
        log.info("Trying to augment cms host custom mount '{}' of type '{}'and pipeline '{}'",
                new String[]{mountName, mountType, mountNamedPipeline});


        for (Map.Entry<String, Map<String, VirtualHost>> entry : hosts.getRootVirtualHostsByGroup().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            final String hostGroup = entry.getKey();

            final List<Mount> mountsByHostGroup = new ArrayList<>(hosts.getMountsByHostGroup(hostGroup));

            for (Mount mount : mountsByHostGroup) {
                if (mount.getParent() != null) {
                    log.debug("Only root mount will get the custom mount added");
                    continue;
                }
                if (mount.getMountPoint() != null) {
                    log.debug("Only root mount that do not have a mount point can be the cms entry points and need to be " +
                            "augmented.");
                    continue;
                }
                final Mount customMount = new CustomMount(mountName, mountType, mountNamedPipeline, mount);
                ((MutableMount) mount).addMount((MutableMount) customMount);
                log.info("Successfully augmented mount {} below platform {}", customMount, mount);

            }
        }
    }
    private boolean validateState() throws ContainerException {
        if (mountName == null || mountName.isEmpty()) {
            log.error("No mount name set for {}", this.getClass().getName());
            return false;
        }
        if (mountType == null || mountType.isEmpty()) {
            log.error("No mount type set for {}", this.getClass().getName());
            return false;
        }
        if (mountNamedPipeline == null || mountNamedPipeline.isEmpty()) {
            log.error("No mount named pipeline set for {}", this.getClass().getName());
            return false;
        }
        return true;
    }


    private final static String FAKE_NON_EXISTING_PATH = "/fakePath/" + UUID.randomUUID().toString();

    private class CustomMount implements MutableMount {

        private Mount parent;
        // just a unique alias
        private String alias = "randomAlias" + UUID.randomUUID().toString();
        private String identifier = "randomIdentifer" + UUID.randomUUID().toString();
        private String name;
        private String namedPipeline;
        private Map<String, Mount> childs = new HashMap<String, Mount>();
        private String mountPath;
        private String type;
        private List<String> types;

        /**
         * Creates only the custom mount
         *
         * @param name
         * @param namedPipeline
         * @param parent
         */
        public CustomMount(String name, String type, String namedPipeline, Mount parent) {
            this.name = name;
            this.namedPipeline = namedPipeline;
            this.parent = parent;
            this.type = type;
            types = Arrays.asList(type);
            mountPath = parent.getMountPath() + "/" + name;
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException {
            if (childs.containsKey(mount.getName())) {
                throw new IllegalArgumentException("Cannot add Mount with name '" + mount.getName() + "' because already exists for " + this.toString());
            }
            childs.put(mount.getName(), mount);
            ((MutableVirtualHosts) parent.getVirtualHost().getVirtualHosts()).addMount(this);
        }

        @Override
        public String getNamedPipeline() {
            return namedPipeline;
        }

        @Override
        public boolean isFinalPipeline() {
            return true;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAlias() {
            return alias;
        }

        @Override
        public boolean isMapped() {
            // must be false for CMS REST MOUNT
            return false;
        }

        @Override
        public Mount getParent() {
            return parent;
        }

        @Override
        public List<Mount> getChildMounts() {
            return Collections.unmodifiableList(new ArrayList<Mount>(childs.values()));
        }

        @Override
        public Mount getChildMount(String name) {
            return childs.get(name);
        }

        @Override
        public VirtualHost getVirtualHost() {
            return parent.getVirtualHost();
        }


        @Override
        public HstSite getHstSite() {
            // no hst site
            return null;
        }

        @Override
        public boolean isContextPathInUrl() {
            // must be false for CMS REST MOUNT
            // TODO what to do with this? We do not use _cmsrest any more
            return false;
        }

        @Override
        public boolean isPortInUrl() {
            // must be false for CMS REST MOUNT
            // TODO what to do with this? We do not use _cmsrest any more
            return false;
        }

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public String getContextPath() {
            return parent.getVirtualHost().getContextPath();
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public Set<String> getUsers() {
            return null;
        }

        @Override
        public boolean isSubjectBasedSession() {
            return false;
        }

        @Override
        public boolean isSessionStateful() {
            return false;
        }

        @Override
        public String getMountPoint() {
            return null;
        }

        @Override
        public boolean hasNoChannelInfo() {
            return true;
        }

        @Override
        public String getContentPath() {
            return FAKE_NON_EXISTING_PATH;
        }

        @Override
        public String getMountPath() {
            return mountPath;
        }

        @Override
        public String getHomePage() {
            return null;
        }


        @Override
        public String getPageNotFound() {
            return null;
        }

        @Override
        public String getScheme() {
            return "http";
        }

        @Override
        public String getHstLinkUrlPrefix() {
            return null;
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
            return HttpServletResponse.SC_OK;
        }

        @Override
        public boolean isPreview() {
            return false;
        }

        @Override
        public boolean isOfType(String type) {
            return getTypes().contains(type);
        }

        @Override
        public String getType() {
            // this has to be LIVE : the links that are created through the CMS REST MOUNT
            // need to be 'live' links. The CMS will decorate these live links to preview
            return type;
        }

        @Override
        public List<String> getTypes() {
            return types;
        }

        @Override
        public boolean isVersionInPreviewHeader() {
            return false;
        }


        @Override
        public String getLocale() {
            return null;
        }

        @Override
        public HstSiteMapMatcher getHstSiteMapMatcher() {
            return null;
        }

        @Override
        public String getFormLoginPage() {
            return null;
        }

        @Override
        public String getProperty(String name) {
            return null;
        }

        @Override
        public List<String> getPropertyNames() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getMountProperties() {
            return Collections.emptyMap();
        }

        @Override
        public String getParameter(String name) {
            return null;
        }

        @Override
        public Map<String, String> getParameters() {
            return Collections.emptyMap();
        }

        @Override
        public String getIdentifier() {
            return identifier;
        }

        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            return null;
        }

        @Override
        public Channel getChannel() {
            return null;
        }

        @Override
        public String[] getDefaultSiteMapItemHandlerIds() {
            return null;
        }

        @Override
        public boolean isCacheable() {
            return false;
        }

        @Override
        public String[] getDefaultResourceBundleIds() {
            return EMPTY_ARRAY;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("CustomMount [hostName=").append(parent.getVirtualHost().getHostName())
                    .append(", mountPath = ").append(mountPath).append("]");
            return builder.toString();
        }

        @Override
        public Map<String, String> getResponseHeaders() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isExplicit() {
            // auto-created mount so not explicit
            return false;
        }

        @Override
        public String getPageModelApi() {
            return null;
        }
    }

}
