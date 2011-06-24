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
package org.hippoecm.hst.configuration.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class BlueprintService implements Blueprint {

    private final String id;
    private final String cmsPluginClass;
    private final String name;
    private final String description;
    private final String path;
    private final List<HstPropertyDefinition> properties;

    public BlueprintService(final Node bluePrint) throws RepositoryException {
        path = bluePrint.getPath();

        id = bluePrint.getName();

        if (bluePrint.hasProperty("hst:name")) {
            this.name = bluePrint.getProperty("hst:name").getString();
        } else {
            this.name = this.id;
        }

        if (bluePrint.hasProperty("hst:description")) {
            this.description = bluePrint.getProperty("hst:description").getString();
        } else {
            this.description = null;
        }

        if (bluePrint.hasProperty("hst:pluginclass")) {
            cmsPluginClass = bluePrint.getProperty("hst:pluginClass").getString();
        } else {
            cmsPluginClass = null;
        }

        if (bluePrint.hasNode("hst:channelproperties")) {
            properties = new ArrayList<HstPropertyDefinition>();
            Node channelProps = bluePrint.getNode("hst:channelproperties");
            for (PropertyIterator propIter = channelProps.getProperties(); propIter.hasNext();) {
                Property prop = propIter.nextProperty();
                if (prop.getDefinition().isProtected()) {
                    continue;
                }
                properties.add(new HstPropertyDefinitionService(prop, true));
            }
        } else {
            properties = null;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getCmsPluginClass() {
        return cmsPluginClass;
    }

    @Override
    public List<HstPropertyDefinition> getPropertyDefinitions() {
        return properties != null ? Collections.unmodifiableList(properties) : null;
    }

    public Node getNode(final Session session) throws RepositoryException {
        return session.getNode(path);
    }
}
