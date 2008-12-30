package org.hippoecm.repository;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.junit.Before;
import org.junit.Test;

public class OrderByNodeNameTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private final static String TEST_PATH = "testnodes";
    private Node testPath;

    private final static List<String> names = new ArrayList<String>();

    static {
        names.add("aa");
        names.add("aab");
        names.add("ba");
        names.add("aa");
        names.add("aaa");
        names.add("_aaa");
        names.add("jcr:aa");
        names.add("jcr:bb");
        names.add("jcr:ba");
        names.add("hippo:ba");
        names.add("hippo:aa");
        names.add("hippo:ab");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode(TEST_PATH)) {
            session.getRootNode().getNode(TEST_PATH).remove();
        }
        testPath = session.getRootNode().addNode(TEST_PATH);
        session.save();
    }

    @Test
    public void testSortingNamesXpath() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();

        String xpath = "/jcr:root" + testPath.getPath() + "//*order by @jcr:name descending";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();

        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }

    }
    
    @Test
    public void testSortingNamesSql() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();

        String sql = "SELECT * FROM nt:base WHERE jcr:path LIKE '/testnodes/%' ORDER BY jcr:name DESC";
         QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(sql, "sql").execute();

        String prev = null;
        for (NodeIterator nodeit = queryResult.getNodes(); nodeit.hasNext();) {
            String nodename = nodeit.nextNode().getName();
            if (prev != null) {
                assertTrue(prev.compareTo(nodename) <= 0);
            }
            prev = nodename;
        }

    }

}
