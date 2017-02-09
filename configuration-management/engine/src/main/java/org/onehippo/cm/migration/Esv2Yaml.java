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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.onehippo.cm.engine.FileConfigurationWriter;
import org.onehippo.cm.engine.FileResourceInputProvider;
import org.onehippo.cm.engine.FileResourceOutputProvider;
import org.onehippo.cm.engine.SourceSerializer;
import org.onehippo.cm.impl.model.ConfigurationImpl;
import org.onehippo.cm.impl.model.ModuleImpl;
import org.onehippo.cm.impl.model.SourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Esv2Yaml {

    static final Logger log = LoggerFactory.getLogger(Esv2Yaml.class);

    public static final String HIPPOECM_EXTENSION_FILE = "hippoecm-extension.xml";
    public static final String TRANSLATIONS_ROOT_PATH = "/hippo:configuration/hippo:translations";

    public static void main(final String[] args) throws IOException, EsvParseException {

        if (args.length != 2) {
            System.out.println("usage: <src> <target>\n" +
                               "<src>   : directory of a "+HIPPOECM_EXTENSION_FILE+" file\n" +
                               "<target>: directory for writing the repo-config (will be emptied first)");
            return;
        }
        try {
            new Esv2Yaml(new File(args[0]), new File(args[1])).convert();
        } catch (Exception e) {
            Throwable t = e;
            if (e.getCause() != null) {
                t = e.getCause();
            }
            log.error("Esv2Yaml.convert() failed: "+t.getMessage());
//            throw e;
        }
    }

    private static final String[] MAIN_YAML_NAMES = { "main", "root", "base", "index" };

    private final File src;
    private final File target;
    private final EsvParser esvParser;
    private final File extensionFile;
    private final ModuleImpl module;

    public Esv2Yaml(final File src, final File target) throws IOException, EsvParseException {
        this.src = src;
        this.target = target;
        extensionFile = new File(src, HIPPOECM_EXTENSION_FILE);
        if (!extensionFile.exists() || !extensionFile.isFile()) {
            throw new IOException("File not found: "+extensionFile.getCanonicalPath());
        }
        if (target.exists()) {
            if (target.isFile()) {
                throw new IllegalArgumentException("Target is not a directory");
            }
            else {
                FileUtils.deleteDirectory(target);
                target.mkdirs();
            }
        }
        esvParser = new EsvParser(src);
        module = new ConfigurationImpl("dummy").addProject("dummy").addModule("dummy");
    }

    public void convert() throws IOException, EsvParseException {
        log.info("Converting src: "+src.getCanonicalPath());

        final EsvNode rootNode = esvParser.parse(new FileInputStream(extensionFile), extensionFile.getCanonicalPath());
        if (rootNode != null) {
            if ("hippo:initializefolder".equals(rootNode.getType())) {

                // parse and create list of initializeitem instructions
                final List<InitializeInstruction> instructions = new ArrayList<>();
                final Set<String> initializeItemNames = new HashSet<>();
                for (EsvNode child : rootNode.getChildren()) {
                    if ("hippo:initializeitem".equals(child.getType())) {
                        // fail on duplicate initializeitem names, as that is not allowed/error anyway and could cause sorting error
                        if (!initializeItemNames.add(child.getName())) {
                            throw new EsvParseException("Duplicate hippo:initializeitem name: "+child.getName());
                        }
                        InitializeInstruction.parse(child, instructions);
                    }
                    else {
                        log.warn("Ignored "+HIPPOECM_EXTENSION_FILE+" node: "+child.getName());
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
                final SourceImpl mainSource = module.addSource(createSourcePath(MAIN_YAML_NAMES, sourcePaths, 0));

                processInitializeInstructions(mainSource, instructions);
                serializeModule();
            }
            else {
                throw new EsvParseException(extensionFile.getCanonicalPath()+" should have a root node with \"hippo:initializefolder\" jcr:primaryType");
            }
            return;
        }
    }

    // create unique source path using candidate(s) name (without extension), appending sequence if needed
    protected String createSourcePath(final String[] candidates, final Set<String> sourcePaths, final int sequence) {
        String sourcePath = null;
        for (String name : candidates) {
            name = name + (sequence == 0 ? "" : "-"+sequence) + ".yaml";
            if (!sourcePaths.contains(name)) {
                sourcePath = name;
                break;
            }
        }
        if (sourcePath == null) {
            return createSourcePath(candidates, sourcePaths, sequence+1);
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
                }
                catch (URISyntaxException e) {
                    throw new EsvParseException("Invalid namespace uri: "+uriValue+" for initialize item: "+instruction.getName());
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
                ((SourceInitializeInstruction)instruction).prepareSource(esvParser);
                break;
            case CONTENTPROPSET:
            case CONTENTPROPADD:
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
    protected void processInitializeInstructions(final SourceImpl mainSource, final List<InitializeInstruction> instructions) throws IOException, EsvParseException {
        for (InitializeInstruction instruction : instructions) {

            // log instruction to be processed
            {
                final StringBuilder delta = new StringBuilder("");
                if (instruction instanceof SourceInitializeInstruction) {
                    EsvMerge merge = ((SourceInitializeInstruction) instruction).getSourceNode().getMerge();
                    if (merge != null) {
                        delta.append(" (").append(merge).append(")");
                    }
                }
                log.info("Instruction: " + instruction.getType() + " named: " + instruction.getName() + delta.toString());
            }

            switch (instruction.getType()) {
                case NAMESPACE:
                    try {
                        mainSource.addNamespaceDefinition(instruction.getName(), new URI(instruction.getTypePropertyValue()));
                    }
                    catch (URISyntaxException e) {
                        // already checked
                    }
                    break;
                case NODETYPESRESOURCE:
                    mainSource.addNodeTypeDefinition(instruction.getResourcePath(), true);
                    break;
                case RESOURCEBUNDLES:
                    ((ResourcebundlesInitializeInstruction)instruction).processResourceBundles(module);
                default:
                    // todo
                    break;
            }
        }
    }

    // write out module
    protected void serializeModule() throws IOException {
        final FileResourceInputProvider resourceInputProvider = new FileResourceInputProvider(src.toPath());
        final FileResourceOutputProvider resourceOutputProvider = new FileResourceOutputProvider(target.toPath());
        new FileConfigurationWriter().writeModule(module, resourceOutputProvider.getModulePath(),
                new SourceSerializer(), resourceInputProvider, resourceOutputProvider);
    }
}
