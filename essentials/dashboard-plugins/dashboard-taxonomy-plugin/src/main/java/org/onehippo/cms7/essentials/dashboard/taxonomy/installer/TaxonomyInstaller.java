package org.onehippo.cms7.essentials.dashboard.taxonomy.installer;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.AbstractCndInstaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class TaxonomyInstaller extends AbstractCndInstaller {

    private static Logger log = LoggerFactory.getLogger(TaxonomyInstaller.class);

    public TaxonomyInstaller(final PluginContext context, final String uri) {
        super(context, uri);
    }

    @Override
    public List<Dependency> getCmsDependencies() {
        final List<Dependency> taxonomyCmsDependencies = new ArrayList<>();
        Dependency api = new Dependency();
        api.setGroupId("org.onehippo");
        api.setArtifactId("taxonomy-api");
        Dependency frontend = new Dependency();
        frontend.setGroupId("org.onehippo");
        frontend.setArtifactId("taxonomy-addon-frontend");
        Dependency repository = new Dependency();
        repository.setGroupId("org.onehippo");
        repository.setArtifactId("taxonomy-addon-repository");
        taxonomyCmsDependencies.add(api);
        taxonomyCmsDependencies.add(frontend);
        taxonomyCmsDependencies.add(repository);
        return taxonomyCmsDependencies;
    }

    /**
     * TODO
     *
     * @return
     */
    @Override
    public List<Dependency> getSiteDependencies() {
        return null;
    }


}
