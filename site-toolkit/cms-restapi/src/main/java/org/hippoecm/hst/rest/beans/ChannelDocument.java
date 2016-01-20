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
package org.hippoecm.hst.rest.beans;


import java.io.Serializable;

/**
 * Information about a document in a certain channel.
 */
public class ChannelDocument implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int HASH_SEED = 17;
    private static final int ODD_PRIME = 37;

    private String channelId;
    private String channelName;
    private String contextPath;
    private String pathInfo;
    private String mountPath;
    private String hostName;
    private String cmsPreviewPrefix;

    /**
     * Creates an empty channel document bean. This constructor is needed by the JAX-RS client framework.
     */
    public ChannelDocument() {
    }

    /**
     * Creates a copy of a channel document.
     *
     * @param original the channel document to copy.
     */
    public ChannelDocument(ChannelDocument original) {
        this.channelId = original.channelId;
        this.channelName = original.channelName;
        this.contextPath = original.contextPath;
        this.cmsPreviewPrefix = original.cmsPreviewPrefix;
        this.pathInfo = original.pathInfo;
        this.mountPath = original.mountPath;
        this.hostName = original.hostName;  
    }

    /**
     * @return the ID of the channel of this document
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * Sets the ID of the channel of this document.
     *
     * @param channelId the ID of the channel of this document
     */
    public void setChannelId(final String channelId) {
        this.channelId = channelId;
    }

    /**
     * @return the name of the channel of this document.
     */
    public String getChannelName() {
        return channelName;
    }

    /**
     * Sets the name of the channel of this document.
     *
     * @param channelName the name of the channel of this document
     */
    public void setChannelName(final String channelName) {
        this.channelName = channelName;
    }

    /**
     * @return Returns the contextpath of the URL 
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Sets the contextpath of the URL 
     *
     * @param contextPath 
     */
    public void setContextPath(final String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * @return the cmsPreviewPrefix to access the channels in the cms. The value can be EMPTY ("") but never <code>null</code>
     */
    public String getCmsPreviewPrefix() {
        return cmsPreviewPrefix;
    }

    public void setCmsPreviewPrefix(String cmsPreviewPrefix) {
        this.cmsPreviewPrefix = cmsPreviewPrefix;
    }

    /**
     * @return returns the pathInfo always starting with a slash or empty string
     */
    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ChannelDocument) {
            ChannelDocument d = (ChannelDocument)o;
            if(!equals(channelId, d.channelId)) {
                return false;
            }
            if(!equals(channelName, d.channelName)) {
                return false;
            }
            if(!equals(contextPath, d.contextPath)) {
                return false;
            }
            if(!equals(pathInfo, d.pathInfo)) {
                return false;
            }
            if(!equals(mountPath, d.mountPath)) {
                return false;
            }
            if(!equals(hostName, d.hostName)) {
                return false;
            }
            // all properties are equal, return true
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = HASH_SEED;
        if (channelId != null) {
            result = ODD_PRIME * result + channelId.hashCode();
        }
        if (channelName != null) { 
            result = ODD_PRIME * result + channelName.hashCode();
        }
        if (contextPath != null) { 
            result = ODD_PRIME * result + contextPath.hashCode();
        }
        if (pathInfo != null) { 
            result = ODD_PRIME * result + pathInfo.hashCode();
        }
        if (mountPath != null) { 
            result = ODD_PRIME * result + mountPath.hashCode();
        }
        if (channelName != null) { 
            result = ODD_PRIME * result + channelName.hashCode();
        }
        if (hostName != null) { 
            result = ODD_PRIME * result + hostName.hashCode();
        }
        return result;
    }
    
    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    @Override
    public String toString() {
        return "ChannelDocument [channelId=" + channelId + ", channelName=" + channelName + ", hostName=" + hostName
                + ", contextPath=" + contextPath + ", cmsPreviewPrefix=" + cmsPreviewPrefix + ", mountPath="
                + mountPath + ", pathInfo=" + pathInfo + "]";
    }
    
    
}
