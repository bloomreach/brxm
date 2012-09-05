package org.onehippo.cms7.autoexport;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.jcr.Session;

import org.dom4j.DocumentException;

final class Module {

    private final String modulePath;
    private final Collection<String> repositoryPaths;
    private final File exportDir;
    private final Extension extension;
    private final Exporter exporter;
    private final InitializeItemFactory factory;
    private final ExclusionContext exclusionContext;
    
    Module(String modulePath, Collection<String> repositoryPaths, File baseDir, InitializeItemRegistry registry, Session session, Configuration configuration) throws DocumentException {
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
