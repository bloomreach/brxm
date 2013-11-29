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

    static Logger log = LoggerFactory.getLogger(RepositorySCXMLRegistry.class);

    public static final String SCXML_DEFINITIONS = "hipposcxml:definitions";
    public static final String NT_SCXML = "hipposcxml:scxml";
    public static final String SCXML_SOURCE = "hipposcxml:source";
    public static final String SCXML_ACTION_NAMESPACE = "hipposcxml:namespace";
    public static final String SCXML_ACTION_CLASSNAME = "hipposcxml:classname";
    public static final String SCXML_ACTION = "hipposcxml:action";

    private Map<String, SCXML> scxmlMap;

    private Session session;
    private String scxmlDefinitionsNodePath;

    public RepositorySCXMLRegistry() {
    }

    void reconfigure(Node configRootNode) throws RepositoryException {
        this.session = configRootNode.getSession();
        Node scxmlDefinitionsNode = configRootNode.getNode(SCXML_DEFINITIONS);
        this.scxmlDefinitionsNodePath = scxmlDefinitionsNode.getPath();

        if (!scxmlDefinitionsNode.getPrimaryNodeType().getName().equals(SCXML_DEFINITIONS)) {
            throw new IllegalStateException("SCXMLRegistry configuration node at path: " + scxmlDefinitionsNodePath + " is not of required primary type: " + SCXML_DEFINITIONS);
        }
    }

    void initialize() {
        refresh();
    }

    @Override
    public SCXML getSCXML(String id) {
        if (scxmlMap != null) {
            return scxmlMap.get(id);
        }

        return null;
    }

    void destroy() {
        if (scxmlMap != null) {
            scxmlMap.clear();
        }

        session = null;
    }

    void refresh() {
        Node scxmlDefsNode = null;

        try {
            String relScxmlDefsNodePath = StringUtils.removeStart(scxmlDefinitionsNodePath, "/");

            if (session.getRootNode().hasNode(relScxmlDefsNodePath)) {
                scxmlDefsNode = session.getRootNode().getNode(relScxmlDefsNodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to read SCXML definitions node.", e);
        }

        if (scxmlDefsNode == null) {
            log.error("SCXML Definitions Node doesn't exist at '{}'.", scxmlDefinitionsNodePath);
            return;
        }

        Map<String, SCXML> newScxmlMap = new HashMap<String, SCXML>();

        try {
            if (scxmlDefsNode.hasNodes()) {
                for (final Node scxmlDefNode : new NodeIterable(scxmlDefsNode.getNodes())) {
                    final String scxmlDefId = scxmlDefNode.getName();
                    // NOTE: in order to keep the existing SCXML instance in case the new SCXML definition has error(s),
                    //       find the existing old SCXML instance here to restore later if necessary.
                    SCXML oldScxml = (scxmlMap != null ? scxmlMap.get(scxmlDefId) : null);
                    SCXML newScxml = null;

                    try {
                        newScxml = readSCXML(scxmlDefNode);

                        if (!validateSemantics(newScxml)) {
                            log.error("Invalid SCXML at '{}'. This will be ignored with keeping old one if exists.", scxmlDefNode.getPath());
                            newScxml = null;
                        }
                    } catch (SCXMLException e) {
                        log.error("Invalid SCXML at " + scxmlDefNode.getPath(), e);
                    }

                    if (newScxml == null) {
                        // NOTE: The new SCXML instance has error(s) so it's null here.
                        //       Now, let put the old existing SCXML instance back into the map if there's any.
                        newScxml = oldScxml;
                        log.info("The existing SCXML definition was kept due to invalid SCXML. Id: '{}'.", scxmlDefId);
                    }

                    if (newScxml != null) {
                        newScxmlMap.put(scxmlDefId, newScxml);
                        log.info("Registering SCXML definition. Id: '{}'.", scxmlDefId);
                    }
                }
            }

            scxmlMap = newScxmlMap;
        } catch (RepositoryException e) {
            log.warn("Failed to parse SCXML definition node.", e);
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
            XMLReporter xmlReporter = new XMLReporterImpl(scxmlDefPath);
            PathResolver pathResolver = new RepositoryPathResolver(scxmlDefinitionsNodePath, null);
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

    private boolean validateSemantics(final SCXML scxml) {
        // TODO: Add more validation logic against Hippo specific semantics.
        return true;
    }

    private static class XMLReporterImpl implements XMLReporter {

        private final String ctxPath;

        public XMLReporterImpl(final String ctxPath) {
            this.ctxPath = ctxPath;
        }

        @Override
        public void report(String message, String errorType, Object relatedInformation, Location location)
                throws XMLStreamException {
            // TODO: what's relatedInformation for?
            log.warn("SCXML model error in {} (L{}:C{}): [{}] {} {}", new Object [] { ctxPath, location.getLineNumber(), location.getColumnNumber(), errorType, message, relatedInformation });
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
