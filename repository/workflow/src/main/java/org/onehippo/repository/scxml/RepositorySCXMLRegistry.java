/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.PathResolver;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.io.SCXMLReader.Configuration;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.CustomAction;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RepositorySCXMLRegistry
 */
public class RepositorySCXMLRegistry implements SCXMLRegistry {

    private static Logger log = LoggerFactory.getLogger(RepositorySCXMLRegistry.class);

    private static final String SCXML_DEFINITIONS = "hipposcxml:definitions";
    private static final String SCXML_SOURCE = "hipposcxml:source";
    private static final String SCXML_ACTION_NAMESPACE = "hipposcxml:namespace";
    private static final String SCXML_ACTION_CLASSNAME = "hipposcxml:classname";
    private static final String SCXML_ACTION = "hipposcxml:action";

    private Map<String,SCXML> scxmlMap = Collections.emptyMap();

    private Session session;
    private String scxmlTypesPath;

    public RepositorySCXMLRegistry() {}

    synchronized protected Map<String, SCXML> loadMap(Map<String, SCXML> oldMap) {
        try {
            final Map<String, SCXML> newMap = new HashMap<>();

            final Node definitionsNode = session.getNode(scxmlTypesPath);
            for (final Node scxmlNode : new NodeIterable(definitionsNode.getNodes())) {
                final String scxmlName = scxmlNode.getName();
                final String scxmlPath = scxmlNode.getPath();
                String className = null;
                // saveguard / reuse old SCXML definition if something fails
                SCXML scxml = oldMap.get(scxmlName);
                try {
                    final String source = scxmlNode.getProperty(SCXML_SOURCE).getString();

                    if (StringUtils.isBlank(source)) {
                        scxml = new SCXML();
                    } else {
                        final List<CustomAction> actions = new ArrayList<>();
                        for (final Node actionNode : new NodeIterable(scxmlNode.getNodes())) {
                            if (actionNode.isNodeType(SCXML_ACTION)) {
                                String namespace = actionNode.getProperty(SCXML_ACTION_NAMESPACE).getString();
                                className = actionNode.getProperty(SCXML_ACTION_CLASSNAME).getString();
                                Class<? extends Action> actionClass = (Class<Action>) getClass().forName(className);
                                actions.add(new CustomAction(namespace, actionNode.getName(), actionClass));
                            }
                        }
                        XMLReporter reporter = null;
                        PathResolver pathResolver = null;
                        Configuration configuration = new Configuration(reporter, pathResolver, actions);
                        scxml = SCXMLReader.read(new StreamSource(new StringReader(source)), configuration);
                    }
                }
                catch (ClassNotFoundException e) {
                    log.warn("Failed to load custom SCXML action class "+className+". SCXML document {} not loaded", scxmlPath, e);
                }
                catch (ModelException e) {
                    log.warn("Failed to parse SCXML document {}. Document not loaded", scxmlPath, e);
                }
                catch (XMLStreamException e) {
                    log.warn("Failed to parse SCXML document {}. Document not loaded", scxmlPath, e);
                }
                catch (IOException e) {
                    log.warn("Failed to load SCXML document {}. Document not loaded", scxmlPath, e);
                }
                newMap.put(scxmlName, scxml);
            }
            return newMap;
        }
        catch (RepositoryException e) {
            log.warn("Unexpected error", e);
            return Collections.emptyMap();
        }
    }

    public void doConfigure(Node configRootNode) throws RepositoryException {
        this.session = configRootNode.getSession();
        Node typesNode = configRootNode.getNode(SCXML_DEFINITIONS);
        this.scxmlTypesPath = typesNode.getPath();
        if (!typesNode.getPrimaryNodeType().getName().equals(SCXML_DEFINITIONS)) {
            throw new IllegalStateException("SCXMLRegistry configuration node at path: "+scxmlTypesPath+" is not of required primary type: "+ SCXML_DEFINITIONS);
        }
        scxmlMap = loadMap(scxmlMap);
    }

    public void onConfigurationChange()
    {
        // TODO: very crude, should handle change events (override #onEvent()) for more fine-grained reloading of individual documents
        scxmlMap = loadMap(scxmlMap);
    }

    @Override
    public SCXML getSCXML(String name) {
        return scxmlMap.get(name);
    }

    public void destroy() {
        scxmlMap.clear();
        session = null;
    }
}
