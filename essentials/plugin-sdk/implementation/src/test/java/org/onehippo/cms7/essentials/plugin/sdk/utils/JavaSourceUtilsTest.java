/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.plugin.sdk.utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.annotations.HippoEssentialsGenerated;
import org.onehippo.cms7.essentials.plugin.sdk.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class JavaSourceUtilsTest extends BaseTest {

    public static final String CLASS_NAME = "TestExampleClass";
    public static final String CLASS_DOC_NAME = "TestDocClass";
    public static final String PACKAGE_NAME = "com.foo.bar";
    public static final String SPACE_FORMATTING = "    ";
    private static Logger log = LoggerFactory.getLogger(JavaSourceUtilsTest.class);
    private String absolutePath = "";
    private Path path;
    private Path docPath;

    @Inject private ProjectService projectService;

    @Override
    @Before
    public void setUp() throws Exception {
        final URL resource = getClass().getResource("/project");
        final Path myDir = new File(GlobalUtils.decodeUrl(resource.getPath())).toPath();
        setProjectRoot(myDir);
        ensureProjectStructure();

        final String tmpDir = System.getProperty("java.io.tmpdir");
        absolutePath = new File(tmpDir).getAbsolutePath();
        path = JavaSourceUtils.createJavaClass(absolutePath, CLASS_NAME, PACKAGE_NAME, ".txt");
        docPath = JavaSourceUtils.createJavaClass(absolutePath, CLASS_DOC_NAME, PACKAGE_NAME, ".txt");
    }

    @Test
    public void testInsertComment() throws Exception {
        final String myHippoBean = JavaSourceUtils.createHippoBean(docPath, "com.foo.bar.doc", "foo:namespace", "MyTesDocBean");
        assertNotNull(myHippoBean);
        final String text = "TODO test";
        final String oneComment = JavaSourceUtils.addClassJavaDoc(myHippoBean, text);
        final String s = JavaSourceUtils.addClassJavaDoc(oneComment, text);
        assertTrue(s.contains(text));
        assertTrue(StringUtils.countMatches(s, text) == 1);
        // add extends keyword:
        JavaSourceUtils.addExtendsClass(docPath, "HippoGalleryImageSet");
        JavaSourceUtils.addImport(docPath, EssentialConst.HIPPO_IMAGE_SET_IMPORT);
        final String extendsClass = JavaSourceUtils.getExtendsClass(docPath);
        assertEquals("HippoGalleryImageSet", extendsClass);


    }



    @Test
    public void testGetPackage() throws Exception {

        assertEquals(JavaSourceUtils.getPackageName(path), PACKAGE_NAME);
    }


    @Test
    public void testJcrType() throws Exception {
        final Path startDirectory = projectService.getBeansPackagePath();
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
        // check spaces formatting:
        final String source = GlobalUtils.readTextFile(path).toString();
        assertTrue(source.contains(SPACE_FORMATTING));
    }

    @Test
    public void testGetName() throws Exception {

        final String fullQualifiedClassName = JavaSourceUtils.getFullQualifiedClassName(path);
        assertEquals(fullQualifiedClassName, "com.foo.bar.TestExampleClass");


    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (path != null) {
            Files.deleteIfExists(path);
        }
        if (docPath != null) {
            Files.deleteIfExists(docPath);
        }
    }
}
