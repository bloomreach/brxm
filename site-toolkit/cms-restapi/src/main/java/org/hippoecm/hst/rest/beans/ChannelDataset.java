/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.services.hst.Channel;

/**
 * HippoFolderRepresentationDataset
 * @version $Id$
 */
@XmlRootElement(name = "dataset")
public class ChannelDataset extends AbstractDataset {

    private List<Channel> channels;

    public ChannelDataset() {
        super();
    }

    @XmlElementWrapper(name = "channels")
    @XmlElements(@XmlElement(name = "channel"))
    public List<Channel> getChannels() {
        return (channels != null) ? channels : Collections.emptyList();
    }

    public void setChannels(List<Channel> channels) {
        this.channels = (channels != null) ? new LinkedList<>(channels) : null;
        setSize((channels != null) ? channels.size() : 0);
    }
}
