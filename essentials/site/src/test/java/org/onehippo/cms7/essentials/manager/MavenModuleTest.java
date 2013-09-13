package org.onehippo.cms7.essentials.manager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.shared.model.Dependency;

import static org.junit.Assert.assertEquals;

public class MavenModuleTest {

    private File testDir;
    private File pomFile;
    private File childDir;
    private File childPOM;

    @Before
    public void createPoms() throws IOException {
        testDir = new File(System.getProperty("java.io.tmpdir"), "pluginManagerTest");
        testDir.mkdir();
        pomFile = new File(testDir, "pom.xml");
        writePOM("<pom><dependencies></dependencies></pom>");

        childDir = new File(testDir, "child");
        childDir.mkdir();
        childPOM = new File(childDir, "pom.xml");
        writePOM("<pom></pom>", childPOM);
    }

    @After
    public void cleanupPoms() {
        childPOM.delete();
        childDir.delete();
        pomFile.delete();
//        if (!testDir.delete()) {
//            throw new RuntimeException("Could not clean up");
//        }
    }

    @Test
    public void pomCanBeRead() throws IOException {
        MavenModule model = new MavenModule(testDir);
        final HashMap<String, MavenModule> childModels = model.getChildModels();
        assertEquals(0, childModels.size());
    }

    @Test
    public void formattingIsRetained() throws IOException {
        final String contents = "<pom><!-- comment --><dependencies>\n</dependencies></pom>";
        writePOM(contents);

        MavenModule model = new MavenModule(testDir);
        model.flush();

        assertEquals(contents, readPOM());
    }

    @Test
    public void dependencyIsAdded() throws IOException {
        writePOM("<pom><dependencies><dependency></dependency>\n<!-- end deps --></dependencies></pom>");

        MavenModule model = new MavenModule(testDir);
        final Dependency mvnDependency = new Dependency();
        mvnDependency.setArtifactId("arty");
        mvnDependency.setGroupId("groupy");
        mvnDependency.setVersion("1.0");
        model.addDependency(mvnDependency);
        model.flush();

        assertEquals("<pom><dependencies><dependency></dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>groupy</groupId>\n" +
                "      <artifactId>arty</artifactId>\n" +
                "      <version>1.0</version>\n" +
                "    </dependency>\n" +
                "<!-- end deps -->" +
                "</dependencies></pom>", readPOM());
    }

    @Test
    public void childModelsAreFound() throws IOException {
        final String contents = "<pom><dependencies>\n</dependencies><modules><module>child</module></modules></pom>";
        writePOM(contents);

        createChildModule();

        MavenModule model = new MavenModule(testDir);
        final HashMap<String, MavenModule> childModels = model.getChildModels();
        assertEquals(1, childModels.size());
        assertEquals("child", childModels.keySet().iterator().next());
    }

    @Test
    public void childModelsAreFoundInDefaultProfile() throws IOException {
        final String contents = "<pom><profiles><profile><id>default</id><modules><module>child</module></modules></profile></profiles></pom>";
        writePOM(contents);

        createChildModule();

        MavenModule model = new MavenModule(testDir);
        final HashMap<String, MavenModule> childModels = model.getChildModels();
        assertEquals(1, childModels.size());
        assertEquals("child", childModels.keySet().iterator().next());
    }

    private void createChildModule() throws IOException {
    }

    private void writePOM(String contents) throws IOException {
        writePOM(contents, pomFile);
    }

    private void writePOM(String contents, File file) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(contents);
        writer.close();
    }

    private String readPOM() throws IOException {
        return readPOM(pomFile);
    }

    private String readPOM(File file) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(file), baos);
        return new String(baos.toByteArray(), "utf-8");
    }

}
