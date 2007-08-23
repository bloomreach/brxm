/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.io.IOException;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public abstract class FacetedNavigationAbstractTest extends TestCase {

    static class Document {
        int docid;
        int x, y, z;
        public Document(int docid) {
            this.docid = docid;
            x = y = z = 0;
        }
    }

    private static String alphabet = "abcde"; // abcdefghijklmnopqrstuvwxyz
    private static int hierDepth = 1;
    private static int saveInterval = 30;
    private final static int defaultNumDocs = 300;
    protected int numDocs = -1;

    private String[] nodeNames;
    private HippoRepository server;
    private Session session;
    private boolean verbose = false;
    private Map<Integer,Document> documents;

    public FacetedNavigationAbstractTest() {
        nodeNames = new String[alphabet.length()];
        for (int i=0; i<alphabet.length(); i++) {
            nodeNames[i] = alphabet.substring(i,i+1);
        }
        numDocs = defaultNumDocs;
    }

    private void createStructure(Node node, int level) throws ItemExistsException, PathNotFoundException, VersionException,
                                                              ConstraintViolationException, LockException, RepositoryException {
        for (int i=0; i<alphabet.length(); i++) {
            if(verbose)
                System.out.println(("          ".substring(0,level))+nodeNames[i]);
            Node child = node.addNode(nodeNames[i]);
            if (level-1 > 0) {
                createStructure(child, level-1);
            }
        }
        if (level-1 == 0) {
            node.getSession().save();
        }
    }

    protected Map<Integer,Document> fill() throws RepositoryException {
        Node node = session.getRootNode();

        if (!node.hasNode("documents")) {
            node.addNode("documents");
        }
        if (!node.hasNode("navigation")) {
            node.addNode("navigation");
        }

        node = node.getNode("documents");
        createStructure(node, hierDepth);
        session.save();
        Map<Integer,Document> documents = new HashMap<Integer,Document>();
        for (int docid=0; docid<numDocs; docid++) {
            Random rnd = new Random(docid);
            Document document = new Document(docid);
            Node child = node;
            for (int depth=0; depth<hierDepth; depth++)
                child = child.getNode(nodeNames[rnd.nextInt(alphabet.length())]);
            child = child.addNode(Integer.toString(docid));
            child.setProperty("docid",docid);
            if ((document.x = rnd.nextInt(3)) > 0) {
                child.setProperty("x","x"+document.x);
            }
            if ((document.y = rnd.nextInt(3)) > 0) {
                child.setProperty("y","y"+document.y);
            }
            if ((document.z = rnd.nextInt(3)) > 0) {
                child.setProperty("z","z"+document.z);
            }
            if ((docid+1) % saveInterval == 0) {
                session.save();
            }
            documents.put(new Integer(docid), document);
        }
        session.save();
        return documents;
    }

    protected void traverse(Node node) throws RepositoryException {
        if(verbose) {
            if(node.hasProperty("hippo:count")) {
                System.out.println(node.getPath() + "\t" + node.getProperty("hippo:count").getLong());
            }
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (!child.getPath().equals("/jcr:system")) {
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
                System.out.println(facetPath + "\t" + node.getProperty("hippo:count"));
            node = node.getNode("hippo:resultset");
            NodeIterator iter = node.getNodes();
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
            if(node.hasProperty("hippo:count")) {
                long obtainedCount = (int) node.getProperty("hippo:count").getLong();
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

    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login();
        session.getRootNode().addNode("navigation");
        session.save();
        documents = fill();
    }

    public void tearDown() throws RepositoryException {
        if(session.getRootNode().hasNode("navigation")) {
            session.getRootNode().getNode("navigation").remove();
        }
        if(session.getRootNode().hasNode("navigation")) {
            session.getRootNode().getNode("documents").remove();
        }
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    protected Node commonStart() throws RepositoryException {
        Node node = session.getRootNode().getNode("navigation");
        node = node.addNode("xyz","hippo:facetsearch");
        node.setProperty("hippo:queryname","xyz");
        node.setProperty("hippo:docbase","documents");
        node.setProperty("hippo:facets",new String[] { "x", "y", "z" });
        return node;
    }
    protected void commonEnd() throws RepositoryException {
        session.refresh(false);
    }

    public void testPerformance() throws RepositoryException, IOException {
        Node node = commonStart();
        long count, tBefore, tAfter;
        tBefore = System.currentTimeMillis();
        count = node.getNode("x1").getNode("y2").getNode("z2").getNode("hippo:resultset").getProperty("hippo:count").getLong();
        tAfter = System.currentTimeMillis();
        commonEnd();
    }
}
