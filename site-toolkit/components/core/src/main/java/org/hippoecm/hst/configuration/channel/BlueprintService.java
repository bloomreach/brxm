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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlueprintService implements Blueprint {

    final static Logger log = LoggerFactory.getLogger(BlueprintService.class);

    private final String id;
    private final String name;
    private final String description;
    private final String path;
    private final Map<HstPropertyDefinition, Object> defaultValues;
    private final Class<?> channelInfoClass;
    private final Map<String, HstPropertyDefinition> properties = new HashMap<String, HstPropertyDefinition>();

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

        Class clazz = null;
        if (bluePrint.hasProperty("hst:channelinfoclass")) {
            String className = bluePrint.getProperty("hst:channelinfoclass").getString();
            try {
                clazz = getClass().getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                log.error("Could not load ", e);
            }
        }
        channelInfoClass = clazz;

        if (bluePrint.hasNode("hst:defaultchannelinfo")) {
            defaultValues = ChannelPropertyMapper.loadProperties(bluePrint.getNode("hst:defaultchannelinfo"), getPropertyDefinitions());
        } else {
            defaultValues = Collections.emptyMap();
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
    public Class<?> getChannelInfoClass() {
        return channelInfoClass;
    }

    public List<HstPropertyDefinition> getPropertyDefinitions() {
        Class<?> channelInfoClass = getChannelInfoClass();
        if (channelInfoClass != null) {
            return ChannelInfoClassProcessor.getProperties(channelInfoClass);
        }
        return Collections.emptyList();
    }

    public Node getNode(final Session session) throws RepositoryException {
        return session.getNode(path);
    }

    public Map<String, Object> loadChannelProperties(Node mountNode) throws RepositoryException {
        List<HstPropertyDefinition> propertyDefinitions = getPropertyDefinitions();
        Map<HstPropertyDefinition, Object> properties = ChannelPropertyMapper.loadProperties(mountNode, propertyDefinitions);
        Map<String, Object> channelProperties = new HashMap<String, Object>();
        for (Map.Entry<HstPropertyDefinition, Object> entry : properties.entrySet()) {
            channelProperties.put(entry.getKey().getName(), entry.getValue());
        }
        return channelProperties;
    }

    public void saveChannelProperties(Node mountNode, Map<String, Object> properties) throws RepositoryException {
        ChannelPropertyMapper.saveProperties(mountNode, getPropertyDefinitions(), properties);
    }

}
