package org.onehippo.cms7.essentials.dashboard.easyforms.installer;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.AbstractCndInstaller;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

public class EasyFormsInstaller extends AbstractCndInstaller {

    public EasyFormsInstaller(PluginContext context, String uri) {
        super(context, uri);
    }

    @Override
    public List<Dependency> getCmsDependencies() {
        // Versions > 2.11.xx have split dependencies
        final List<Dependency> dependencies = new ArrayList<>();
        Dependency repository = new Dependency();
        repository.setGroupId("org.onehippo.forge");
        repository.setArtifactId("easy-forms-repository");
        Dependency cms = new Dependency();
        cms.setGroupId("org.onehippo.forge");
        cms.setArtifactId("easy-forms-cms");
        dependencies.add(repository);
        dependencies.add(cms);
        return dependencies;
    }

    @Override
    public List<Dependency> getSiteDependencies() {
        final List<Dependency> dependencies = new ArrayList<>();
        Dependency site = new Dependency();
        site.setGroupId("org.onehippo.forge");
        site.setArtifactId("easy-forms-hst");
        dependencies.add(site);
        return dependencies;
    }
}
