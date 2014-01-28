package org.onehippo.cms7.essentials.dashboard.relateddocs;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.Test;
import org.onehippo.cms7.essentials.dashboard.installer.Installer;
import org.onehippo.cms7.essentials.dashboard.relateddocs.installer.RelatedDocsInstaller;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class RelatedDocsInstallerTest {


    @Test
    public void testRelatedDocsCMSDependencyInstaller() throws Exception {
        System.setProperty(EssentialConst.PROJECT_BASEDIR_PROPERTY, getClass().getResource("/project").getPath());
        Installer installer = new RelatedDocsInstaller(null, "");
        installer.install();

        File file = new File(getClass().getResource("/project/cms/pom.xml").getFile());
        assertTrue(file.exists());

        final MavenXpp3Reader reader = new MavenXpp3Reader();
        FileReader fileReader = new FileReader(file);

        final Model model = reader.read(fileReader);

        boolean found = false;

        final List<Dependency> dependencies = model.getDependencies();
        for (Dependency dependency : dependencies) {
            if (dependency.getArtifactId().equals("relateddocs")) {
                found = true;
                break;
            }
        }

        assertTrue(found);
    }



}
