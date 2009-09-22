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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.hippoecm.repository.standardworkflow.ChangeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hippoecm.repository.standardworkflow.Change;

public class UpdateAllContentTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(UpdateAllContentTest.class);

    private String base;
    
    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(true);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        session.refresh(false);
        super.tearDown();
    }
    
  @Test
  public void doTest() throws Exception {
    String cnd1 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
	+ "<hippostd='http://www.hippoecm.org/hippostd/nt/1.3'>"
	+ "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
	+ "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.5'>"
	+ ""
	+ "[defaultcontent:basedocument] > hippo:document, hippostd:publishable, hippostd:publishableSummary"
	+ ""
	+ "[defaultcontent:address]"
	+ "- defaultcontent:street (string)"
	+ "- defaultcontent:number (long)"
	+ ""
	+ "[defaultcontent:article] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:event] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:address (defaultcontent:address)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:news] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:internallink (hippo:facetselect)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:overview] > defaultcontent:basedocument"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:query (hippo:query)"
	+ "- defaultcontent:introduction (string)"
	+ "";
    String cnd2 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
	+ "<hippostd='http://www.hippoecm.org/hippostd/nt/1.3'>"
	+ "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
	+ "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.6'>"
	+ ""
	+ "[defaultcontent:basedocument] > hippo:document, hippostd:publishable, hippostd:publishableSummary"
	+ ""
	+ "[defaultcontent:address]"
	+ "- defaultcontent:street (string)"
	+ "- defaultcontent:number (long)"
	+ ""
	+ "[defaultcontent:article] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:event] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:address (defaultcontent:address)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:news] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:internallink (hippo:facetselect)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:overview] > defaultcontent:basedocument"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:query (hippo:query)"
	+ "- defaultcontent:introduction (string)"
	+ "";
    String cnd3 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
	+ "<hippostd='http://www.hippoecm.org/hippostd/nt/1.3'>"
	+ "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
	+ "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.7'>"
	+ ""
	+ "[defaultcontent:basedocument] > hippo:document, hippostd:publishable, hippostd:publishableSummary"
	+ ""
	+ "[defaultcontent:address]"
	+ "- defaultcontent:street (string)"
	+ "- defaultcontent:number (long)"
	+ ""
	+ "[defaultcontent:article] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
        + "- defaultcontent:a (string) mandatory"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:event] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:address (defaultcontent:address)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:news] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:internallink (hippo:facetselect)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:overview] > defaultcontent:basedocument"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:query (hippo:query)"
	+ "- defaultcontent:introduction (string)"
	+ "";
    String cnd4 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
	+ "<hippostd='http://www.hippoecm.org/hippostd/nt/1.3'>"
	+ "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
	+ "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.8'>"
	+ ""
	+ "[defaultcontent:basedocument] > hippo:document, hippostd:publishable, hippostd:publishableSummary"
	+ ""
	+ "[defaultcontent:address]"
	+ "- defaultcontent:street (string)"
	+ "- defaultcontent:number (long)"
	+ ""
	+ "[defaultcontent:article] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "+ defaultcontent:ydob (hippostd:html) mandatory"
	+ "- defaultcontent:title (string)"
        + "- defaultcontent:a (string) mandatory"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:event] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:address (defaultcontent:address)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:news] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:internallink (hippo:facetselect)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:overview] > defaultcontent:basedocument"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:query (hippo:query)"
	+ "- defaultcontent:introduction (string)"
	+ "";
    String cnd5 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
	+ "<hippostd='http://www.hippoecm.org/hippostd/nt/1.3'>"
	+ "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
	+ "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.9'>"
	+ ""
	+ "[defaultcontent:basedocument] > hippo:document, hippostd:publishable, hippostd:publishableSummary"
	+ ""
	+ "[defaultcontent:address]"
	+ "- defaultcontent:street (string)"
	+ "- defaultcontent:number (long)"
	+ ""
	+ "[defaultcontent:article] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "+ defaultcontent:ydob (hippostd:html) mandatory"
	+ "- defaultcontent:title (string)"
        + "- defaultcontent:a (string) mandatory"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:event] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:address (defaultcontent:address)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:news] > defaultcontent:basedocument"
	+ "+ defaultcontent:body (hippostd:html)"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:internallink (hippo:facetselect)"
	+ "+ defaultcontent:date (hippostd:date)"
	+ "- defaultcontent:introduction (string)"
	+ ""
	+ "[defaultcontent:overview] > defaultcontent:basedocument"
	+ "- defaultcontent:title (string)"
	+ "+ defaultcontent:query (hippo:query)"
	+ "- defaultcontent:introduction (string)"
	+ "";
    
    Map<String, List<Change>> cargo1 = new HashMap<String, List<Change>>();
    cargo1.put("defaultcontent:address", new LinkedList<Change>());
    cargo1.put("defaultcontent:article", new LinkedList<Change>());
    cargo1.put("defaultcontent:basedocument", new LinkedList<Change>());
    cargo1.put("defaultcontent:event", new LinkedList<Change>());
    cargo1.put("defaultcontent:news", new LinkedList<Change>());
    cargo1.put("defaultcontent:overview", new LinkedList<Change>());

    List<Change> list;
    Map<String, List<Change>> cargo3 = new HashMap<String, List<Change>>();
    cargo3.put("defaultcontent:address", new LinkedList<Change>());
    cargo3.put("defaultcontent:article", list = new LinkedList<Change>());
    cargo3.put("defaultcontent:basedocument", new LinkedList<Change>());
    cargo3.put("defaultcontent:event", new LinkedList<Change>());
    cargo3.put("defaultcontent:news", new LinkedList<Change>());
    cargo3.put("defaultcontent:overview", new LinkedList<Change>());
    list.add(new Change(ChangeType.ADDITION,"defaultcontent:a","/hippo:namespaces/defaultcontent/article/hipposysedit:prototypes/hipposysedit:prototype[2]/defaultcontent:a"));

    Map<String, List<Change>> cargo4 = new HashMap<String, List<Change>>();
    cargo4.put("defaultcontent:address", new LinkedList<Change>());
    cargo4.put("defaultcontent:article", list = new LinkedList<Change>());
    cargo4.put("defaultcontent:basedocument", new LinkedList<Change>());
    cargo4.put("defaultcontent:event", new LinkedList<Change>());
    cargo4.put("defaultcontent:news", new LinkedList<Change>());
    cargo4.put("defaultcontent:overview", new LinkedList<Change>());
    list.add(new Change(ChangeType.ADDITION,"defaultcontent:ydob","/hippo:namespaces/defaultcontent/article/hipposysedit:prototypes/hipposysedit:prototype[3]/defaultcontent:ydob"));

    Node editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    Workflow wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    assertTrue(wf instanceof NamespaceWorkflow);
    ((NamespaceWorkflow) wf).updateModel(cnd1, cargo1);

    session.logout();
    session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

    editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    ((NamespaceWorkflow) wf).updateModel(cnd2, cargo1);

    session.logout();
    session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

    editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    Node newPrototype = editorNamespaceFolder.getNode("article").getNode("hipposysedit:prototypes");
    newPrototype = newPrototype.addNode("hipposysedit:prototype","nt:unstructured");
    newPrototype.addMixin("hippostd:publishableSummary");
    newPrototype.addMixin("hippo:harddocument");
    newPrototype.addMixin("hippostd:publishable");
    newPrototype.setProperty("defaultcontent:a","Test");
    newPrototype.setProperty("defaultcontent:introduction","introduction");
    newPrototype.setProperty("defaultcontent:title","title");
    newPrototype.setProperty("hippostd:state","unpublished");
    newPrototype.setProperty("hippostd:stateSummary","new");
    Node newPrototypeChild = newPrototype.addNode("defaultcontent:body","hippostd:html");
    newPrototypeChild.setProperty("hippostd:content","&lt;html&gt;&lt;body&gt;&lt;/body&gt;&lt;/html&gt;");
    session.save();
    wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    ((NamespaceWorkflow) wf).updateModel(cnd3, cargo3);

    session.logout();
    session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

    editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    newPrototype = editorNamespaceFolder.getNode("article").getNode("hipposysedit:prototypes");
    newPrototype = newPrototype.addNode("hipposysedit:prototype","nt:unstructured");
    newPrototype.addMixin("hippostd:publishableSummary");
    newPrototype.addMixin("hippo:harddocument");
    newPrototype.addMixin("hippostd:publishable");
    newPrototype.setProperty("defaultcontent:a","Test");
    newPrototype.setProperty("defaultcontent:introduction","introduction");
    newPrototype.setProperty("defaultcontent:title","title");
    newPrototype.setProperty("hippostd:state","unpublished");
    newPrototype.setProperty("hippostd:stateSummary","new");
    newPrototypeChild = newPrototype.addNode("defaultcontent:body","hippostd:html");
    newPrototypeChild.setProperty("hippostd:content","&lt;html&gt;&lt;body&gt;&lt;/body&gt;&lt;/html&gt;");
    newPrototypeChild = newPrototype.addNode("defaultcontent:ydob","hippostd:html");
    newPrototypeChild.setProperty("hippostd:content","&lt;html&gt;&lt;body&gt;&lt;/body&gt;&lt;/html&gt;");
    session.save();
    wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    ((NamespaceWorkflow) wf).updateModel(cnd4, cargo4);

    session.logout();
    session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

    editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    ((NamespaceWorkflow) wf).updateModel(cnd5, cargo1);
  }
}
