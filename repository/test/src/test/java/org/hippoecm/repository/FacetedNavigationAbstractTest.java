/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;

public abstract class FacetedNavigationAbstractTest extends RepositoryTestCase {

    static class Document {
        int docid;
        int x, y, z;
        public Document(int docid) {
            this.docid = docid;
            x = y = z = 0;
        }
    }

    private static String alphabet = "abcde"; // abcdefghijklmnopqrstuvwxyz
    private static int hierDepth = 3;
    private static int saveInterval = 250;
    private static final int defaultNumDocs = 20;
    private int numDocs = -1;
    private static Random rnd;
    private String[] nodeNames;
    protected boolean verbose = false;
    private Map<Integer,Document> documents;

    protected FacetedNavigationAbstractTest() {
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    private void createNodeNames() {
        nodeNames = new String[alphabet.length()];
        for (int i=0; i<alphabet.length(); i++) {
            nodeNames[i] = alphabet.substring(i,i+1);
        }
    }
    
    private void createStructure(Node node, int level) throws ItemExistsException, PathNotFoundException, VersionException,
                                                              ConstraintViolationException, LockException, RepositoryException {
        for (int i=0; i<alphabet.length(); i++) {
            if(verbose) {
                System.out.println(("          ".substring(0,level))+nodeNames[i]);
            }
            Node child = node.addNode(nodeNames[i],"hippo:testdocument");
            child.addMixin("mix:versionable");
            if (level-1 > 0) {
                createStructure(child, level-1);
            }
        }
        if (level-1 == 0) {
            node.getSession().save();
        }
    }

    protected Node getRandomDocNode() throws RepositoryException {
        StringBuffer path = new StringBuffer("test/documents");
        for (int depth = 0; depth < hierDepth; depth++) {
            path.append("/");
            path.append(nodeNames[rnd.nextInt(alphabet.length())]);
        }
        return session.getRootNode().getNode(path.toString());
        
    }
    
    private Map<Integer,Document> fill(Node node) throws RepositoryException {
        Node docs = node.addNode("documents", "nt:unstructured");
        docs.addMixin("mix:referenceable");
        createStructure(docs, hierDepth);
        session.save();
        // don't change seed. Tests depend on it to stay the same
        rnd = new Random(1L);
        Map<Integer, Document> documents = new HashMap<Integer, Document>();
        for (int docid = 0; docid < numDocs; docid++) {
            Document document = new Document(docid);
            
            Node doc = getRandomDocNode();
            doc = doc.addNode(Integer.toString(docid), "hippo:testdocument");
            doc.addMixin("mix:versionable");
            doc.setProperty("docid", Integer.toString(docid));
            if ((document.x = rnd.nextInt(3)) > 0) {
                doc.setProperty("x", "x" + document.x);
            }
            if ((document.y = rnd.nextInt(3)) > 0) {
                doc.setProperty("y", "y" + document.y);
            }
            if ((document.z = rnd.nextInt(3)) > 0) {
                doc.setProperty("z", "z" + document.z);
            }
            if ((docid + 1) % saveInterval == 0) {
                session.save();
            }
            documents.put(Integer.valueOf(docid), document);
        }
        return documents;
    }

    final void createSearchNode(Node node) throws RepositoryException {
        node = node.addNode("navigation");
        node = node.addNode("xyz", HippoNodeType.NT_FACETSEARCH);
        node.setProperty(HippoNodeType.HIPPO_QUERYNAME, "xyz");
        node.setProperty(HippoNodeType.HIPPO_DOCBASE, session.getRootNode().getNode("test/documents").getIdentifier());
        node.setProperty(HippoNodeType.HIPPO_FACETS, new String[] { "x", "y", "z" });
    }
    
    final Node getSearchNode() throws RepositoryException {
        return session.getRootNode().getNode("test/navigation/xyz");
    }
    
    final Node getDocsNode() throws RepositoryException {
        return session.getRootNode().getNode("test/documents");
    }
    
    protected void traverse(Node node) throws RepositoryException {
        if(verbose) {
            if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                System.out.println(node.getPath() + "\t" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            }
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!"jcr:system".equals(child.getName())) {
                traverse(child);
            }
        }
    }

