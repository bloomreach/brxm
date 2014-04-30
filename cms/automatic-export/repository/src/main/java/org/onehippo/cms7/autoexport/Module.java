/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import javax.jcr.Session;

final class Module {

    private final String modulePath;
    private final Collection<String> repositoryPaths;
    private final File exportDir;
    private final Extension extension;
    private final Exporter exporter;
    private final InitializeItemFactory factory;
    private final ExclusionContext exclusionContext;
    
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
}
