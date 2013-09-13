package org.onehippo.cms7.essentials.dashboard.taxonomy;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class TaxonomyTest {

    private static Logger log = LoggerFactory.getLogger(TaxonomyTest.class);

   // private Session session;

//    @org.junit.Before
//    public void setUp() throws Exception {
//        try {
//            final HippoRepository repository = HippoRepositoryFactory.getHippoRepository("rmi://localhost:1099/hipporepository");
//            session = repository.login("admin", "admin".toCharArray());
//        } catch (RepositoryException e) {
//            log.error("Error creating repository connection", e);
//        }
//        assumeNotNull(session);
//
//    }

    @Test
    public void testAddTaxonomyToCMSDependencyPom() throws Exception {

        FileReader fileReader = null;
        FileWriter fileWriter = null;
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        final MavenXpp3Writer writer = new MavenXpp3Writer();
        final File targetDir = new File(System.getProperty("java.io.tmpdir"));
        final String absolutePomTempPath = targetDir + "/pom2.xml";

        try {
            final URL resource = getClass().getResource("/pom.xml");
            final String path = resource.getPath();

            final File source = new File(path);
            final File target = new File(absolutePomTempPath);
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

            final List<Dependency> taxonomyDependencies = createTaxonomyDependencies();


            for(Dependency dependency: taxonomyDependencies){
                model.addDependency(dependency);
            }

            fileWriter = new FileWriter(target);
            writer.write(fileWriter, model);

        } catch (IOException e) {
            log.error("io exception. {}", e);
        } finally {
            IOUtils.closeQuietly(fileReader);
            IOUtils.closeQuietly(fileWriter);
        }

        final Model testModel = reader.read(new FileReader(targetDir + "/pom2.xml"));
        assertTrue(testModel.getDependencies().size() == 3);


    }

    public List<Dependency> createTaxonomyDependencies(){
        List<Dependency> list = new ArrayList<Dependency>();
        final Dependency dependency = new Dependency();
        dependency.setGroupId("org.onehippo");
        dependency.setArtifactId("taxonomy.api");

        final Dependency dependency2 = new Dependency();
        dependency2.setGroupId("org.onehippo");
        dependency2.setArtifactId("taxonomy-addon-frontend");

        final Dependency dependency3 = new Dependency();
        dependency3.setGroupId("org.onehippo");
        dependency3.setArtifactId("taxonomy-addon-repository");
        list.add(dependency);
        list.add(dependency2);
        list.add(dependency3);
        return list;
    }
}
