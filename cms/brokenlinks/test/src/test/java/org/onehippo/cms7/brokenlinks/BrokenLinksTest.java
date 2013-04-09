/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Stack;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.test.TestWorkflow;
import org.onehippo.cms7.test.TestWorkflowImpl;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrokenLinksTest extends RepositoryTestCase {


    private static final Logger log = LoggerFactory.getLogger(BrokenLinksTest.class);
    private static final String GOOD = TestHttpClient.OK_URL;
    private static final String BAD = TestHttpClient.BAD_URL;
    private static final String TEXT1 = "<html><body>\n<H1>Wat is Lorem Ipsum</H1>\n<B><A HREF=\""+GOOD+"\">Lorem Ipsum</A></B> is slechts een proeftekst uit het drukkerij- en zetterijwezen. Lorem Ipsum is de standaard proeftekst in deze bedrijfstak sinds de 16e eeuw, toen een onbekende drukker een zethaak met letters nam en ze door elkaar husselde om een font-catalogus te maken. Het heeft niet alleen vijf eeuwen overleefd maar is ook, vrijwel onveranderd, overgenomen in elektronische letterzetting. Het is in de jaren '60 populair geworden met de introductie van Letraset vellen met Lorem Ipsum passages en meer recentelijk door desktop publishing software zoals Aldus PageMaker die versies van Lorem Ipsum bevatten.\n<H1>Waar komt het vandaan?</H1>\n<P>In tegenstelling tot wat algemeen aangenomen wordt is Lorem Ipsum niet zomaar willekeurige tekst. het heeft zijn wortels in een stuk klassieke <A HREF=\""+BAD+"\">latijnse</A> literatuur uit 45 v.Chr. en is dus meer dan 2000 jaar oud. Richard McClintock, een professor <A HREF=\""+BAD+"\">latijn</A> aan de Hampden-Sydney College in Virginia, heeft één van de meer obscure <A HREF=\""+BAD+"\">latijnse</A> woorden, consectetur, uit een Lorem Ipsum passage opgezocht, en heeft tijdens het zoeken naar het woord in de klassieke literatuur de onverdachte bron ontdekt. Lorem Ipsum komt uit de secties 1.10.32 en 1.10.33 van &quot;de Finibus Bonorum et Malorum&quot; (De uitersten van goed en kwaad) door Cicero, geschreven in 45 v.Chr. Dit boek is een verhandeling over de theorie der ethiek, erg populair tijdens de renaissance. De eerste regel van Lorem Ipsum, &quot;Lorem ipsum dolor sit amet..&quot;, komt uit een zin in sectie 1.10.32.</P>\n<P>Het standaard stuk van Lorum Ipsum wat sinds de 16e eeuw wordt gebruikt is hieronder, voor wie er interesse in heeft, weergegeven. Secties 1.10.32 en 1.10.33 van &quot;de Finibus Bonorum et Malorum&quot; door Cicero zijn ook weergegeven in hun exacte originele vorm, vergezeld van engelse versies van de 1914 vertaling door H. Rackham.</P><H1>Waarom gebruiken we het?</H1>\n<P>Het is al geruime tijd een bekend gegeven dat een lezer, tijdens het bekijken van de layout van een pagina, afgeleid wordt door de tekstuele inhoud. Het belangrijke punt van het gebruik van Lorem Ipsum is dat het uit een min of meer normale verdeling van letters bestaat, in tegenstelling tot &quot;Hier uw tekst, hier uw tekst&quot; wat het tot min of meer leesbaar nederlands maakt. Veel desktop publishing pakketten en web pagina editors gebruiken tegenwoordig Lorem Ipsum als hun standaard model tekst, en een zoekopdracht naar &quot;lorem ipsum&quot; ontsluit veel websites die nog in aanbouw zijn. Verscheidene versies hebben zich ontwikkeld in de loop van de jaren, soms per ongeluk soms expres (ingevoegde humor en dergelijke).</P>\n<H1>Waar kan ik het vinden?</H1>\n<P>Er zijn vele variaties van passages van Lorem Ipsum beschikbaar maar het merendeel heeft te lijden gehad van wijzigingen in een of andere vorm, door ingevoegde humor of willekeurig gekozen woorden die nog niet half geloofwaardig ogen. Als u een passage uit Lorum Ipsum gaat gebruiken dient u zich ervan te verzekeren dat er niets beschamends midden in de tekst verborgen zit. Alle Lorum Ipsum generators op Internet hebben de eigenschap voorgedefinieerde stukken te herhalen waar nodig zodat dit de eerste echte generator is op internet. Het gebruikt een woordenlijst van 200 <A HREF=\""+GOOD+"\">latijnse</A> woorden gecombineerd met een handvol zinsstructuur modellen om een Lorum Ipsum te genereren die redelijk overkomt. De gegenereerde Lorum Ipsum is daardoor altijd vrij van herhaling, ingevoegde humor of ongebruikelijke woorden etc.</P>\n</body></html>\n";
    private static final String TEXT2 = "<html><body><A HREF=\""+BAD+"one\">One</A><A HREF=\""+BAD+"two\">Two</A><A HREF=\""+BAD+"three\">Three</A><A HREF=\""+BAD+"four\">Four</A><A HREF=\""+BAD+"five\">Five</A><A HREF=\""+BAD+"six\">Six</A><A HREF=\""+BAD+"seven\">Seven</A><A HREF=\""+BAD+"eight\">Eight</A><A HREF=\""+BAD+"nine\">Nine</A><A HREF=\""+BAD+"ten\">Ten</A><A HREF=\""+BAD+"eleven\">Eleven</A><A HREF=\""+BAD+"twelve\">Twelve</A><A HREF=\""+BAD+"thirteen\">Thirteen</A><A HREF=\""+BAD+"fourteen\">Fourteen</A><A HREF=\""+BAD+"fifteen\">Fifteen</A><A HREF=\""+BAD+"sixteen\">Sixteen</A><A HREF=\""+BAD+"seventeen\">Seventeen</A></body></html>";
    private static final String TEXT3 = "<html><body>\n<H1>Wat is Lorem Ipsum</H1>\n<B><A HREF=\""+GOOD+"\">Lorem Ipsum</A></B>\n</body></html>\n";
    private WorkflowManager workflowManager;
    private Stack<Integer> levels;

    @Before
    public void setUp() throws Exception {
        levels = new Stack<Integer>();
        super.setUp();

        // we need to change the default configuration setup for
        // /hippo:configuration/hippo:workflows/brokenlinks/checkbrokenlinks/hipposys:config
        // because it has startPath = /content/documents
        // but we need to scan for the unit tests below /test

        session.getNode("/hippo:configuration/hippo:workflows/brokenlinks/checkbrokenlinks/hipposys:config").setProperty(CheckExternalBrokenLinksConfig.CONFIG_START_PATH, "/test");
        session.getNode("/hippo:configuration/hippo:workflows/brokenlinks/checkbrokenlinks/hipposys:config").setProperty(CheckExternalBrokenLinksConfig.CONFIG_HTTP_CLIENT_CLASSNAME, TestHttpClient.class.getName());

        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.getRootNode().addNode("test", "hippostd:folder").addMixin("hippo:harddocument");
        session.save();
        workflowManager = ((HippoWorkspace)session.getWorkspace()).getWorkflowManager();
        String[] content = new String[] {
            "/test/cfg", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/cfg/cfg", "brokenlinks:config",
                    "jcr:mixinTypes", "hippo:harddocument"
        };
        build(session, content);
        session.save();
        String[] config = new String[] {
            "/hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test", "hippostd:templatequery",
                "hippostd:modify", "./_name",
                "hippostd:modify", "$name",
                "hippostd:modify", "./_node/_name",
                "hippostd:modify", "$name",
                "jcr:language", "xpath",
                "jcr:statement", "/jcr:root/hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test/hippostd:templates/node()",
                "/hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test/hippostd:templates", "hippostd:templates",
                    "/hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test/hippostd:templates/brokenlinks:test", "hippo:handle",
                        "jcr:mixinTypes", "hippo:hardhandle",
                        "/hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test/hippostd:templates/brokenlinks:test/brokenlinks:test", "brokenlinks:test",
                            "jcr:mixinTypes", "hippo:harddocument"
        };
        build(session, config);
        session.save();
        TestWorkflowImpl.invocationCountNoArg = 0;
        TestWorkflowImpl.invocationCountDateArg = 0;

        QueryResult result = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [brokenlinks:brokenlinks]", Query.JCR_SQL2).execute();
        int countDocuments = 0;
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            ++countDocuments;
        }
        assertEquals(0, countDocuments);
    }

    @After
    public void tearDown() throws Exception {
        TestWorkflowImpl.invocationCountNoArg = 0;
        TestWorkflowImpl.invocationCountDateArg = 0;
        Node node;
        node = session.getRootNode();
        if (node.hasNode("test")) {
            node.getNode("test").remove();
        }
        if (node.hasNode("hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test")) {
            node.getNode("hippo:configuration/hippo:queries/hippo:templates/brokenlinks:test").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testBasic() throws RepositoryException, WorkflowException, RemoteException, ClassNotFoundException {
        String[] content = new String[] {
            "/test/doc", "hippo:handle",
            "jcr:mixinTypes", "hippo:hardhandle",
            "/test/doc/doc", "hippo:testdocument",
            "jcr:mixinTypes", "hippo:harddocument",
            "/test/doc/doc/text", "hippostd:html",
            "hippostd:content", "<html><body><a href=\""+BAD+"\">link</a></body></html>"
        };
        build(session, content);
        session.save();
        WorkflowDescriptor wfDesc = workflowManager.getWorkflowDescriptor("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow(wfDesc);
        wf.checkLinks();
        session.refresh(false);
    }

    @Test
    public void testImageReference() throws RepositoryException, WorkflowException, RemoteException, ClassNotFoundException {
        build(session, new String[] {
                    "/test/doc", "hippo:handle",
                    "jcr:mixinTypes", "hippo:hardhandle",
                    "/test/doc/doc", "hippo:testdocument",
                    "jcr:mixinTypes", "hippo:harddocument",
                    "/test/doc/doc/text", "hippostd:html",
                    "hippostd:content", "<html><body><img src=\""+GOOD+"\"/><img src=\""+BAD+"\"/></body></html>"
                });
        session.save();
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        Node node = session.getRootNode().getNode("test/doc");
        assertTrue(node.isNodeType("brokenlinks:brokenlinks"));
        assertTrue(node.hasNode("brokenlinks:link"));
        assertFalse(node.hasNode("brokenlinks:link[2]"));
        assertEquals(BAD, node.getNode("brokenlinks:link").getProperty("brokenlinks:url").getString());
    }

    @Test
    public void testMalformedLink() throws RepositoryException, WorkflowException, RemoteException, ClassNotFoundException {
        String malformedUrl = "http://<";
        String[] content = new String[] {
                "/test/doc", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
                "/test/doc/doc", "hippo:testdocument",
                "jcr:mixinTypes", "hippo:harddocument",
                "/test/doc/doc/text", "hippostd:html",
                "hippostd:content", "<html><body><a href=\""+malformedUrl+"\">link</a></body></html>"
        };
        build(session, content);
        session.save();
        WorkflowDescriptor wfDesc = workflowManager.getWorkflowDescriptor("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow(wfDesc);
        wf.checkLinks();
        session.refresh(false);
        Node node = session.getRootNode().getNode("test/doc");
        assertTrue(node.isNodeType("brokenlinks:brokenlinks"));
        assertTrue(node.hasNode("brokenlinks:link"));
        assertFalse(node.hasNode("brokenlinks:link[2]"));
        assertEquals(malformedUrl, node.getNode("brokenlinks:link").getProperty("brokenlinks:url").getString());
    }

    @Test
    public void testSomeFaultyLinks() throws Exception {
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT1;
            }
        };
        int total = 1;
        for (int count : new int[] {10, 10}) {
            total *= count;
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        assertEquals(total, countBrokenDocuments(session.getRootNode().getNode("test")));
    }

    @Test
    public void testManyFaultyLinks() throws Exception {
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT1;
            }
        };
        int total = 1;
        for (int count : new int[] {10, 10, 10}) {
            total *= count;
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        assertEquals(total, countBrokenDocuments(session.getRootNode().getNode("test")));
    }


    @Test
    public void testManyLinksInDocument() throws Exception {
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT2;
            }
        };
        for (int count : new int[]{10, 10}) {
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        QueryResult result = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [brokenlinks:brokenlinks]", Query.JCR_SQL2).execute();
        int countDocuments = 0;
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node node = iter.nextNode();
            ++countDocuments;
            int countLinks = 0;
            for (NodeIterator links = node.getNodes("brokenlinks:link"); links.hasNext(); links.nextNode()) {
                ++countLinks;
            }
            assertEquals(17, countLinks);
        }
        assertEquals(100, countDocuments);
    }

    @Test
    public void testNoFaultyLinks() throws Exception {
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT3;
            }
        };
        for (int count : new int[]{10, 10}) {
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        QueryResult result = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [brokenlinks:brokenlinks]", Query.JCR_SQL2).execute();
        int countDocuments = 0;
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            ++countDocuments;
        }
        assertEquals(0, countDocuments);
    }

    @Test
    public void testLinksFromBadToGood() throws Exception {
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT1;
            }
        };
        int total = 1;
        for (int count : new int[] {10, 10}) {
            total *= count;
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        QueryResult result = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [brokenlinks:brokenlinks]", Query.JCR_SQL2).execute();
        int countDocuments = 0;
        for (NodeIterator iter = result.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            ++countDocuments;
            assertTrue(child.hasNode(child.getName()));
            child = child.getNode(child.getName());
            assertTrue(child.hasNode("text"));
            child = child.getNode("text");
            assertTrue(child.hasProperty("hippostd:content"));
            child.setProperty("hippostd:content", TEXT3);
        }
        session.save();
        assertEquals(total, countDocuments);
        wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        wf.checkLinks();
        session.refresh(false);
        result = session.getWorkspace().getQueryManager().createQuery("SELECT * FROM [brokenlinks:brokenlinks]", Query.JCR_SQL2).execute();
        countDocuments = 0;
        for (NodeIterator iter = result.getNodes(); iter.hasNext(); iter.nextNode()) {
            ++countDocuments;
        }
        assertEquals(0, countDocuments);
    }

    interface DocumentText {
        public String getTextForDocument(int index);
    }

    private int createDocuments(Node folder, Stack<Integer> levels, int numof, DocumentText documentText) throws MappingException, WorkflowException, RepositoryException, RemoteException {
        Stack<Integer> subLevels = new Stack<Integer>();
        subLevels.addAll(levels);
        int count = subLevels.pop();
        for (int i = 0; i < count; i++) {
            if (subLevels.size() > 0) {
                String subName = "folder" + i;
                final Node childFolder = folder.addNode(subName, "hippostd:folder");
                childFolder.addMixin("hippo:harddocument");
                numof = createDocuments(childFolder, subLevels, numof, documentText);
            } else {
                String subName = "document" + i;
                final Node handleNode = folder.addNode(subName, "hippo:handle");
                handleNode.addMixin("hippo:hardhandle");
                final Node documentNode = handleNode.addNode(subName, "brokenlinks:test");
                documentNode.addMixin("hippo:harddocument");
                Node htmlNode = documentNode.addNode("text", "hippostd:html");
                htmlNode.setProperty("hippostd:content", documentText.getTextForDocument(i));
                ++numof;
            }
        }
        session.save();
        return numof;
    }

    private int countBrokenDocuments(Node node) throws RepositoryException {
        if (node.isNodeType("hippo:document")) {
            int count = 0;
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                count += countBrokenDocuments(child);
            }
            return count;
        } else if (node.isNodeType("hippo:handle")) {
            if (node.isNodeType("brokenlinks:brokenlinks")) {
                return 1;
            } else {
                int count = 0;
                for (NodeIterator iter = node.getNodes(node.getName()); iter.hasNext();) {
                    Node child = iter.nextNode();
                    if (child.isNodeType(("brokenlinks:brokenlinks"))) {
                        count += countBrokenDocuments(child);
                    }
                }
                return count;
            }
        } else {
            return 0;
        }
    }

    @Test
    /**
     * Test that while the broken links checker is running, we can still perform other workflow calls.
     */
    public void testConcurrentSchedule() throws Exception {
        ScheduledCheck checker = new ScheduledCheck();
        DocumentText documents = new DocumentText() {
            public String getTextForDocument(int index) {
                return TEXT1;
            }
        };
        for (int count : new int[] {32, 32}) {
            levels.push(count);
        }
        createDocuments(session.getRootNode().getNode("test"), levels, 0, documents);
        CheckBrokenLinksWorkflow wf = (CheckBrokenLinksWorkflow)workflowManager.getWorkflow("brokenlinks", session.getRootNode().getNode("test/cfg/cfg"));
        checker.start();
        wf.checkLinks();
        session.refresh(false);
        checker.join();
        assertTrue(checker.scheduledTaskDone);
    }

    class ScheduledCheck extends Thread {
        boolean scheduledTaskDone = false;

        public void run() {
            try {
                Session session = server.login("admin", "admin".toCharArray());
                build(session, new String[] {
                            "/test/check", "hippo:handle",
                            "jcr:mixinTypes", "hippo:hardhandle",
                            "/test/check/check", "hippo:document",
                            "jcr:mixinTypes", "hippo:harddocument",});
                Node node = session.getRootNode().getNode("hippo:configuration/hippo:workflows");
                node = node.addNode("test");
                node = node.addNode("test", "hipposys:workflow");
                node.setProperty("hipposys:nodetype", "hippo:document");
                node.setProperty("hipposys:classname", TestWorkflowImpl.class.getName());
                node.setProperty("hipposys:display", "Test workflow");
                session.save();

                if (TestWorkflowImpl.invocationCountNoArg != 0) {
                    log.error("precondition not met");
                }

                TestWorkflow wf = (TestWorkflow)((HippoWorkspace)session.getWorkspace()).getWorkflowManager().getWorkflow("test", session.getRootNode().getNode("test/check/check"));
                wf.schedule(new Date(System.currentTimeMillis() + 500));

                int nretries = 100;
                while (TestWorkflowImpl.invocationCountNoArg != 1 && nretries-- > 0) {
                    Thread.sleep(500);
                }

                if (TestWorkflowImpl.invocationCountNoArg != 1) {
                    log.error("postcondition not met");
                } else {
                    scheduledTaskDone = true;
                }

                node = session.getRootNode().getNode("hippo:configuration/hippo:workflows");
                if (node.hasNode("test")) {
                    node.getNode("test").remove();
                }
                session.save();
                session.logout();
            } catch (InterruptedException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                scheduledTaskDone = false;
            } catch (MappingException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                scheduledTaskDone = false;
            } catch (WorkflowException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                scheduledTaskDone = false;
            } catch (RepositoryException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                scheduledTaskDone = false;
            } catch (RemoteException ex) {
                log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                scheduledTaskDone = false;
            }
        }
    }

}
