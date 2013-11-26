/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.model.ModelLoadingException;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountDecoratorImpl implements MountDecorator {

    protected final static Logger log = LoggerFactory.getLogger(MountDecoratorImpl.class);
    @Override
    public ContextualizableMount decorateMountAsPreview(ContextualizableMount mount) {
        if(mount.isPreview()) {
            return mount;
        }
        return new MountAsPreviewDecorator(mount);
    }

    private class MountAsPreviewDecorator implements ContextualizableMount {

        private ContextualizableMount delegatee;
        private Mount parentAsPreview;
        private Map<String, Mount> childAsPreview = new HashMap<String, Mount>();

        public MountAsPreviewDecorator(ContextualizableMount delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public HstSite getHstSite() {
            if (delegatee.isPreview()) {
                return delegatee.getHstSite();
            }
            return delegatee.getPreviewHstSite();
        }

        @Override
        public String getMountPoint() {
            return delegatee.getMountPoint();
        }

        @Override
        @Deprecated
        public String getCanonicalContentPath() {
            return delegatee.getContentPath();
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
            if (delegatee.isPreview()) {
                return delegatee.getParent();
            }
            if (delegatee.getParent() == null) {
                return null;
            }
            if(!(delegatee.getParent() instanceof ContextualizableMount)) {
                log.warn("Don't know how to get the preview parent Mount of mount '{}' because the parent Mount is not an instance of ContextualizableMount. Return null",
                          delegatee.getParent().getMountPath());
                return null;
            }
            parentAsPreview = new MountAsPreviewDecorator((ContextualizableMount)delegatee.getParent());
            return parentAsPreview;
        }

        @Override
        public Mount getChildMount(String name) {
            if (delegatee.isPreview()) {
                return delegatee.getChildMount(name);
            }
            Mount child = childAsPreview.get(name);
            if (child != null) {
                return child;
            }
            if (delegatee.getChildMount(name) == null) {
                return null;
            }
            if(!(delegatee.getChildMount(name) instanceof ContextualizableMount)) {
                log.warn("Don't know how to get the preview child Mount of mount '{}' because the parent Mount is not an instance of ContextualizableMount. Return null",
                        delegatee.getChildMount(name).getMountPath());
                return null;
            }
            child = new MountAsPreviewDecorator((ContextualizableMount)delegatee.getChildMount(name));
            childAsPreview.put(name, child);
            return child;
        }

        @Override
        public List<Mount> getChildMounts() {
            List<Mount> childMounts = delegatee.getChildMounts();
            List<Mount> previewChilds = new ArrayList<Mount>();
            for(Mount child : childMounts) {
                previewChilds.add(getChildMount(child.getName()));
            }
            return Collections.unmodifiableList(previewChilds);
        }

        @Override
        public HstSite getPreviewHstSite() {
            return getHstSite();
        }

        /*
         * below delegate everything to original mount
         */

        /*
         * NOTE For getType and getTypes the 'preview' version still returns the value the original mount had. So, for a live mount, it will still
         * be live. This is because otherwise cross mount links will fail (you cannot link from preview to live and vice versa).
         *
         * This means, implementation should always check isPreview() to check whether the mount is preview, and not isOfType("preview")
         */
        @Override
        public String getType() {
            return delegatee.getType();
        }

        @Override
        public List<String> getTypes() {
            return delegatee.getTypes();
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            return (T) delegatee.getChannelInfo();
        }

        @Override
        public String getChannelPath() {
            return delegatee.getChannelPath();
        }

        /**
         * @return the repository path to the channel configuration node and <code>null</code> if not configured
         */
        @Override
        public Channel getChannel() {
            return delegatee.getChannel();  //To change body of implemented methods use File | Settings | File Templates.
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


        @Deprecated
        @Override
        public String getDefaultResourceBundleId() {
            return delegatee.getDefaultResourceBundleId();
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
            return delegatee.getVirtualHost();
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
        public boolean isSite() {
            return delegatee.isSite();
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
        public String onlyForContextPath() {
            return delegatee.onlyForContextPath();
        }

        @Override
        public String getCmsLocation() {
            return delegatee.getCmsLocation();
        }

        @Override
        public void setChannelInfo(ChannelInfo info) {
            throw new UnsupportedOperationException("setChannelInfo not allowed on decorated mounts");
        }

        @Override
        public void setChannel(final Channel channel) throws UnsupportedOperationException {
            throw new UnsupportedOperationException("setChannel not allowed on decorated mounts");
        }

        @Override
        public void addMount(MutableMount mount) throws IllegalArgumentException, ModelLoadingException {
            throw  new UnsupportedOperationException("addMount not allowed on decorated mounts");
        }
    }
}
