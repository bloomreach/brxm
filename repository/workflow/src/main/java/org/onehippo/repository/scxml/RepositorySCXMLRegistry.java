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
import javax.xml.stream.Location;
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

    private Map<String, SCXML> scxmlDefsMap;

    private Session session;
    private String scxmlTypesPath;

    private XMLReporter xmlReporter;
    private PathResolver pathResolver;

    public RepositorySCXMLRegistry() {
    }

    void reconfigure(Node configRootNode) throws RepositoryException {
        this.session = configRootNode.getSession();
        Node typesNode = configRootNode.getNode(SCXML_DEFINITIONS);
        this.scxmlTypesPath = typesNode.getPath();

        if (!typesNode.getPrimaryNodeType().getName().equals(SCXML_DEFINITIONS)) {
            throw new IllegalStateException("SCXMLRegistry configuration node at path: " + scxmlTypesPath + " is not of required primary type: " + SCXML_DEFINITIONS);
        }
    }

    @Override
    public void initialize() {
        xmlReporter = new XMLReporterImpl();
        pathResolver = new RepositoryPathResolver(scxmlTypesPath, null);

        refresh();
    }

    @Override
    public SCXML getSCXML(String id) {
        if (scxmlDefsMap != null) {
            return scxmlDefsMap.get(id);
        }

        return null;
    }

    @Override
    public void destroy() {
        if (scxmlDefsMap != null) {
            scxmlDefsMap.clear();
        }

        session = null;
    }

    void refresh() {
        if (scxmlDefsMap == null) {
            scxmlDefsMap = Collections.synchronizedMap(new HashMap<String, SCXML>());
        }

        Node scxmlDefsNode = null;

        try {
            String relScxmlDefsNodePath = StringUtils.removeStart(scxmlTypesPath, "/");

            if (session.getRootNode().hasNode(relScxmlDefsNodePath)) {
                scxmlDefsNode = session.getRootNode().getNode(relScxmlDefsNodePath);
            } else {
                log.error("SCXML Definitions Node doesn't exist at '{}'.", scxmlTypesPath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to read SCXML definitions node.", e);
        }

        if (scxmlDefsNode != null) {
            try {
                // first add or update
                if (scxmlDefsNode.hasNodes()) {
                    for (final Node scxmlDefNode : new NodeIterable(scxmlDefsNode.getNodes())) {
                        final String scxmlDefId = scxmlDefNode.getName();
                        SCXML newScxml = null;

                        try {
                            newScxml = readSCXML(scxmlDefNode);
                        } catch (SCXMLException e) {
                            log.error("Invalid SCXML at " + scxmlDefNode.getPath(), e);
                        }

                        if (newScxml != null && validateSCXML(newScxml)) {
                            SCXML oldScxml = scxmlDefsMap.put(scxmlDefId, newScxml);

                            if (oldScxml == null) {
                                log.info("SCXML definition added. Id: '{}'.", scxmlDefId);
                            } else {
                                log.info("SCXML definition updated. Id: '{}'.", scxmlDefId);
                            }
                        }
                    }
                }

                // remove if no definition node found
                synchronized (scxmlDefsMap) {
                    for (Map.Entry<String, SCXML> entry : scxmlDefsMap.entrySet()) {
                        String scxmlDefId = entry.getKey();

                        if (!scxmlDefsNode.hasNode(scxmlDefId)) {
                            scxmlDefsMap.remove(scxmlDefId);
                            log.info("SCXML definition removed. Id: '{}'.", scxmlDefId);
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.warn("Failed to parse SCXML definition node.", e);
            }
        }
    }

    private SCXML readSCXML(final Node scxmlDefNode) throws SCXMLException {
        String scxmlDefPath = null;
        String scxmlSource = null;
        final List<CustomAction> actions = new ArrayList<CustomAction>();
        String className = null;

        try {
            scxmlDefPath = scxmlDefNode.getPath();
            scxmlSource = scxmlDefNode.getProperty(SCXML_SOURCE).getString();

            if (StringUtils.isBlank(scxmlSource)) {
                log.error("SCXML definition source is blank at '{}'. '{}'.", scxmlDefNode.getPath(), scxmlSource);
            }

            for (final Node actionNode : new NodeIterable(scxmlDefNode.getNodes())) {
                if (!actionNode.isNodeType(SCXML_ACTION)) {
                    continue;
                }

                String namespace = actionNode.getProperty(SCXML_ACTION_NAMESPACE).getString();
                className = actionNode.getProperty(SCXML_ACTION_CLASSNAME).getString();
                @SuppressWarnings("unchecked")
                Class<? extends Action> actionClass = (Class<Action>) Thread.currentThread().getContextClassLoader().loadClass(className);
                actions.add(new CustomAction(namespace, actionNode.getName(), actionClass));
            }
        } catch (ClassNotFoundException e) {
            throw new SCXMLException("Failed to find the custom action class: " + className, e);
        } catch (RepositoryException e) {
            throw new SCXMLException("Failed to read custom actions from repository.", e);
        } catch (Exception e) {
            throw new SCXMLException("Failed to load custom actions.", e);
        }

        try {
            Configuration configuration = new Configuration(xmlReporter, pathResolver, actions);
            return SCXMLReader.read(new StreamSource(new StringReader(scxmlSource)), configuration);
        } catch (IOException e) {
            throw new SCXMLException("IO error while reading SCXML definition at '" + scxmlDefPath + "'.", e);
        } catch (ModelException e) {
            throw new SCXMLException("Invalid SCXML model definition at '" + scxmlDefPath + "'.", e);
        } catch (XMLStreamException e) {
            throw new SCXMLException("Failed to read SCXML XML stream at '" + scxmlDefPath + "'.", e);
        }
    }

    private boolean validateSCXML(final SCXML scxml) {
        return true;
    }

    private static class XMLReporterImpl implements XMLReporter {
        @Override
        public void report(String message, String errorType, Object relatedInformation, Location location)
                throws XMLStreamException {
            log.warn("SCXML parse error: [{}] {} {} ({}:{})", new Object [] { errorType, message, relatedInformation, location.getLineNumber(), location.getColumnNumber() });
        }
    }

    private static class RepositoryPathResolver implements PathResolver {

        private final String scxmlDefsPath;
        private final String relativeBasePath;

        private RepositoryPathResolver(final String scxmlDefsPath, String relativeBasePath) {
            this.scxmlDefsPath = scxmlDefsPath;
            this.relativeBasePath = relativeBasePath;
        }

        @Override
        public String resolvePath(String ctxPath) {
            StringBuilder sbTemp = new StringBuilder(scxmlDefsPath);

            if (StringUtils.isNotEmpty(relativeBasePath)) {
                sbTemp.append('/');
                sbTemp.append(StringUtils.removeStart(relativeBasePath, "/"));
            }

            if (StringUtils.isNotEmpty(ctxPath)) {
                sbTemp.append('/');
                sbTemp.append(StringUtils.removeStart(ctxPath, "/"));
            }

            return sbTemp.toString();
        }

        @Override
        public PathResolver getResolver(String ctxPath) {
            return new RepositoryPathResolver(scxmlDefsPath, ctxPath);
        }
    }

}
