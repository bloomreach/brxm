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

import org.hippoecm.hst.configuration.channel.Blueprint;

/**
 * HippoFolderRepresentationDataset
 * @version $Id$
 */
@XmlRootElement(name = "dataset")
public class BlueprintDataset extends AbstractDataset {

    private List<Blueprint> blueprints;

    public BlueprintDataset() {
        super();
    }

    @XmlElementWrapper(name = "blueprints")
    @XmlElements(@XmlElement(name = "blueprint"))
    public List<Blueprint> getBlueprints() {
        return (blueprints != null) ? Collections.unmodifiableList(blueprints) : Collections.emptyList();
    }

    public void setBlueprints(List<Blueprint> blueprints) {
        this.blueprints = (blueprints != null) ? new LinkedList<>(blueprints) : null;
        setSize((blueprints != null) ? blueprints.size() : 0);
    }
}
