package org.onehippo.cms7.essentials.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.onehippo.cms7.essentials.shared.model.Dependency;
import org.onehippo.cms7.essentials.shared.model.Plugin;
import org.onehippo.cms7.essentials.shared.model.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeroen Reijn
 */
@Path("/manager")
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON})
public class PluginManagerResource extends AbstractResource {

    private final static Logger logger = LoggerFactory.getLogger(PluginManagerResource.class);

    public static final String PROJECT_BASEDIR_PROPERTY = "project.basedir";
    private static final String ROOT_MODULE_NAME = "root";


    public PluginManagerResource() {
    }

    @POST
    @Path("/install")
    public Plugin installPlugin(Plugin plugin) {

        String baseDir = getBaseDir();
        if (baseDir != null) {
            try {
                MavenModule mavenProjectModel = getMavenProjectModel(baseDir);

                Map<String, MavenModule> childModels = mavenProjectModel.getChildModels();
                List<Version> versions = plugin.getVersions();
                if (versions != null && versions.size() > 0) {

                    final Version lastVersion = versions.get(0);
                    final List<Dependency> dependencies = lastVersion.getDependencies();
                    for (Dependency dependency : dependencies) {
                        String projectType = dependency.getProjectType().toLowerCase();
                        childModels.get(projectType).addDependency(dependency);

                    }
                }
                for (Map.Entry<String, MavenModule> entry : childModels.entrySet()) {
                    entry.getValue().flush();
                }

                mavenProjectModel.flush();

                plugin.setInstalled(true);

            } catch (Exception e) {
                logger.error("An exception occurred: {}", e);
            }
        }

        return plugin;
    }

    @GET
    @Path("/list")
    public List<Dependency> listDependencies() {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        String baseDir = getBaseDir();
        if (baseDir != null) {
            try {
                MavenModule module = getMavenProjectModel(baseDir);
                dependencies.addAll(module.getDependencies());
                for (Map.Entry<String, MavenModule> entry : module.getChildModels().entrySet()) {
                    for (Dependency childDep : entry.getValue().getDependencies()) {
                        childDep.setProjectType(entry.getKey());
                        dependencies.add(childDep);
                    }
                }
            } catch (Exception e) {
                logger.error("An exception occurred: {}", e);
            }
        }
        return dependencies;
    }

    @DELETE
    @Path("/uninstall")
    public Response uninstallPlugin(Plugin plugin) {
        return Response.ok().build();
    }

    private MavenModule getMavenProjectModel(final String baseDir) throws IOException, XmlPullParserException {
        return new MavenModule(new File(baseDir));
    }

    private String getBaseDir() {
        if (System.getProperty(PROJECT_BASEDIR_PROPERTY) != null && !System.getProperty(PROJECT_BASEDIR_PROPERTY).isEmpty()) {
            return System.getProperty(PROJECT_BASEDIR_PROPERTY);
        } else {
            return null;
        }
    }
}
