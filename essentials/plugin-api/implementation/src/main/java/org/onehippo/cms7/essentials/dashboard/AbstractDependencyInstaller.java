package org.onehippo.cms7.essentials.dashboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.onehippo.cms7.essentials.dashboard.installer.InstallState;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class will install cms and site pom dependencies to your plugin.
 * Implement the #getCmsDependencies and #getSiteDependencies method to retrieve the dependencies in your plugin
 *
 * @version "$Id: AbstractDependencyInstaller.java 174582 2013-08-21 16:56:23Z mmilicevic $"
 */
public abstract class AbstractDependencyInstaller implements Installer {

    private static Logger log = LoggerFactory.getLogger(AbstractDependencyInstaller.class);

    /**
     * Implement CMS dependencies
     * @return a list of CMS dependencies according to the apache maven model
     */
    public abstract List<Dependency> getCmsDependencies();

    /**
     * Implement SITE dependencies
     * @return a list of Site dependencies according to the apache maven model
     */
    public abstract List<Dependency> getSiteDependencies();

    private FileReader fileReader = null;
    private FileWriter fileWriter = null;
    private final MavenXpp3Reader reader = new MavenXpp3Reader();
    private final MavenXpp3Writer writer = new MavenXpp3Writer();

    @Override
    public void install() {
        installCmsDependencies();
        installSiteDependencies();
    }

    /**
     * Install CMS dependencies
     */
    public void installCmsDependencies() {
        try {
            fileReader = new FileReader(ProjectUtils.getCms().getPath() + File.separatorChar + EssentialConst.POM_XML);
            final Model model = reader.read(fileReader);
            final List<Dependency> cmsDependencies = getCmsDependencies();
            if (cmsDependencies != null && !cmsDependencies.isEmpty()) {
                for (Dependency dependency : cmsDependencies) {
                    model.addDependency(dependency);
                }
            }
            fileWriter = new FileWriter(ProjectUtils.getCms().getPath() + File.separatorChar + EssentialConst.POM_XML);
            writer.write(fileWriter, model);
        } catch (IOException e) {
            log.error("io exception while trying to add cms dependency {}", e);
        } catch (XmlPullParserException e) {
            log.error("xml pull parser exception while trying to add cms dependency", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
            IOUtils.closeQuietly(fileWriter);
        }
    }


    /**
     *
     */
    public void installSiteDependencies() {
        try {
            fileReader = new FileReader(ProjectUtils.getSite().getPath() + File.separatorChar + EssentialConst.POM_XML);
            final Model model = reader.read(fileReader);
            final List<Dependency> siteDependencies = getSiteDependencies();
            if (siteDependencies != null && !siteDependencies.isEmpty()) {
                for (Dependency dependency : siteDependencies) {
                    model.addDependency(dependency);
                }
            }
            fileWriter = new FileWriter(ProjectUtils.getSite().getPath() + File.separatorChar + EssentialConst.POM_XML);
            writer.write(fileWriter, model);
        } catch (IOException e) {
            log.error("io exception while trying to add cms dependency {}", e);
        } catch (XmlPullParserException e) {
            log.error("xml pull parser exception while trying to add cms dependency", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
            IOUtils.closeQuietly(fileWriter);
        }

    }


    @Override
    public InstallState getInstallState() {
        final List<Dependency> cmsDependencies = getCmsDependencies();
        if (cmsDependencies != null && !cmsDependencies.isEmpty()) {
            for (Dependency dependency : cmsDependencies) {
                final boolean installed = ProjectUtils.isInstalled(DependencyType.CMS, dependency);
                if (!installed) {
                    return InstallState.UNINSTALLED;
                }
            }
        }
        final List<Dependency> siteDependencies = getSiteDependencies();
        if (siteDependencies != null && !siteDependencies.isEmpty()) {
            for (Dependency dependency : siteDependencies) {
                final boolean installed = ProjectUtils.isInstalled(DependencyType.SITE, dependency);
                if (!installed) {
                    return InstallState.UNINSTALLED;
                }
            }
        }
        return InstallState.INSTALLED;
    }

    @Override
    public boolean isInstalled() {
        return getInstallState() != InstallState.UNINSTALLED;
    }
}
