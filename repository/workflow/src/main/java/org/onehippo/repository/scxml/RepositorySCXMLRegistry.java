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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.ArrayUtils;
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

    private static final Pattern XML_STREAM_EXCEPTION_MESSAGE_PATTERN = Pattern.compile("Message:\\s(http://www.w3.org/TR/1999/REC-xml-names-19990114(\\S+))\\s?");

    private Map<String, SCXMLDefinition> scxmlDefMap;

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
    public SCXMLDefinition getSCXMLDefinition(String id) {
        if (scxmlDefMap != null) {
            return scxmlDefMap.get(id);
        }

        return null;
    }

    void destroy() {
        if (scxmlDefMap != null) {
            scxmlDefMap.clear();
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

        Map<String, SCXMLDefinition> newScxmlDefMap = new HashMap<String, SCXMLDefinition>();

        try {
            if (scxmlDefsNode.hasNodes()) {
                for (final Node scxmlDefNode : new NodeIterable(scxmlDefsNode.getNodes())) {
                    final String scxmlDefId = scxmlDefNode.getName();
                    // NOTE: in order to keep the existing SCXML instance in case the new SCXML definition has error(s),
                    //       find the existing old SCXML instance here to restore later if necessary.
                    SCXMLDefinition oldScxmlDef = (scxmlDefMap != null ? scxmlDefMap.get(scxmlDefId) : null);
                    SCXMLDefinition newScxmlDef = null;

                    try {
                        newScxmlDef = readSCXMLDefinition(scxmlDefId, scxmlDefNode);

                        if (!validateSemantics(newScxmlDef)) {
                            log.error("Invalid SCXML at '{}'. This will be ignored with keeping old one if exists.", scxmlDefNode.getPath());
                            newScxmlDef = null;
                        }
                    } catch (SCXMLException e) {
                        log.error("Invalid SCXML at " + scxmlDefNode.getPath(), e);
                    }

                    if (newScxmlDef == null) {
                        // NOTE: The new SCXML instance has error(s) so it's null here.
                        //       Now, let put the old existing SCXML instance back into the map if there's any.
                        newScxmlDef = oldScxmlDef;
                        log.info("The existing SCXML definition was kept due to invalid SCXML. Id: '{}'.", scxmlDefId);
                    }

                    if (newScxmlDef != null) {
                        newScxmlDefMap.put(scxmlDefId, newScxmlDef);
                        log.info("Registering SCXML definition. Id: '{}'.", scxmlDefId);
                    }
                }
            }

            scxmlDefMap = newScxmlDefMap;
        } catch (RepositoryException e) {
            log.warn("Failed to parse SCXML definition node.", e);
        }
    }

    private SCXMLDefinition readSCXMLDefinition(final String scxmlDefId, final Node scxmlDefNode) throws SCXMLException {
        String scxmlDefPath = null;
        String scxmlSource = null;
        final List<CustomAction> customActions = new ArrayList<CustomAction>();
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
                customActions.add(new CustomAction(namespace, actionNode.getName(), actionClass));
            }
        } catch (ClassNotFoundException e) {
            throw new SCXMLException("Failed to find the custom action class: " + className, e);
        } catch (RepositoryException e) {
            throw new SCXMLException("Failed to read custom actions from repository. " + e.getLocalizedMessage(), e);
        } catch (Exception e) {
            throw new SCXMLException("Failed to load custom actions. " + e.getLocalizedMessage(), e);
        }

        try {
            Configuration configuration = createSCXMLReaderConfiguration(scxmlDefPath, customActions);
            SCXML scxml = SCXMLReader.read(new StreamSource(new StringReader(scxmlSource)), configuration);
            return new SCXMLDefinitionImpl(scxmlDefId, scxmlDefPath, scxml);
        } catch (IOException e) {
            throw new SCXMLException("IO error while reading SCXML definition at '" + scxmlDefPath + "'. " + e.getLocalizedMessage(), e);
        } catch (ModelException e) {
            throw new SCXMLException("Invalid SCXML model definition at '" + scxmlDefPath + "'. " + e.getLocalizedMessage(), e);
        } catch (XMLStreamException e) {
            throw new SCXMLException("Failed to read SCXML XML stream at '" + scxmlDefPath + "'. " + naturalizeXMLStreamExceptionMessage(e), e);
        }
    }

    private Configuration createSCXMLReaderConfiguration(final String scxmlDefPath, final List<CustomAction> customActions) {
        final String factoryId = null;
        final ClassLoader classLoader = null;
        final XMLEventAllocator allocator = null;
        final Map<String, Object> properties = new HashMap<String, Object>();
        //properties.put(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
        final XMLResolver resolver = null;
        final XMLReporter xmlReporter = new XMLReporterImpl(scxmlDefPath);
        final String encoding = null;
        final String systemId = null;
        final boolean validate = false;
        // TODO: for now, no base url resolver, which works when src attribute is set to an absolute url.
        //       later, do we need to have a configuration for a base url to refer to other scxml definitions?
        final PathResolver pathResolver = new RepositoryURLResolver(null);
        final ClassLoader customActionClassLoader = null;
        final boolean useContextClassLoaderForCustomActions = true;

        Configuration configuration = 
                new Configuration(
                        factoryId, classLoader, allocator, 
                        properties, resolver, xmlReporter, 
                        encoding, systemId, validate, 
                        pathResolver, customActions, 
                        customActionClassLoader, useContextClassLoaderForCustomActions);
        configuration.setStrict(true);
        configuration.setSilent(false);

        return configuration;
    }

    private boolean validateSemantics(final SCXMLDefinition scxmlDef) {
        // TODO: Add more validation logic against Hippo specific semantics.
        return true;
    }

    private String naturalizeXMLStreamExceptionMessage(final XMLStreamException xse) {
        String naturalized = xse.getLocalizedMessage();
        final Matcher m = XML_STREAM_EXCEPTION_MESSAGE_PATTERN.matcher(naturalized);

        if (m.find()) {
            final String errorInfo = m.group(2);
            if (StringUtils.isNotEmpty(errorInfo)) {
                final String [] tokens = StringUtils.split(errorInfo, "#?&");
                if (!ArrayUtils.isEmpty(tokens)) {
                    final Location location = xse.getLocation();
                    final StringBuilder sbTemp =
                            new StringBuilder().append("XML Stream Error at (L")
                            .append(location.getLineNumber())
                            .append(":C").append(location.getColumnNumber())
                            .append("). Cause: ").append(StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(tokens[0]), " "));
                    if (tokens.length > 1) {
                        sbTemp.append(" (").append(StringUtils.join(tokens, ", ", 1, tokens.length)).append(")");
                    }
                    naturalized = sbTemp.toString();
                }
            }
        }

        return naturalized;
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

    private static class RepositoryURLResolver implements PathResolver {

        /** The base URL to resolve against. */
        private URL baseURL;

        /**
         * Constructor.
         *
         * @param baseURL The base URL to resolve against
         */
        public RepositoryURLResolver(final URL baseURL) {
            this.baseURL = baseURL;
        }

        /**
         * Uses URL(URL, String) constructor to combine URL's.
         * @see org.apache.commons.scxml2.PathResolver#resolvePath(java.lang.String)
         */
        public String resolvePath(final String ctxPath) {
            URL combined = null;

            try {
                if (baseURL != null) {
                    combined = new URL(baseURL, ctxPath);
                } else {
                    // try to resolve URL from the path directly.
                    combined = new URL(ctxPath);
                }
                return combined.toString();
            } catch (MalformedURLException e) {
                log.error("Malformed URL", e);
            }

            return null;
        }

        /**
         * @see org.apache.commons.scxml2.PathResolver#getResolver(java.lang.String)
         */
        public PathResolver getResolver(final String ctxPath) {
            URL combined = null;

            try {
                if (baseURL != null) {
                    combined = new URL(baseURL, ctxPath);
                } else {
                    // try to resolve URL from the path directly.
                    combined = new URL(ctxPath);
                }
                return new RepositoryURLResolver(combined);
            } catch (MalformedURLException e) {
                log.error("Malformed URL", e);
            }

            return null;
        }
    }

    private static class SCXMLDefinitionImpl implements SCXMLDefinition {

        private final String id;
        private final String path;
        private final SCXML scxml;

        public SCXMLDefinitionImpl(final String id, final String path, final SCXML scxml) {
            this.id = id;
            this.path = path;
            this.scxml = scxml;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public SCXML getSCXML() {
            return scxml;
        }
    }
}
