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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import javax.jcr.PropertyType;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.Constants;
import org.onehippo.cm.model.MigrationMode;
import org.onehippo.cm.model.definition.DefinitionType;
import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.ContentSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.serializer.ModuleContext;
import org.onehippo.cm.model.serializer.ModuleWriter;
import org.onehippo.cm.model.source.Source;
import org.onehippo.cm.model.source.SourceType;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.DefinitionNode;
import org.onehippo.cm.model.tree.ValueType;
import org.onehippo.cm.model.util.InjectResidualMatchers;
import org.onehippo.cm.model.util.OverrideResidualMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.migration.ResourceProcessor.deleteEmptyDirectory;
import static org.onehippo.cm.model.Constants.HCM_CONFIG_FOLDER;
import static org.onehippo.cm.model.Constants.HCM_CONTENT_FOLDER;

public class Esv2Yaml {

    static final Logger log = LoggerFactory.getLogger(Esv2Yaml.class);

    private static final String HIPPOECM_EXTENSION_FILE = "hippoecm-extension.xml";
    private static final String TRANSLATIONS_ROOT_PATH = "/hippo:configuration/hippo:translations";
    private static final String SOURCE_FOLDER = "s";
    private static final String TARGET_FOLDER = "t";
    private static final String MODE = "m";
    private static final String ECM_LOCATION = "i";
    private static final String CONTENT_ROOTS = "content";
    private static final String CUSTOM_INJECT_RESIDUAL_NODES_SHORT = "ir";
    private static final String CUSTOM_INJECT_RESIDUAL_NODES = "custominjectresidualcategory";
    private static final String CUSTOM_OVERRIDE_RESIDUAL_NODES_SHORT = "or";
    private static final String CUSTOM_OVERRIDE_RESIDUAL_NODES = "customoverrideresidualcategory";
    private static final String[] MAIN_YAML_NAMES = {"main", "root", "base", "index"};

    private final File src;
    private final File target;
    private final EsvParser esvParser;
    private final File extensionFile;
    private final ModuleImpl module;
    private final MigrationMode migrationMode;
    private final String[] contentRoots;
    private final InjectResidualMatchers injectResidualCategoryMatchers;
    private final OverrideResidualMatchers overrideResidualCategoryMatchers;
    private final ResourceProcessor resourceProcessor = new ResourceProcessor();
    public static boolean translationMode = false;

