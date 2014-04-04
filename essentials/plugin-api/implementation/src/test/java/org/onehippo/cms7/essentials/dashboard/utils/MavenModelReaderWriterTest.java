package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: MavenModelReaderWriterTest.java 173934 2013-08-15 15:19:54Z mmilicevic $"
 */
// TODO: is this utility test and which utility class is this???

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MavenModelReaderWriterTest {

    final static File targetDir = new File(System.getProperty("java.io.tmpdir"));
    final static String absolutePomTempPath = targetDir + "/pom2.xml";
    static File target;
    private static Logger log = LoggerFactory.getLogger(MavenModelReaderWriterTest.class);
    final MavenXpp3Reader reader = new MavenXpp3Reader();
    final MavenXpp3Writer writer = new MavenXpp3Writer();
    FileReader fileReader = null;
    FileWriter fileWriter = null;

    @BeforeClass
    public static void setUpClass() throws Exception {
        target = new File(GlobalUtils.decodeUrl(absolutePomTempPath));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        assertTrue(target.exists());
        final boolean access = target.delete();
        if (access) {
            FileUtils.forceDeleteOnExit(target);
            assertFalse(target.exists());
        }
    }

    @Test
    public void firstTestReadWriteMavenModel() throws Exception {
        try {
            final URL resource = getClass().getResource("/cms-pom.xml");
            final String path = resource.getPath();

            final File source = new File(GlobalUtils.decodeUrl(path));

            // To copy a file to a specified folder we can use the
            // FileUtils.copyFileToDirectory() method.
            log.info("Copying " + source + " file to " + targetDir);
            FileUtils.copyFileToDirectory(source, targetDir);

            // Using FileUtils.copyFile() method to copy a file.
            log.info("Copying " + source + " file to " + target);
            FileUtils.copyFile(source, target);

            // To copy a file to a specified folder we can use the
            // FileUtils.copyFileToDirectory() method.
            log.info("Copying " + source + " file to " + targetDir);
            FileUtils.copyFileToDirectory(source, targetDir);

            fileReader = new FileReader(target);
            final Model model = reader.read(fileReader);

            final Dependency dependency = new Dependency();
            dependency.setGroupId("test1");
            dependency.setArtifactId("test2");
            dependency.setVersion("test3");
            model.addDependency(dependency);

            fileWriter = new FileWriter(target);
            writer.write(fileWriter, model);

        } catch (IOException e) {
            log.error("io exception. {}", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
            IOUtils.closeQuietly(fileWriter);
        }

        final Model testModel = reader.read(new FileReader(absolutePomTempPath));
        assertTrue(testModel.getDependencies().size() > 0);

    }

    @Test
    public void secondTestCheckExistingMavenDependencyInPomFile() throws Exception {
        String groupId = "test1";
        String artifactId = "test2";
        String version = "test3";
        final Model testModel = reader.read(new FileReader(absolutePomTempPath));
        final boolean b = hasDependency(testModel, groupId, artifactId, version, null);
        assertTrue(b);
    }

    private boolean hasDependency(Model model, String groupId, String artifactId, String version, String scope) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        dependency.setScope(scope);
        for (Dependency searchingForDependency : model.getDependencies()) {
            if (searchingForDependency.toString().equals(dependency.toString())) {
                return true;
            }
        }
        return false;
    }

}
