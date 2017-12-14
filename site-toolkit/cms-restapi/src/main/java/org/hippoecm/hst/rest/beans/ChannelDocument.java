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

    private String channelId;
    private String channelName;
    private String branchId;
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

    public void setBranchId(final String branchId) {
        this.branchId = branchId;
    }

    public String getBranchId() {
        return branchId;
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ChannelDocument that = (ChannelDocument) o;

        if (channelId != null ? !channelId.equals(that.channelId) : that.channelId != null) return false;
        if (channelName != null ? !channelName.equals(that.channelName) : that.channelName != null) return false;
        if (branchId != null ? !branchId.equals(that.branchId) : that.branchId != null) return false;
        if (contextPath != null ? !contextPath.equals(that.contextPath) : that.contextPath != null) return false;
        if (pathInfo != null ? !pathInfo.equals(that.pathInfo) : that.pathInfo != null) return false;
        if (mountPath != null ? !mountPath.equals(that.mountPath) : that.mountPath != null) return false;
        if (hostName != null ? !hostName.equals(that.hostName) : that.hostName != null) return false;
        return cmsPreviewPrefix != null ? cmsPreviewPrefix.equals(that.cmsPreviewPrefix) : that.cmsPreviewPrefix == null;
    }

    @Override
    public int hashCode() {
        int result = channelId != null ? channelId.hashCode() : 0;
        result = 31 * result + (channelName != null ? channelName.hashCode() : 0);
        result = 31 * result + (branchId != null ? branchId.hashCode() : 0);
        result = 31 * result + (contextPath != null ? contextPath.hashCode() : 0);
        result = 31 * result + (pathInfo != null ? pathInfo.hashCode() : 0);
        result = 31 * result + (mountPath != null ? mountPath.hashCode() : 0);
        result = 31 * result + (hostName != null ? hostName.hashCode() : 0);
        result = 31 * result + (cmsPreviewPrefix != null ? cmsPreviewPrefix.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ChannelDocument [channelId=" + channelId + ", channelName=" + channelName + ", branchId= " + branchId +
                ", hostName=" + hostName
                + ", contextPath=" + contextPath + ", cmsPreviewPrefix=" + cmsPreviewPrefix + ", mountPath="
                + mountPath + ", pathInfo=" + pathInfo + "]";
    }

}
