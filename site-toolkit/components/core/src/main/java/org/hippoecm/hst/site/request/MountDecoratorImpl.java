/*
 *  Copyright 2011 Hippo.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.internal.MountDecorator;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountDecoratorImpl implements MountDecorator {

    protected final static Logger log = LoggerFactory.getLogger(MountDecoratorImpl.class);
    @Override
    public Mount decorateMountAsPreview(Mount mount) {
        if(mount.isPreview()) {
            return mount;
        }
        return new MountAsPreviewDecorator(mount);
    }

    private class MountAsPreviewDecorator implements ContextualizableMount {

        private Mount delegatee;
        private Mount parentAsPreview;
        private Map<String, Mount> childAsPreview = new HashMap<String, Mount>();

        public MountAsPreviewDecorator(Mount delegatee) {
            this.delegatee = delegatee;
        }

        @Override
        public HstSite getHstSite() {
            if (delegatee.isPreview()) {
                return delegatee.getHstSite();
            }
            if (delegatee instanceof ContextualizableMount) {
                return ((ContextualizableMount) delegatee).getPreviewHstSite();
            } else {
                log.warn(
                       "Don't know how to get the preview of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'."
                         + "Return the current mount and not preview version. ", delegatee
                            .getMountPath(), delegatee.getClass().getName());
                return delegatee.getHstSite();
            }
        }

        @Override
        public String getMountPoint() {
            if (delegatee.isPreview()) {
                return delegatee.getMountPoint();
            }
            if (delegatee instanceof ContextualizableMount) {
                return ((ContextualizableMount) delegatee).getPreviewMountPoint();
            } else {
                log.warn("Don't know how to get the preview mountPoint of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'."
                           + "Return the value from the  current mount and not preview version. ",
                             delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getMountPoint();
            }
        }

        @Override
        public String getCanonicalContentPath() {
            if (delegatee.isPreview()) {
                return delegatee.getCanonicalContentPath();
            }
            if (delegatee instanceof ContextualizableMount) {
                return ((ContextualizableMount) delegatee).getPreviewCanonicalContentPath();
            } else {
                log.warn("Don't know how to get the preview canonical content path of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'."
                          + "Return the value from the current mount and not preview version. ",
                             delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getCanonicalContentPath();
            }

        }

        @Override
        public String getContentPath() {
            if (delegatee.isPreview()) {
                return delegatee.getContentPath();
            }
            if (delegatee instanceof ContextualizableMount) {
                return ((ContextualizableMount) delegatee).getPreviewContentPath();
            } else {
                log.warn("Don't know how to get the preview content path of a Mount that is not an instanceof MountService. Unable for mount '{}' of type '{}'."
                          + "Return the value from the  current mount and not preview version. ",
                             delegatee.getMountPath(), delegatee.getClass().getName());
                return delegatee.getContentPath();
            }
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
            parentAsPreview = new MountAsPreviewDecorator(delegatee.getParent());
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

            child = new MountAsPreviewDecorator(delegatee.getChildMount(name));
            childAsPreview.put(name, child);
            return child;
        }

        @Override
        public String getPreviewCanonicalContentPath() {
            return getCanonicalContentPath();
        }

        @Override
        public String getPreviewContentPath() {
            return getContentPath();
        }

        @Override
        public HstSite getPreviewHstSite() {
            return getHstSite();
        }

        @Override
        public String getPreviewMountPoint() {
            return getMountPoint();
        }
        
        @Override
        public String getChannelId() {
            return delegatee.getChannelId();
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

        @Override
        public <T extends ChannelInfo> T getChannelInfo() {
            return (T) delegatee.getChannelInfo();
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
        public String getEmbeddedMountPath() {
            return delegatee.getEmbeddedMountPath();
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
        public Set<String> getRoles() {
            return delegatee.getRoles();
        }

        @Override
        public String getScheme() {
            return delegatee.getScheme();
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

        

    }
}