    public static void main(final String[] args) throws IOException, EsvParseException, ParseException {

        String property = System.getProperty("allow.duplicate.translation.bundles");
        if ("true".equals(property)) {
            translationMode = true;
        }

        final Options options = createCmdOptions();

        try {

            final CommandLineParser parser = new DefaultParser();
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(SOURCE_FOLDER) && cmd.hasOption(TARGET_FOLDER)) {
                final String src = cmd.getOptionValue(SOURCE_FOLDER);
                final String target = cmd.getOptionValue(TARGET_FOLDER);
                final String[] contentRoots = parseContentRoots(cmd.getOptionValue(CONTENT_ROOTS));
                final InjectResidualMatchers injectResidualCategoryMatchers =
                        parseInjectResidualCategoryPatterns(cmd.getOptionValue(CUSTOM_INJECT_RESIDUAL_NODES));
                final OverrideResidualMatchers overrideResidualCategoryMatchers =
                        parseOverrideResidualCategoryPatterns(cmd.getOptionValue(CUSTOM_OVERRIDE_RESIDUAL_NODES));

                final MigrationMode mode = cmd.hasOption(MODE) ? MigrationMode.valueOf(cmd.getOptionValue(MODE).toUpperCase()) : MigrationMode.COPY;
                if (mode == MigrationMode.GIT && !Objects.equals(src, target)) {
                    throw new IllegalArgumentException("GIT mode requires same source & destination location");
                }

                if (cmd.hasOption(ECM_LOCATION)) {
                    new Esv2Yaml(new File(cmd.getOptionValue(ECM_LOCATION)), new File(src), new File(target), mode,
                            contentRoots, injectResidualCategoryMatchers, overrideResidualCategoryMatchers).convert();
                } else {
                    new Esv2Yaml(new File(src), new File(target), mode, contentRoots,
                            injectResidualCategoryMatchers, overrideResidualCategoryMatchers).convert();
                }
            } else {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("esv2yaml", "Converts esv based xml files to yaml format", options, StringUtils.EMPTY);
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
        options.addOption(SOURCE_FOLDER, "src", true, "bootstrap initialization resources folder (must be an absolute path)");
        options.addOption(TARGET_FOLDER, "target", true, "directory for writing the output yaml (must be an absolute path, and will be emptied first)");
        options.addOption(MODE, "mode", true, "(optional) File system mode. git/move/copy. Default is copy");
        options.addOption(CONTENT_ROOTS, "content", true, "Content root paths. Comma separated.");
        options.addOption(CUSTOM_INJECT_RESIDUAL_NODES_SHORT, CUSTOM_INJECT_RESIDUAL_NODES, true,
                "Custom paths to inject .meta:residual-child-node-category. Comma separated.");
        options.addOption(CUSTOM_OVERRIDE_RESIDUAL_NODES_SHORT, CUSTOM_OVERRIDE_RESIDUAL_NODES, true,
                "Custom paths to override .meta:residual-child-node-category. Comma separated.");
        return options;
    }

    private static String[] parseContentRoots(final String optionValue) {
        if (optionValue == null) {
            return new String[0];
        }
        final String[] split = optionValue.split(",");
        for (int i = 0; i < split.length; i++) {
            final String contentRoot = split[i].trim();
            if (contentRoot.equals("/")) {
                throw new IllegalArgumentException("'/' cannot be used as content root");
            }
            if (!contentRoot.startsWith("/")) {
                throw new IllegalArgumentException("Illegal content root '" + contentRoot + "'; content root must start with a '/'");
            }
            if (StringUtils.containsAny(contentRoot, "[", "]")) {
                throw new IllegalArgumentException("Illegal content root '" + contentRoot + "'; content root must not contain '[' or ']'");
            }
            split[i] = StringUtils.removeEnd(contentRoot, "/");
        }
        return split;
    }

    private static InjectResidualMatchers parseInjectResidualCategoryPatterns(final String optionValue) {
        if (optionValue == null) {
            return new InjectResidualMatchers(
                    "**/hst:workspace/**[hst:containercomponent]: content",
                    "**/hst:workspace/**[hst:sitemenu]: content",
                    "**/hst:workspace/hst:abstractpages: content",
                    "**/hst:workspace/hst:channel: content",
                    "**/hst:workspace/hst:components: content",
                    "**/hst:workspace/hst:pages: content",
                    "**/hst:workspace/hst:sitemap: content",
                    "**/hst:workspace/hst:templates: content",
                    "/hst:hst/hst:hosts: content",
                    "/hst:hst/hst:hosts/**[hst:virtualhostgroup]: content",
                    "/hst:hst/hst:hosts/**[hst:virtualhost]: content",
                    "/hst:hst/hst:hosts/**[hst:mount]: content");
        } else {
            if (optionValue.equals("")) {
                return new InjectResidualMatchers();
            }
            return new InjectResidualMatchers(optionValue.split(","));
        }
    }

    private static OverrideResidualMatchers parseOverrideResidualCategoryPatterns(final String optionValue) {
        if (optionValue == null) {
            return new OverrideResidualMatchers("/hst:hst/hst:hosts: config", "/hst:hst/hst:hosts/**: config");
        } else {
            if (optionValue.equals("")) {
                return new OverrideResidualMatchers();
            }
            return new OverrideResidualMatchers(optionValue.split(","));
        }
    }

    public Esv2Yaml(final File src, final File target, MigrationMode mode, final String[] contentRoots,
                    final InjectResidualMatchers injectResidualCategoryMatchers,
                    final OverrideResidualMatchers overrideResidualCategoryMatchers) throws IOException, EsvParseException {
        this(null, src, target, mode, contentRoots, injectResidualCategoryMatchers, overrideResidualCategoryMatchers);
    }

    public Esv2Yaml(final File init, final File src, final File target, MigrationMode mode, final String[] contentRoots,
                    final InjectResidualMatchers injectResidualCategoryMatchers,
                    final OverrideResidualMatchers overrideResidualCategoryMatchers) throws IOException, EsvParseException {
        this.migrationMode = mode;
        this.src = src;
        this.target = target;
        this.contentRoots = contentRoots;
        this.injectResidualCategoryMatchers = injectResidualCategoryMatchers;
        this.overrideResidualCategoryMatchers = overrideResidualCategoryMatchers;

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
                final Path configFolder = Paths.get(target.toURI()).resolve(HCM_CONFIG_FOLDER);
                final Path contentFolder = Paths.get(target.toURI()).resolve(HCM_CONTENT_FOLDER);

                if (Files.exists(configFolder) && Files.isDirectory(configFolder)) {
                    FileUtils.forceDelete(configFolder.toFile());
                }

                if (Files.exists(contentFolder) && Files.isDirectory(contentFolder)) {
                    FileUtils.forceDelete(contentFolder.toFile());
                }
            }
        }
        esvParser = new EsvParser(src);
        module = new GroupImpl("dummy").addProject("dummy").addModule("dummy");
    }

    public void convert() throws IOException, EsvParseException {
        log.info("Converting src: " + src.getCanonicalPath());

        final FileInputStream inputStream = new FileInputStream(extensionFile);
        final EsvNode rootNode = esvParser.parse(inputStream, extensionFile.getCanonicalPath());
        IOUtils.closeQuietly(inputStream);

        if (rootNode != null) {


            if ("hippo:initialize".equals(rootNode.getName())) {

                // keep track of new/custom content roots for declaring their category in main.yaml later
                final Set<String> newContentRoots = new HashSet<>();

                // keep track where we've injected a custom residual category
                final Map<String, ConfigurationItemCategory> injectResidualCategoryRegistry = new HashMap<>();

                // parse and create list of initializeitem instructions
                final List<InitializeInstruction> instructions = new ArrayList<>();
                final Set<String> initializeItemNames = new HashSet<>();
                for (EsvNode child : rootNode.getChildren()) {
                    if ("hippo:initializeitem".equals(child.getType())) {
                        // fail on duplicate initializeitem names, as that is not allowed/error anyway and could cause sorting error
                        if (!initializeItemNames.add(child.getName())) {
                            throw new EsvParseException("Duplicate hippo:initializeitem name: " + child.getName());
                        }
                        InitializeInstruction.parse(child, instructions, contentRoots, newContentRoots,
                                injectResidualCategoryMatchers, injectResidualCategoryRegistry,
                                overrideResidualCategoryMatchers);
                    } else {
                        log.warn("Ignored " + HIPPOECM_EXTENSION_FILE + " node: " + child.getName());
                    }
                }

                final Set<String> sourcePaths = new HashSet<>();


                Map<String, String> resourceList = new LinkedHashMap<>();
                // preprocess initializeitems and build set of claimed/needed target yaml source paths
                for (InitializeInstruction instruction : instructions) {
                    preprocessInitializeInstruction(instruction);
                    if (instruction instanceof SourceInitializeInstruction) {
                        sourcePaths.add(instruction.getSourcePath());
                        resourceList.putIfAbsent(instruction.getResourcePath(), instruction.getSourcePath());
                    }
                }

                // now sort the instructions on processing order
                instructions.sort(InitializeInstruction.COMPARATOR);

                // combine/link nodetypes to namespace instructions and remove nodetype instructions from the list
                combineNamespaceAndNodeTypesInstructions(instructions);

                // for resourcebundles (.json) make sure to use/create a unique target yaml source path
                // e.g. to cater for a xyz.xml esv file + xyz.json -> xyz.yaml + xyz-1.yaml
                for (InitializeInstruction instruction : instructions) {
                    if (instruction.getType() == InitializeInstruction.Type.RESOURCEBUNDLES) {
                        final String candidate = FilenameUtils.removeExtension(instruction.getResourcePath());
                        String sourcePath = createSourcePath(new String[]{candidate}, sourcePaths, 0);
                        instruction.setSourcePath(sourcePath);
                        sourcePaths.add(sourcePath);
                        resourceList.putIfAbsent(instruction.getResourcePath(), instruction.getSourcePath());
                    }
                }

                // generate unique 'main.yaml' source, optionally using 'root.yaml', 'base.yaml' or 'index.yaml' as fallback
                final ConfigSourceImpl mainSource = module.addConfigSource(createSourcePath(MAIN_YAML_NAMES, sourcePaths, 0));  //TODO SS: review this. how to distinguish content from config definitions

                processInitializeInstructions(mainSource, instructions);

                for (String contentRoot : newContentRoots) {
                    final ConfigDefinitionImpl definition = mainSource.addConfigDefinition();
                    final DefinitionNodeImpl definitionNode = new DefinitionNodeImpl(
                            contentRoot, StringUtils.substringAfterLast(contentRoot, "/"), definition);
                    definitionNode.setCategory(ConfigurationItemCategory.CONTENT);
                    definition.setNode(definitionNode);
                }

                if (!translationMode) {
                    for (final String sourcePath : resourceList.keySet()) {
                        final String destination = resourceList.get(sourcePath);
                        final Source source = module.getModifiableSources().stream().filter(s -> s.getPath().equals(destination)).findFirst().get();
                        final Path sourceFilePath = Paths.get(src.toString(), sourcePath);
                        if (source.getDefinitions().isEmpty()) {
                            if (migrationMode == MigrationMode.GIT || migrationMode == MigrationMode.MOVE) {
                                Files.deleteIfExists(sourceFilePath);
                            }
                        } else {
                            String hcmFolder = source.getType() == SourceType.CONFIG ? HCM_CONFIG_FOLDER : HCM_CONTENT_FOLDER;
                            handleFsResource(sourceFilePath, Paths.get(src.toString(), hcmFolder, destination));
                        }
                    }
                } else { //in case of translation, just delete the source file
                    for (final String sourcePath : resourceList.keySet()) {
                        final Path sourceFilePath = Paths.get(src.toString(), sourcePath);
                        Files.deleteIfExists(sourceFilePath);
                    }
                }

                final Path sourceFilePath = Paths.get(extensionFile.getAbsolutePath());
                final Path yamlMainSourceLocation = sourceFilePath.getParent().resolve(HCM_CONFIG_FOLDER).resolve(mainSource.getPath());

                if (mainSource.getDefinitions().isEmpty()) {
                    if (migrationMode == MigrationMode.GIT || migrationMode == MigrationMode.MOVE) {
                        Files.deleteIfExists(sourceFilePath);
                    }
                } else {
                    handleFsResource(sourceFilePath, yamlMainSourceLocation);
                }

                serializeModule();
            } else {
                throw new EsvParseException(extensionFile.getCanonicalPath() +
                        " should have a root node with name \"hippo:initialize\"");
            }
            return;
        }
    }

    /**
     * Handle resource at filesystem level. Depending on a migration mode:
     * <pre/>
     * GIT: performs git mv -f source target and thus, preserve version control history
     * <pre/>
     * MOVE: DELETES source file and folder if it is empty
     * <pre/>
     * COPY: Does nothing
     *
     * @throws IOException
     */
    private void handleFsResource(final Path sourceFilePath, final Path destinationPath) throws IOException {
        final Path srcDirPath = sourceFilePath.getParent();

        switch (migrationMode) {
            case GIT:
                log.info(String.format("Moving item from %s to %s", sourceFilePath, destinationPath));
                resourceProcessor.moveGitResource(sourceFilePath, destinationPath);
                deleteEmptyDirectory(srcDirPath);
                break;
            case MOVE:
                Files.deleteIfExists(sourceFilePath);
                deleteEmptyDirectory(srcDirPath);
                break;
            case COPY:
                //do Nothing
                break;
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

    protected void combineNamespaceAndNodeTypesInstructions(final List<InitializeInstruction> initializeInstructions)
            throws EsvParseException {
        for (int i = initializeInstructions.size() - 1; i >= 0; i--) {
            final InitializeInstruction instruction = initializeInstructions.get(i);
            if (instruction.getType() == InitializeInstruction.Type.NODETYPESRESOURCE) {
                if (instruction.getCombinedWith() == null) {
                    // try find matching namespace instruction based on file basename as prefix
                    final String basename = FilenameUtils.getBaseName(instruction.getResource().getName());
                    InitializeInstruction match = null;
                    for (InitializeInstruction initializeInstruction : initializeInstructions) {
                        if (initializeInstruction.getType() == InitializeInstruction.Type.NAMESPACE &&
                                initializeInstruction.getName().equals(basename)) {
                            if (initializeInstruction.getCombinedWith() != null) {
                                if (initializeInstruction.getCombinedWith().getResourcePath().equals(instruction.getResourcePath())) {
                                    // special case: additional instruction to (re)load same CND again...? Whatever, its a match
                                    match = initializeInstruction;
                                }
                            } else {
                                // match found: combine
                                initializeInstruction.setCombinedWith(instruction);
                                match = initializeInstruction;
                            }
                        }
                    }
                    if (match == null) {
                        throw new EsvParseException("Cannot match separate nodetypesresource initialize item: "
                                + instruction.getName() + " to a corresponding separate namespace initialize item");
                    }
                }
                initializeInstructions.remove(i);
            }
        }
    }

    // generate and merge instruction definitions which already have been sorted in processing order
    protected void processInitializeInstructions(final ConfigSourceImpl mainSource, final List<InitializeInstruction> instructions)
            throws IOException, EsvParseException {

        Set<String> resourceBundles = new HashSet<>();
        Set<DefinitionNode> deltaNodes = new HashSet<>();
        Map<MinimallyIndexedPath, DefinitionNodeImpl> nodeDefinitions = new LinkedHashMap<>();

        // not yet 'added' definition for resourcebundle root translation parent definitions, if needed
        final ConfigDefinitionImpl resourceBundleParents = mainSource.addConfigDefinition();
        resourceBundleParents.setNode(null);

        for (InitializeInstruction instruction : instructions) {

            log.info(instruction.getType().getPropertyName() + ": " + instruction.getName());

            switch (instruction.getType()) {
                case NAMESPACE:
                    try {
                        final String cndPath = instruction.getCombinedWith() != null ? instruction.getCombinedWith().getResourcePath() : null;
                        final ValueImpl value = cndPath != null ? new ValueImpl(cndPath, ValueType.STRING, true, false) : null;
                        mainSource.addNamespaceDefinition(instruction.getName(), new URI(instruction.getTypePropertyValue()), value);
                    } catch (URISyntaxException e) {
                        // already checked
                    }
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
                    ((ResourceBundlesInitializeInstruction) instruction).processResourceBundles(translationMode ? mainSource : module.addConfigSource(instruction.getSourcePath()), resourceBundleParents, resourceBundles);
                    break;
            }
        }

        orderContentDefinitions(nodeDefinitions.values());

        if (resourceBundleParents.getNode() == null || resourceBundleParents.getNode().getNodes().isEmpty()) {
            // remove empty resourcebundles translations root definition parents
            for (Iterator<AbstractDefinitionImpl> defIter = mainSource.getModifiableDefinitions().iterator(); defIter.hasNext(); ) {
                if (defIter.next() == resourceBundleParents) {
                    defIter.remove();
                    break;
                }
            }
        }
    }

    /**
     * Initialize order before property for root content definitions so that they could be applied in right order
     * @param definitions - collection of ordered definition nodes based on esv sequence number
     */
    private void orderContentDefinitions(final Collection<DefinitionNodeImpl> definitions) {
        final List<DefinitionNodeImpl> orderedDefinitions = new ArrayList<>(definitions);

        for (int i = 0; i < orderedDefinitions.size(); i++) {
            final DefinitionNodeImpl definitionNode = orderedDefinitions.get(i);
            if (definitionNode.getDefinition().getSource() instanceof ContentSourceImpl
                    && i != orderedDefinitions.size() - 1 && definitionNode.isRoot()) {
                final JcrPath currentParentPath = JcrPaths.getPath(definitionNode.getPath()).getParent();
                IntStream.range(i + 1, orderedDefinitions.size())
                        .mapToObj(orderedDefinitions::get)
                        .filter(item -> currentParentPath.equals(JcrPaths.getPath(item.getPath()).getParent()))
                        .findFirst()
                        .ifPresent(x -> definitionNode.setOrderBefore(x.getName()));
            }
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

        // visit all node definitions and sort properties
        module.getModifiableSources().stream().flatMap(source -> source.getDefinitions().stream())
                .filter(def -> DefinitionType.CONFIG.isOfType(def) || DefinitionType.CONTENT.isOfType(def))
                .map(TreeDefinitionImpl.class::cast)
                .map(TreeDefinitionImpl::getNode)
                .forEach(DefinitionNodeImpl::recursiveSortProperties);

        ModuleContext moduleContext = new LegacyModuleContext(module, src.toPath(), migrationMode);
        moduleContext.createOutputProviders(target.toPath());

        // patch up references to RIPs, so they can be accessed during processing via back-references
        module.setConfigResourceInputProvider(moduleContext.getConfigInputProvider());
        module.setContentResourceInputProvider(moduleContext.getContentInputProvider());

        new ModuleWriter().writeModule(module, Constants.DEFAULT_EXPLICIT_SEQUENCING, moduleContext);
    }

}
