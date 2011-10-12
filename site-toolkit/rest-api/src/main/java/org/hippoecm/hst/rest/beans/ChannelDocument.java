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
package org.hippoecm.hst.rest.beans;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Information about a document in a certain channel.
 */
@XmlRootElement(name = "channeldocument")
public class ChannelDocument implements Serializable {

    private static final int HASH_SEED = 17;
    private static final int ODD_PRIME = 37;

    private String channelId;
    private String channelName;
    private String canonicalUrl;
    private boolean urlContainsContextPath;

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
        this.channelId = original.getChannelId();
        this.channelName = original.getChannelName();
        this.canonicalUrl = original.getCanonicalUrl();
        this.urlContainsContextPath = original.getUrlContainsContextPath();
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
     * @return the canonical URL of this document in this channel.
     */
    public String getCanonicalUrl() {
        return canonicalUrl;
    }

    /**
     * Sets the canonical URL of this document in this channel.
     *
     * @param canonicalUrl the canonical URL of this document in this channel.
     */
    public void setCanonicalUrl(final String canonicalUrl) {
        this.canonicalUrl = canonicalUrl;
    }

    /**
     * Returns whether the canonical URL of this document in this channel contains a context path.
     *
     * @return true if the canonical URL of this document in this channel contains a context path, false otherwise.
     */
    public boolean getUrlContainsContextPath() {
        return urlContainsContextPath;
    }

    /**
     * Sets whether the canonical URL of this document in this channel contains a context path.
     *
     * @param urlContainsContextPath whether the canonical URL of this document in this channel contains a context path.
     */
    public void setUrlContainsContextPath(final boolean urlContainsContextPath) {
        this.urlContainsContextPath = urlContainsContextPath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ChannelDocument) {
            ChannelDocument d = (ChannelDocument)o;
            return channelId.equals(d.channelId) && canonicalUrl.equals(d.canonicalUrl);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = HASH_SEED;
        result = ODD_PRIME * result + channelId.hashCode();
        result = ODD_PRIME * result + canonicalUrl.hashCode();
        return result;
    }

}
