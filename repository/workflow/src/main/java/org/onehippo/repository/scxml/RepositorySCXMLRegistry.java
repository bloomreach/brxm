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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.stream.Location;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.scxml2.env.groovy.GroovyEvaluator;
import org.apache.commons.scxml2.env.groovy.GroovyExtendableScriptCache;
import org.apache.commons.scxml2.io.SCXMLReader;
import org.apache.commons.scxml2.io.SCXMLReader.Configuration;
import org.apache.commons.scxml2.model.Action;
import org.apache.commons.scxml2.model.CustomAction;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.SCXML;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RepositorySCXMLRegistry is a concrete implementation of {@link SCXMLRegistry} to provide repository based loading
 * of SCXML state machine definitions.
 * <p>
 * This implementation also provides caching and and cache refresh capabilities for loaded {@link SCXMLDefinition}
 * instances. Through the management of this registry by a {@link SCXMLRegistryModule}, this effectively means that
 * SCXMLDefinitions are cached in memory until either the module is reloaded or a node under the module configured
 * repository storage location is modified.
 * </p>
 * <p>
 * If a SCXML state machine definition is re-loaded from the repository, but fails to be successfully parsed and
 * instantiated errors will be logged and the previous instance of the SCXMLDefinition will be kept in cache and be used.
 * </p>
 * <p>
 * This implementation also instantiates a dedicated {@link GroovyEvaluator} to be used for each SCXML state machine.
 * </p>
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
            if (session.nodeExists(scxmlDefinitionsNodePath)) {
                scxmlDefsNode = session.getNode(scxmlDefinitionsNodePath);
            }
        } catch (RepositoryException e) {
            log.error("Failed to read SCXML definitions node.", e);
        }

        if (scxmlDefsNode == null) {
            log.error("SCXML Definitions Node doesn't exist at '{}'.", scxmlDefinitionsNodePath);
            return;
        }

        Map<String, SCXMLDefinition> newScxmlDefMap = new HashMap<>();

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

                    if (newScxmlDef == null && oldScxmlDef != null) {
                        // NOTE: The new SCXML instance has error(s) so it's null here.
                        //       Now, let put the old existing SCXML instance back into the map if there's any.
                        newScxmlDef = oldScxmlDef;
                        log.error("The existing SCXML definition was kept due to invalid SCXML. Id: '{}'.", scxmlDefId);
                    }

                    if (newScxmlDef != null) {
                        newScxmlDefMap.put(scxmlDefId, newScxmlDef);
                        log.debug("Registering SCXML definition. Id: '{}'.", scxmlDefId);
                    }
                }
            }

            scxmlDefMap = newScxmlDefMap;
        } catch (RepositoryException e) {
            log.warn("Failed to parse SCXML definition node.", e);
        }
    }

    private SCXMLDefinition readSCXMLDefinition(final String scxmlDefId, final Node scxmlDefNode) throws SCXMLException {
        String scxmlDefPath;
        String scxmlSource;
        final List<CustomAction> customActions = new ArrayList<>();
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
        final XMLReporter xmlReporter = new XMLReporterImpl(scxmlDefPath);
        Configuration configuration =
                new Configuration(xmlReporter, null, customActions);
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

    private static class CustomGroovyEvaluator extends GroovyEvaluator {

        private static final GroovyExtendableScriptCache.ParentClassLoaderFactory parentClassLoaderFactory = new GroovyExtendableScriptCache.ParentClassLoaderFactory() {

            @Override
            public ClassLoader getClassLoader() {
                return RepositorySCXMLRegistry.class.getClassLoader();
            }
        };

        private final GroovyExtendableScriptCache.CompilerConfigurationFactory compilerConfigurationFactory = new GroovyExtendableScriptCache.CompilerConfigurationFactory() {

            @Override
            public CompilerConfiguration getCompilerConfiguration() {
                if (compilerConfiguration == null) {
                    compilerConfiguration = new CompilerConfiguration();
                    // TODO: add AST transformations like for security/sandbox
                }
                return compilerConfiguration;
            }
        };

        private transient CompilerConfiguration compilerConfiguration;

        public CustomGroovyEvaluator() {
            super(true);
        }

        @Override
        protected GroovyExtendableScriptCache newScriptCache() {
            GroovyExtendableScriptCache scriptCache = super.newScriptCache();
            scriptCache.setParentClassLoaderFactory(parentClassLoaderFactory);
            scriptCache.setCompilerConfigurationFactory(compilerConfigurationFactory);
            return scriptCache;
        }
    }

    private static class SCXMLDefinitionImpl implements SCXMLDefinition {

        private final String id;
        private final String path;
        private final SCXML scxml;
        private final GroovyEvaluator evaluator;

        public SCXMLDefinitionImpl(final String id, final String path, final SCXML scxml) {
            this.id = id;
            this.path = path;
            this.scxml = scxml;
            this.evaluator = new CustomGroovyEvaluator();
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

        @Override
        public GroovyEvaluator getEvaluator() {
            return evaluator;
        }
    }
}
