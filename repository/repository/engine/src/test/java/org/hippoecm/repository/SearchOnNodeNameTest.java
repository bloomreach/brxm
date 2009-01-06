package org.hippoecm.repository;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.junit.Before;
import org.junit.Test;

public class SearchOnNodeNameTest extends TestCase {
    @SuppressWarnings("unused")
   
    private final static String TEST_PATH = "testnodes";
    private Node testPath;

    private final static List<String> names = new ArrayList<String>();

    static {
        names.add("foobarlux");
        names.add("fooluxbar");
        names.add("barfoolux");
        names.add("barluxfoo");
        names.add("luxbarfoo");
        names.add("luxfoo bar");
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
    public void testSearchNodeNameInNodeScope() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();
        String xpath = "//*[jcr:contains(.,'luxfoo')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        assertTrue(queryResult.getNodes().getSize() > 0);
        
    }
    
    @Test
    public void testSearchNodeNameInProp() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();
        String xpath = "//*[jcr:contains(@hippo:_localname,'luxfoo')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        assertTrue(queryResult.getNodes().getSize() > 0);
        
    }
    
    
    @Test
    public void testSearchWildcardNodeNameInProp() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();
        String xpath = "//*[jcr:contains(@hippo:_localname,'luxfoo*')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();

        assertTrue(queryResult.getNodes().getSize() > 0);
    }
    
    @Test
    public void testSearchWildcardNodeNameInNodeScope() throws RepositoryException {
        for (String name : names) {
            testPath.addNode(name);
        }
        session.save();
        String xpath = "//*[jcr:contains(.,'luxfoo*')]";
        QueryResult queryResult = session.getWorkspace().getQueryManager().createQuery(xpath, "xpath").execute();
        
        assertTrue(queryResult.getNodes().getSize() > 0);
        
    }

}
