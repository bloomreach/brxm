/*
 *  Copyright 2009 Hippo.
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

import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.Test;
import org.junit.Ignore;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class ReferringDocumentsTest extends TestCase
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /** height (depth) of the tree of documents to build, together with the fan-out this determins the number of documents the
     * tree holds
     */
    private final int DEPTH = 4;

    /** the number of subfolders and the number of documents for each folder, the total number of html-consisting
     * documents will be FANOUT^DEPTH, currently over 130000
     */
    private final int FANOUT = 10;

    /** number of references to each document, must be smaller than number of documents */
    private final int NUMREFS = 25;

    /** limit of the number of documents to create between session.save() */
    private final int SAVECNT = 150;

    private Vector<String> documents = new Vector<String>((int) Math.pow(FANOUT,DEPTH));

    private Random random;
    private int saveCounter;

    @Override
    public void setUp() throws Exception {
        super.setUp(true);
        Node test = session.getRootNode().addNode("test", "nt:unstructured");

        saveCounter = 0;
        long t1 = System.currentTimeMillis();
        buildContent(DEPTH, test);
        session.save();
        long t2 = System.currentTimeMillis();
        System.err.println("timing building="+(t2-t1)/1000.0+"s");

        random = new Random(478923066);

        saveCounter = 0;
        long t3 = System.currentTimeMillis();
        buildReferences(DEPTH, test);
        session.save();
        long t4 = System.currentTimeMillis();
        System.err.println("timing linking="+(t4-t3)/1000.0+"s");
    }

    @Override
    public void tearDown() throws Exception {
        Node test = session.getRootNode().getNode("test");
        test.remove();
        session.save();
        super.tearDown(true);
    }

    @Test
    public void dummy() {
    }

    @Ignore
    // @Test(timeout=5000)
    public void benchmark() throws RepositoryException {
        /* once to warm up, second one for real */
        String uuid = documents.get(random.nextInt(documents.size()));
        Node document = session.getNodeByUUID(uuid);
        document = document.getNode(document.getName());
        long t1 = System.currentTimeMillis();
        Set<Node> referrers = getReferrers(document);
        System.err.println("result "+referrers.size()+" out of "+documents.size());
        long t2 = System.currentTimeMillis();
        System.err.println("timing references "+(t2-t1)/1000.0);

        uuid = documents.get(random.nextInt(documents.size()));
        document = session.getNodeByUUID(uuid);
        document = document.getNode(document.getName());
        t1 = System.currentTimeMillis();
        referrers = getReferrers(document);
        t2 = System.currentTimeMillis();
        System.err.println("timing references "+(t2-t1)/1000.0);
    }

    private void buildContent(int level, Node base) throws RepositoryException {
        if(level > 1) {
            for(int i=0; i<FANOUT; i++) {
                if(level == DEPTH) {
                    System.err.println(i);
                } else if(level == DEPTH-1) {
                    System.err.println("  "+i);
                }
                Node folder = addFolder(base, "folder"+i);
                buildContent(level-1, folder);
            }
        } else {
            for(int i=0; i<FANOUT; i++) {
                Node document = addDocument(base, "document"+i);
                documents.add(document.getUUID());
                if(++saveCounter >= SAVECNT) {
                    session.save();
                    saveCounter = 0;
                }
            }
        }
    }

    private void buildReferences(int level, Node base) throws RepositoryException {
        if(level > 1) {
            for(int i=0; i<FANOUT; i++) {
                if(level == DEPTH) {
                    System.err.println(i);
                } else if(level == DEPTH-1) {
                    System.err.println("  "+i);
                }
                buildReferences(level-1, base.getNode("folder"+i));
            }
        } else {
            for(int i=0; i<FANOUT; i++) {
                buildReferences(base.getNode("document"+i));
            }
        }
    }

    private void buildReferences(Node handle) throws RepositoryException {
        Node document = handle.getNode(handle.getName());
        Node html = document.getNode("html");
        for(int i=0; i<NUMREFS; i++) {
            String docbase = documents.get(random.nextInt(documents.size()));
            Node link = html.addNode("link"+i, HippoNodeType.NT_FACETSELECT);
            link.setProperty("hippo:docbase", docbase);
            link.setProperty("hippo:facets", new String[0]);
            link.setProperty("hippo:modes", new String[0]);
            link.setProperty("hippo:values", new String[0]);
        }
        if(++saveCounter >= SAVECNT) {
            session.save();
            saveCounter = 0;
        }
    }

    private Node addFolder(Node folder, String name) throws RepositoryException {
        Node document = folder.addNode(name, "hippostd:folder");
        document.addMixin(HippoNodeType.NT_HARDDOCUMENT);
        return document;
    }

    private Node addDocument(Node folder, String name) throws RepositoryException {
        Node handle = folder.addNode(name, HippoNodeType.NT_HANDLE);
        handle.addMixin(HippoNodeType.NT_HARDHANDLE);
        Node document = handle.addNode(name, "hippo:coredocument");
        document.addMixin(HippoNodeType.NT_HARDDOCUMENT);
        Node html = document.addNode("html", "hippostd:html");
        html.setProperty("hippostd:content", "<html><body>Lorem</body></html>");
        return handle;
    }

    public Set<Node> getReferrers(Node document) throws RepositoryException {
        Set<Node> referrers = new TreeSet<Node>(new Comparator<Node>() {
            @Override
            public int compare(Node node1, Node node2) {
                try {
                    return node1.getUUID().compareTo(node2.getUUID());
                } catch(UnsupportedRepositoryOperationException ex) {
                    // cannot happen
                    return 0;
                } catch(RepositoryException ex) {
                    return 0;
                }
            }
        });
        if(!document.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            System.err.println("not a document "+document.getPath()+" "+document.getPrimaryNodeType().getName());
            return null;
        }
        document = ((HippoNode)document).getCanonicalNode();
        Node handle = document.getParent();
        if(!handle.isNodeType(HippoNodeType.NT_HANDLE) || !handle.isNodeType(HippoNodeType.NT_HARDHANDLE)) {
            System.err.println("not a handle "+handle.getPath()+" "+handle.getPrimaryNodeType().getName());
            return null;
        }
        String uuid = handle.getUUID();
        QueryManager queryManager = document.getSession().getWorkspace().getQueryManager();
        String statement = "//*[@hippo:docbase='"+uuid+"']";
        System.err.println("statement "+statement);
        Query query = queryManager.createQuery(statement, Query.XPATH);
        //Query query = queryManager.createQuery("SELECT * FROM nt:base WHERE @hippo:docbase = '"+uuid+"'", Query.SQL);
        QueryResult result = query.execute();
        for(NodeIterator iter=result.getNodes(); iter.hasNext(); ) {
            Node node = iter.nextNode();
            while(node != null && !node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                node = (node.getDepth() > 0 ? node.getParent() : null);
            }
            if(node != null) {
                referrers.add(node);
            }
        }
        return referrers;
    }
}