    protected void check(String facetPath, int x, int y, int z)
        throws RepositoryException {
        int realCount = -1;
        Node node = session.getRootNode();
        if(facetPath.startsWith("/"))
            facetPath = facetPath.substring(1); // skip the initial slash
        String[] pathElements = facetPath.split("/");
        try {
            for(int i=0; i<pathElements.length; i++) {
                node = node.getNode(pathElements[i]);
            }
            if(verbose)
                System.out.println(facetPath + "\t" + node.getProperty(HippoNodeType.HIPPO_COUNT).getLong());
            Node nodeResultSet = node.getNode(HippoNodeType.HIPPO_RESULTSET);
            NodeIterator iter = nodeResultSet.getNodes();
            realCount = 0;
            while(iter.hasNext()) {
                Node child = iter.nextNode();
                ++realCount;
                if(verbose) {
                    System.out.print("\t" + child.getProperty("docid").getString());
                    System.out.print("\t" + (child.hasProperty("x") ? child.getProperty("x").getString().substring(1) : "0"));
                    System.out.print("\t" + (child.hasProperty("y") ? child.getProperty("y").getString().substring(1) : "0"));
                    System.out.print("\t" + (child.hasProperty("z") ? child.getProperty("z").getString().substring(1) : "0"));
                    System.out.println();
                }
            }
            if(node.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                long obtainedCount = (int) node.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
                assertEquals("counted and indicated mismatch on "+facetPath, realCount, obtainedCount);
            }
        } catch(PathNotFoundException ex) {
            System.err.println("PathNotFoundException: "+ex.getMessage());
            ex.printStackTrace(System.err);
            realCount = 0;
            if(verbose)
                System.out.println(facetPath + "\tno results");
        }
        int checkedCount = 0;
        if(verbose)
            System.out.println();
        for(Document document : documents.values()) {
            if((x == 0 || x == document.x) && (y == 0 || y == document.y) && (z == 0 || z == document.z)) {
                if(verbose)
                    System.out.println("\t"+document.docid+"\t"+document.x+"\t"+document.y+"\t"+document.z);
                ++checkedCount;
            }
        }
        if(verbose)
            System.out.println(facetPath + "\t" + realCount + "\t" + checkedCount);
        assertEquals("counted and reference mismatch on "+facetPath, checkedCount, realCount);
    }

    final void commonStart(int numDocs) throws RepositoryException {
        this.numDocs = numDocs;
        Node test = session.getRootNode().addNode("test");
        createNodeNames();
        documents = fill(test);
        // do save and refresh to make sure the uuid is generated
        session.save();
        session.refresh(false);
        createSearchNode(test);
        session.save();
        session.refresh(false);
    }
    
    final void commonStart() throws RepositoryException {
        /**
         * DefaultNumDocs results in:
/test/navigation/xyz    25
/test/navigation/xyz/x1 8
/test/navigation/xyz/x1/y1      3
/test/navigation/xyz/x1/y1/z1   1
/test/navigation/xyz/x1/y1/z1/hippo:resultset   1
/test/navigation/xyz/x1/y1/hippo:resultset      3
/test/navigation/xyz/x1/y2      3
/test/navigation/xyz/x1/y2/z1   1
/test/navigation/xyz/x1/y2/z1/hippo:resultset   1
/test/navigation/xyz/x1/y2/z2   1
/test/navigation/xyz/x1/y2/z2/hippo:resultset   1
/test/navigation/xyz/x1/y2/hippo:resultset      3
/test/navigation/xyz/x1/hippo:resultset 8
/test/navigation/xyz/x2 6
/test/navigation/xyz/x2/y1      6
/test/navigation/xyz/x2/y1/z1   2
/test/navigation/xyz/x2/y1/z1/hippo:resultset   2
/test/navigation/xyz/x2/y1/z2   2
/test/navigation/xyz/x2/y1/z2/hippo:resultset   2
/test/navigation/xyz/x2/y1/hippo:resultset      6
/test/navigation/xyz/x2/hippo:resultset 6
/test/navigation/xyz/hippo:resultset    25
         */
        commonStart(defaultNumDocs);
    }

    final void commonEnd() throws RepositoryException {
    }

    public boolean getVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
