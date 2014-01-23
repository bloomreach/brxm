package org.onehippo.cms7.essentials.dashboard.utils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.HippoEssentialsGeneratedObject;
import org.onehippo.cms7.essentials.dashboard.utils.beansmodel.MemoryBean;
import org.onehippo.cms7.essentials.dashboard.utils.code.ExistingMethodsVisitor;
import org.onehippo.cms7.essentials.dashboard.utils.code.NoAnnotationMethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id: BeanWriterUtilsTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class BeanWriterUtilsTest extends BaseTest {


    private static Logger log = LoggerFactory.getLogger(BeanWriterUtilsTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        buildExisting();
    }

/*
    @Test
    public void testAnnotationDuplicates() throws Exception {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream("_test_annotation.txt");
        final String myBean = GlobalUtils.readStreamAsText(resourceAsStream);
        log.info("myBean {}", myBean);
        BeanWriterUtils.annotateExistingBeans();


    }*/

    @Test
    public void testMethodCreation() throws Exception {
        final List<Path> existing = BeanWriterUtils.findExitingBeans(getContext(), "txt");
        assertEquals(NAMESPACES_TEST_SET.size(), existing.size());
        for (Path path : existing) {
            final NoAnnotationMethodVisitor methodCollection = JavaSourceUtils.getAnnotateMethods(getContext(), path);
            assertEquals(0, methodCollection.getModifiableMethods().size());
            final ExistingMethodsVisitor annotated = JavaSourceUtils.getMethodCollection(path);
            if (path.toFile().getName().equals("DependencyCompound.txt")) {
                // unchanged
                assertEquals(6, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("BaseDocument.txt")) {
                // unchanged
                assertEquals(2, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("NewsDocument.txt")) {
                // one extra method added
                assertEquals(9, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("TextDocument.txt")) {
                // unchanged
                assertEquals(2, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("DependencyCompound.txt")) {
                // unchanged
                assertEquals(6, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("PluginDocument.txt")) {
                // unchanged
                assertEquals(2, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("VendorDocument.txt")) {
                // unchanged
                assertEquals(3, annotated.getMethods().size());
            }
            //############################################
            // NEW CREATED
            //############################################
            else if (path.toFile().getName().equals("Extendedbase.txt")) {
                // unchanged
                assertEquals(1, annotated.getMethods().size());
            } else if (path.toFile().getName().equals("Extendingnews.txt")) {
                // unchanged
                assertEquals(11, annotated.getMethods().size());
            }
        }


    }

    @Test
    public void testFindExistingBeans() throws Exception {
        final List<Path> existing = BeanWriterUtils.findExitingBeans(getContext(), "txt");
        assertEquals(NAMESPACES_TEST_SET.size(), existing.size());
    }

    @SuppressWarnings("StringContatenationInLoop")
    private void buildExisting() throws Exception {

        final PluginContext context = getContext();
        context.setProjectNamespacePrefix(HIPPOPLUGINS_NAMESPACE);
        final List<MemoryBean> memoryBeans = BeanWriterUtils.buildBeansGraph(getProjectRoot(), context, "txt");
        // NOTE: one bean is not mapped within XML (only java {@code DuplicateAnnotation.txt})
        assertEquals(NAMESPACES_TEST_SET.size() -1, memoryBeans.size());
        for (MemoryBean memoryBean : memoryBeans) {
            final String namespaced = String.format("%s:%s", memoryBean.getNamespace(), memoryBean.getName());
            assertTrue("expected " + namespaced, NAMESPACES_TEST_SET.contains(namespaced));
            if (!namespaced.contains("extend")) {
                if (memoryBean.getBeanPath() == null) {
                    log.info("memoryBean {}", memoryBean);
                }
                assertTrue("Expected bean path to be none null:" + namespaced, memoryBean.getBeanPath() != null);
            }
        }
        BeanWriterUtils.addMissingMethods(context, memoryBeans, ".txt");

    }

    @SuppressWarnings("StringContatenationInLoop")
    @Test
    public void testAnnotateExisting() throws Exception {
        BeanWriterUtils.annotateExistingBeans(getContext(), "txt");
        final List<Path> existing = BeanWriterUtils.findExitingBeans(getContext(), "txt");
        assertEquals(NAMESPACES_TEST_SET.size(), existing.size());
        final Collection<String> parsedNamespaces = new ArrayList<>();
        for (Path path : existing) {
            final HippoEssentialsGeneratedObject annotationObj = JavaSourceUtils.getHippoGeneratedAnnotation(path);
            assertTrue(annotationObj != null);
            parsedNamespaces.add(annotationObj.getInternalName());
        }
        for (String namespace : NAMESPACES_TEST_SET) {
            if (namespace.contains("extend")) {
                continue;
            }

            final boolean contains = parsedNamespaces.contains(namespace);
            if (!contains) {
                log.error("namespace {}", namespace);
            }

            assertTrue("Expected to find:" + namespace, contains);
        }


    }
}
