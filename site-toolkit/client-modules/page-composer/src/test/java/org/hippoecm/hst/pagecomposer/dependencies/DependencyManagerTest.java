package org.hippoecm.hst.pagecomposer.dependencies;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class DependencyManagerTest {

    @Test
    public void testEmpty() throws Exception{
        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);

        DependencyManager manager = new DependencyManager();
        manager.write(writer);

        assertEquals(0, results.size());
    }

    @Test
    public void testCssDependency() throws Exception {
        DependencyManager manager = new DependencyManager();
        manager.add(new CssDependency("file.css"));

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(1, results.size());
        assertEquals("<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"file.css\"/>", results.get(0));
    }

    @Test
    public void testJsDependency() throws Exception {
        DependencyManager manager = new DependencyManager();
        manager.add(new JsDependency("file.js"));

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(1, results.size());
        assertEquals("<script type=\"text/javascript\" src=\"file.js\"></script>", results.get(0));
    }

    @Test
    public void testJsDependencyDevmode() throws Exception {
        DependencyManager manager = new DependencyManager(true);
        manager.add(new JsDependency("file.js", "file1.js", "file2.js"));

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(2, results.size());
        assertEquals("<script type=\"text/javascript\" src=\"file1.js\"></script>", results.get(0));
        assertEquals("<script type=\"text/javascript\" src=\"file2.js\"></script>", results.get(1));
    }

    @Test
    public void testMetaDependency() throws Exception {
        DependencyManager manager = new DependencyManager();
        Dependency root = new PathDependency("/pagecomposer/sources/css", new CssDependency("file.css"));
        manager.add(root);

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(1, results.size());
        assertEquals("<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/pagecomposer/sources/css/file.css\"/>", results.get(0));
    }

    @Test
    public void testversionedDependency() throws Exception {
        DependencyManager manager = new DependencyManager();
        Dependency root = new VersionedDependency("/pagecomposer/sources/css", "3.0") {
            {
                addDependency(new CssDependency("file.css"));
            }
        };
        manager.add(root);

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(1, results.size());
        assertEquals("<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/pagecomposer/sources/css/3.0/file.css\"/>", results.get(0));
    }

    @Test
    public void testMultiMetaDependency() throws Exception {
        DependencyManager manager = new DependencyManager();
        Dependency root = new PathDependency("/root") {
            {
                addDependency(new PathDependency("level1/") {
                    {
                        addDependency(new PathDependency("level2") {
                            {
                                addDependency(new CssDependency("file.css"));
                            }
                        });
                    }
                });
            }
        };
        manager.add(root);

        List<String> results = new LinkedList<String>();
        StringWriter writer = new StringListWriter(results);
        manager.write(writer);

        assertEquals(1, results.size());
        assertEquals("<link rel=\"stylesheet\" media=\"screen\" type=\"text/css\" href=\"/root/level1/level2/file.css\"/>", results.get(0));
    }

}
