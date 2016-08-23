/*
 * Copyright 2012-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.autoexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;

import static org.onehippo.cms7.autoexport.AutoExportModule.log;

final class Module {

    private final String modulePath;
    private final Collection<String> repositoryPaths;
    private final File exportDir;
    private final Extension extension;
    private final Exporter exporter;
    private final InitializeItemFactory factory;
    private final ExclusionContext exclusionContext;
    private final InitializeItemRegistry registry;
    
    Module(String modulePath, Collection<String> repositoryPaths, File baseDir, InitializeItemRegistry registry, Session session, Configuration configuration) throws Exception {
        this.modulePath = modulePath;
        this.repositoryPaths = repositoryPaths;
        exportDir = new File(baseDir.getPath() + "/" + modulePath + "/src/main/resources");
        List<String> exclusionPatterns = new ArrayList<String>();
        for (String repositoryPath : repositoryPaths) {
            exclusionPatterns.add(repositoryPath);
            exclusionPatterns.add(repositoryPath.equals("/") ? "/**" : repositoryPath + "/**");
        }
        // 'misuse' exclusion context for matching of repository paths
        exclusionContext = new ExclusionContext(exclusionPatterns);
        extension = new Extension(this, registry);
        factory = new InitializeItemFactory(this, registry, extension.getId());
        exporter = new Exporter(this, session, registry, configuration);
        this.registry = registry;
    }
    
    File getExportDir() {
        return exportDir;
    }
    
    String getModulePath() {
        return modulePath;
    }

    Collection<String> getRepositoryPaths() {
        return repositoryPaths;
    }

    Extension getExtension() {
        return extension;
    }
    
    Exporter getExporter() {
        return exporter;
    }

    InitializeItemFactory getInitializeItemFactory() {
        return factory;
    }
    
    public boolean isPathForModule(String path) {
        return exclusionContext.isExcluded(path);
    }
    
    void check() {
        if (!exportDir.exists()) {
            log.info("Auto-export module directory {} does not (yet) exist", exportDir.getPath());
        } else {
            detectBrokenInitializeItems();
            detectOrphanedFiles(exportDir, getBootstrapFiles());
        }
    }

    private void detectBrokenInitializeItems() {
        Set<String> names = new HashSet<>();
        for (InitializeItem initializeItem : extension.getInitializeItems()) {
            final String itemName = initializeItem.getName();
            if (!names.add(itemName)) {
                log.warn("detected duplicate items {}", itemName);
            }
            final String contentResource = initializeItem.getContentResource();
            if (!StringUtils.isEmpty(contentResource)) {
                final File file = new File(exportDir, contentResource);
                if (!file.exists()) {
                    log.warn("Found initialize item {} that has missing content resource {}", itemName, contentResource);
                }
            }
            final String nodeTypesResource = initializeItem.getNodeTypesResource();
            if (!StringUtils.isEmpty(nodeTypesResource)) {
                final File file = new File(exportDir, nodeTypesResource);
                if (!file.exists()) {
                    log.warn("Found initialize item {} that has missing node types resource {}", itemName, nodeTypesResource);
                }
            }
            final String resourceBundles = initializeItem.getResourceBundles();
            if (!StringUtils.isEmpty(resourceBundles)) {
                final File file = new File(exportDir, resourceBundles);
                if (!file.exists()) {
                    log.warn("Found initialize item {} that has missing resource bundles resource {}", itemName, resourceBundles);
                }
            }
        }
    }

    private void detectOrphanedFiles(final File directory, final List<File> bootstrapFiles) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                detectOrphanedFiles(file, bootstrapFiles);
            } else if ((file.getName().endsWith(".xml") || file.getName().endsWith(".cnd") || file.getName().endsWith(".json")) && !file.getName().endsWith("hippoecm-extension.xml")) {
                if (!bootstrapFiles.contains(file)) {
                    log.warn("Detected possibly orphaned bootstrap configuration file {}", file.getPath());
                }
            }
        }
    }
    
    private List<File> getBootstrapFiles() {
        final List<File> bootstrapFiles = new ArrayList<>();
        for (InitializeItem initializeItem : extension.getInitializeItems()) {
            final String contentResource = initializeItem.getContentResource();
            if (!StringUtils.isEmpty(contentResource)) {
                bootstrapFiles.add(new File(exportDir, contentResource));
            }
            final String nodetypesResource = initializeItem.getNodeTypesResource();
            if (!StringUtils.isEmpty(nodetypesResource)) {
                bootstrapFiles.add(new File(exportDir, nodetypesResource));
            }
            final String resourceBundles = initializeItem.getResourceBundles();
            if (!StringUtils.isEmpty(resourceBundles)) {
                bootstrapFiles.add(new File(exportDir, resourceBundles));
            }
        }
        return bootstrapFiles;
    }
}
