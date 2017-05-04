/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.onehippo.cm.api.model.DefinitionNode;
import org.onehippo.cm.engine.Constants;
import org.onehippo.cm.engine.FileConfigurationWriter;
import org.onehippo.cm.engine.ModuleContext;
import org.onehippo.cm.impl.model.ConfigDefinitionImpl;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.DefinitionNodeImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Esv2Yaml {

    static final Logger log = LoggerFactory.getLogger(Esv2Yaml.class);

    private static final String HIPPOECM_EXTENSION_FILE = "hippoecm-extension.xml";
    private static final String TRANSLATIONS_ROOT_PATH = "/hippo:configuration/hippo:translations";
    private static final String SOURCE_FOLDER = "s";
    private static final String TARGET_FOLDER = "t";
    private static final String AGGREGATE = "a";
    private static final String ECM_LOCATION = "i";
    private static final String[] MAIN_YAML_NAMES = {"main", "root", "base", "index"};

    private final File src;
    private final File target;
    private final EsvParser esvParser;
    private final File extensionFile;
    private final ModuleImpl module;
    private final boolean aggregate;

    public static void main(final String[] args) throws IOException, EsvParseException, ParseException {

        final Options options = createCmdOptions();

        try {

            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(SOURCE_FOLDER) && cmd.hasOption(TARGET_FOLDER)) {
                final boolean aggregate = cmd.hasOption(AGGREGATE);
                final String src = cmd.getOptionValue(SOURCE_FOLDER);
                final String target = cmd.getOptionValue(TARGET_FOLDER);

                if (cmd.hasOption(ECM_LOCATION)) {
                    new Esv2Yaml(new File(cmd.getOptionValue(ECM_LOCATION)), new File(src), new File(target), aggregate).convert();
                } else {
                    new Esv2Yaml(new File(src), new File(target), aggregate).convert();
                }
            } else {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("esv2yaml", options);
            }
        } catch (Exception e) {
            Throwable t = e;
            if (e.getCause() != null) {
                t = e.getCause();
            }
            if (log.isDebugEnabled()) {
                log.error("Esv2Yaml.convert() failed: " + t.getMessage(), t);
            } else {
                log.error("Esv2Yaml.convert() failed: " + t.getMessage());
            }
            throw e;
        }
    }

    private static Options createCmdOptions() {
        Options options = new Options();
        options.addOption(ECM_LOCATION, "init", true, "(optional) location of hippoecm-extension.xml file, if not within <src> folder");
        options.addOption(SOURCE_FOLDER, "src", true, "bootstrap initialization resources folder");
        options.addOption(TARGET_FOLDER, "target", true, "directory for writing the repo-config (will be emptied first)");
        options.addOption(AGGREGATE, "aggregate", false, "Aggregate module");
        return options;
    }

    public Esv2Yaml(final File src, final File target, final boolean aggregate) throws IOException, EsvParseException {
        this(null, src, target, aggregate);
    }

    public Esv2Yaml(final File init, final File src, final File target, final boolean aggregate) throws IOException, EsvParseException {
        this.aggregate = aggregate;
        this.src = src;
        this.target = target;
        extensionFile = init != null ? new File(init, HIPPOECM_EXTENSION_FILE) : new File(src, HIPPOECM_EXTENSION_FILE);
        if (!extensionFile.exists() || !extensionFile.isFile()) {
            throw new IOException("File not found: " + extensionFile.getCanonicalPath());
        }
        if (init != null) {
            if (!src.exists() || !src.isDirectory()) {
                throw new IOException("bootstrap folder not found: " + src.getCanonicalPath());
            }
        }

        if (target.exists()) {
            if (target.isFile()) {
                throw new IllegalArgumentException("Target is not a directory");
            } else {
                final Path configFolder = Paths.get(target.toURI()).resolve(Constants.REPO_CONFIG_FOLDER);
                final Path contentFolder = Paths.get(target.toURI()).resolve(Constants.REPO_CONTENT_FOLDER);

                if (Files.exists(configFolder) && Files.isDirectory(configFolder)) {
                    FileUtils.forceDelete(configFolder.toFile());
                }

                if (Files.exists(contentFolder) && Files.isDirectory(contentFolder)) {
                    FileUtils.forceDelete(contentFolder.toFile());
                }
            }
        }
        esvParser = new EsvParser(src);
        module = new ConfigurationImpl("dummy").addProject("dummy").addModule("dummy");
    }

    public void convert() throws IOException, EsvParseException {
        log.info("Converting src: " + src.getCanonicalPath());

        final EsvNode rootNode = esvParser.parse(new FileInputStream(extensionFile), extensionFile.getCanonicalPath());
        if (rootNode != null) {
            if ("hippo:initialize".equals(rootNode.getName())) {

                // parse and create list of initializeitem instructions
                final List<InitializeInstruction> instructions = new ArrayList<>();
                final Set<String> initializeItemNames = new HashSet<>();
                for (EsvNode child : rootNode.getChildren()) {
                    if ("hippo:initializeitem".equals(child.getType())) {
                        // fail on duplicate initializeitem names, as that is not allowed/error anyway and could cause sorting error
                        if (!initializeItemNames.add(child.getName())) {
                            throw new EsvParseException("Duplicate hippo:initializeitem name: " + child.getName());
                        }
                        InitializeInstruction.parse(child, instructions);
                    } else {
                        log.warn("Ignored " + HIPPOECM_EXTENSION_FILE + " node: " + child.getName());
                    }
                }

                final Set<String> sourcePaths = new HashSet<>();

                // preprocess initializeitems and build set of claimed/needed target yaml source paths
                for (InitializeInstruction instruction : instructions) {
                    preprocessInitializeInstruction(instruction);
                    if (instruction instanceof SourceInitializeInstruction) {
                        sourcePaths.add(instruction.getSourcePath());
                    }
                }

                // now sort the instructions on processing order
                Collections.sort(instructions, InitializeInstruction.COMPARATOR);

                // for resourcebundles (.json) make sure to use/create a unique target yaml source path
                // e.g. to cater for a xyz.xml esv file + xyz.json -> xyz.yaml + xyz-1.yaml
                for (InitializeInstruction instruction : instructions) {
                    if (instruction.getType() == InitializeInstruction.Type.RESOURCEBUNDLES) {
                        final String candidate = FilenameUtils.removeExtension(instruction.getResourcePath());
                        instruction.setSourcePath(createSourcePath(new String[]{candidate}, sourcePaths, 0));
                        sourcePaths.add(instruction.getSourcePath());
                    }
                }

                // generate unique 'main.yaml' source, optionally using 'root.yaml', 'base.yaml' or 'index.yaml' as fallback
                final SourceImpl mainSource = module.addConfigSource(createSourcePath(MAIN_YAML_NAMES, sourcePaths, 0));  //TODO SS: review this. how to distinguish content from config definitions

                processInitializeInstructions(mainSource, instructions);
                serializeModule();
            } else {
                throw new EsvParseException(extensionFile.getCanonicalPath() +
                        " should have a root node with name \"hippo:initialize\"");
            }
            return;
        }
    }

    // create unique source path using candidate(s) name (without extension), appending sequence if needed
    protected String createSourcePath(final String[] candidates, final Set<String> sourcePaths, final int sequence) {
        String sourcePath = null;
        for (String name : candidates) {
            name = name + (sequence == 0 ? "" : "-" + sequence) + ".yaml";
            if (!sourcePaths.contains(name)) {
                sourcePath = name;
                break;
            }
        }
        if (sourcePath == null) {
            return createSourcePath(candidates, sourcePaths, sequence + 1);
        }
        return sourcePath;
    }

    // preprocess instruction, resolving resources, validating namespace uris, and pre-processing esv resources and content paths
    protected void preprocessInitializeInstruction(final InitializeInstruction instruction) throws IOException, EsvParseException {
        switch (instruction.getType()) {
            case NAMESPACE:
                // validate having a URI value
                String uriValue = instruction.getTypePropertyValue();
                try {
                    new URI(uriValue);
                } catch (URISyntaxException e) {
                    throw new EsvParseException("Invalid namespace uri: " + uriValue + " for initialize item: " + instruction.getName());
                }
                break;
            case NODETYPESRESOURCE:
                instruction.prepareResource(src, true);
                break;
            case CONTENTDELETE:
            case CONTENTPROPDELETE:
                instruction.setContentPath(instruction.getTypePropertyValue());
                break;
            case CONTENTRESOURCE:
                ((SourceInitializeInstruction) instruction).prepareSource(esvParser);
                break;
            case CONTENTPROPADD:
                // ensure/force multiple by definition
                instruction.getTypeProperty().setMultiple(true);
            case CONTENTPROPSET:
                instruction.setContentPath(instruction.getPropertyValue("hippo:contentroot", PropertyType.STRING, true));
                break;
            case WEBFILEBUNDLE:
                instruction.prepareResource(src, false);
                break;
            case RESOURCEBUNDLES:
                instruction.prepareResource(src, true);
                instruction.setContentPath(TRANSLATIONS_ROOT_PATH);
                break;
        }
    }

    // generate and merge instruction definitions which already have been sorted in processing order
    protected void processInitializeInstructions(final SourceImpl mainSource, final List<InitializeInstruction> instructions)
            throws IOException, EsvParseException {

        Set<String> resourceBundles = new HashSet<>();
        Set<DefinitionNode> deltaNodes = new HashSet<>();
        Map<String, DefinitionNodeImpl> nodeDefinitions = new HashMap<>();

        // not yet 'added' definition for resourcebundle root translation parent definitions, if needed
        final ConfigDefinitionImpl resourceBundleParents = new ConfigDefinitionImpl(mainSource);
        resourceBundleParents.setNode(null);

        for (InitializeInstruction instruction : instructions) {

            log.info(instruction.getType().getPropertyName() + ": " + instruction.getName());

            switch (instruction.getType()) {
                case NAMESPACE:
                    try {
                        mainSource.addNamespaceDefinition(instruction.getName(), new URI(instruction.getTypePropertyValue()));
                    } catch (URISyntaxException e) {
                        // already checked
                    }
                    break;
                case NODETYPESRESOURCE:
                    mainSource.addNodeTypeDefinition(instruction.getResourcePath(), true);
                    break;
                case CONTENTDELETE:
                case CONTENTPROPDELETE:
                case CONTENTPROPSET:
                case CONTENTPROPADD:
                    ((ContentInitializeInstruction) instruction).processContentInstruction(mainSource, nodeDefinitions, deltaNodes);
                    break;
                case CONTENTRESOURCE:
                    ((SourceInitializeInstruction) instruction).processSource(module, nodeDefinitions, deltaNodes);
                    break;
                case WEBFILEBUNDLE:
                    ((WebFileBundleInstruction) instruction).processWebFileBundle(mainSource, target);
                    break;
                case RESOURCEBUNDLES:
                    ((ResourcebundlesInitializeInstruction) instruction).processResourceBundles(module, resourceBundleParents, resourceBundles);
                    break;
            }
        }
        if (resourceBundleParents.getNode() != null && !resourceBundleParents.getNode().getNodes().isEmpty()) {
            // add resourcebundles translations root definition parents
            mainSource.addContentDefinition(resourceBundleParents);
        }
    }

    // write out module
    protected void serializeModule() throws IOException {
        Iterator<SourceImpl> sourceIter = module.getModifiableSources().iterator();
        while (sourceIter.hasNext()) {
            SourceImpl source = sourceIter.next();
            if (source.getDefinitions().isEmpty()) {
                log.info("No definitions found or left for source " + source.getPath() + ": source skipped.");
                sourceIter.remove();
            }
        }

        boolean multiModule = module.getProject().getConfiguration().getProjects().stream().mapToInt(p -> p.getModules().size()).sum() > 1;
        ModuleContext moduleContext = aggregate ? new AggregatedModuleContext(module, src.toPath(), multiModule) :
                new LegacyModuleContext(module, src.toPath(), multiModule);
        moduleContext.createOutputProviders(target.toPath());

        new FileConfigurationWriter().writeModule(module, Constants.DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
    }
}
