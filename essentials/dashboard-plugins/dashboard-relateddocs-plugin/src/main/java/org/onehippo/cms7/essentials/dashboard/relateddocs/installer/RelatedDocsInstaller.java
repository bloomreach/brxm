package org.onehippo.cms7.essentials.dashboard.relateddocs.installer;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.onehippo.cms7.essentials.dashboard.AbstractCndInstaller;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;

/**
 * @version "$Id$"
 */
public class RelatedDocsInstaller extends AbstractCndInstaller {

    public RelatedDocsInstaller(final PluginContext context, final String uri) {
        super(context, uri);
    }

    @Override
    public List<Dependency> getCmsDependencies() {
        final List<Dependency> relatedDocsInstaller = new ArrayList<>();
        Dependency relatedDocs = new Dependency();
        relatedDocs.setGroupId("org.onehippo.forge");
        relatedDocs.setArtifactId("relateddocs");
        relatedDocsInstaller.add(relatedDocs);
        return relatedDocsInstaller;
    }

    @Override
    public List<Dependency> getSiteDependencies() {
        return null;
    }
}
