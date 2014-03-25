package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.onehippo.cms7.essentials.dashboard.utils.code.ComponentInformation;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class JavaSourceUtilsTest extends BaseResourceTest {

    public static final String CLASS_NAME = "TestExampleClass";
    private static Logger log = LoggerFactory.getLogger(JavaSourceUtilsTest.class);
    private String absolutePath = "";
    private Path path;
    private Path componentFile;

    @Override
    @Before
    public void setUp() throws Exception {

        super.setUp();
        final String tmpDir = System.getProperty("java.io.tmpdir");
        absolutePath = new File(tmpDir).getAbsolutePath();
        path = JavaSourceUtils.createJavaClass(absolutePath, CLASS_NAME, "com.foo.bar", ".txt");
    }

    @Test
    public void testJcrType() throws Exception {
        final Path startDirectory = getContext().getBeansPackagePath();
        Collection<String> myTypes = new ArrayList<>();
        final List<Path> directories = new ArrayList<>();
        GlobalUtils.populateDirectories(startDirectory, directories);
        final String pattern = "*." + "txt";
        for (Path directory : directories) {
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
                for (Path myPath : stream) {
                    final String nodeJcrType = JavaSourceUtils.getNodeJcrType(myPath);
                    myTypes.add(nodeJcrType);
                }

            } catch (IOException e) {
                log.error("Error reading java files", e);
            }
        }
        assertTrue(myTypes.size() <= NAMESPACES_TEST_SET.size());
        for (String namespace : NAMESPACES_TEST_SET) {
            if (namespace.contains("extend")) {
                continue;
            }
            assertTrue(myTypes.contains(namespace));
        }

    }

    @Test
    public void testJavaClassCreation() throws Exception {
        final String expected = absolutePath + File.separator + "com" + File.separator + "foo" + File.separator + "bar" + File.separator + CLASS_NAME + ".txt";
        assertEquals(expected, path.toFile().getAbsolutePath());
    }

    @Test
    public void testWritingComponent() throws Exception {

        final ComponentInformation info = new ComponentInformation();

        info.setTargetClassName("MyHippoComponent");
        info.setTargetPackageName("org.test");
        info.addDefaultComponentImports();
        info.setExtendingComponentName("EssentialsDocumentComponent");
        info.addImport("org.onehippo.cms7.essentials.components.EssentialsDocumentComponent");
        info.addImport("org.onehippo.cms7.essentials.components.info.EssentialsDocumentComponentInfo");
        componentFile = JavaSourceUtils.writeEssentialsComponent(info, getContext());

    }

    @Test
    public void testCreateHippoBean() throws Exception {
        final String myHippoBean = JavaSourceUtils.createHippoBean(path, "com.foo.bar", "foo:namespace", "MyHippoBean");
        log.info("myHippoBean {}", myHippoBean);
        assertTrue(myHippoBean != null);
        ExistingMethodsVisitor collection = JavaSourceUtils.getMethodCollection(path);
        assertEquals(0, collection.getMethodsNames().size());
        // add method:
        JavaSourceUtils.addBeanMethodString(path, "testMethod", "my:property", false);
        JavaSourceUtils.addBeanMethodCalendar(path, "calendarTestMethod", "my:property", false);
        JavaSourceUtils.addBeanMethodHippoHtml(path, "htmlTestMethod", "my:property", false);
        // reload:
        collection = JavaSourceUtils.getMethodCollection(path);
        assertEquals(3, collection.getMethodsNames().size());
        assertEquals(3, collection.getGeneratedMethodNames().size());
        assertEquals("testMethod", collection.getMethodsNames().get(0));
        assertEquals("testMethod", collection.getGeneratedMethodNames().get(0));
        // check imports
        final List<String> statements = JavaSourceUtils.getImportStatements(path);
        assertTrue(statements.contains(HippoEssentialsGenerated.class.getCanonicalName()));

    }

    @Test
    public void testGetName() throws Exception {

        final String fullQualifiedClassName = JavaSourceUtils.getFullQualifiedClassName(path);
        assertEquals(fullQualifiedClassName, "com.foo.bar.TestExampleClass");


    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (path != null) {
            Files.deleteIfExists(path);
        }
        if (componentFile != null) {
            Files.deleteIfExists(componentFile);
        }


    }
}
