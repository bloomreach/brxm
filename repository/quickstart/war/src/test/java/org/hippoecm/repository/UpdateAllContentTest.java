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
import org.junit.Ignore;
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
        + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>"
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
        + "+ defaultcontent:internallink (hippo:mirror)"
        + "+ defaultcontent:date (hippostd:date)"
        + "- defaultcontent:introduction (string)"
        + ""
        + "[defaultcontent:overview] > defaultcontent:basedocument"
        + "- defaultcontent:title (string)"
        + "+ defaultcontent:query (hippo:query)"
        + "- defaultcontent:introduction (string)"
        + "";
    String cnd2 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
        + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>"
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
        + "+ defaultcontent:internallink (hippo:mirror)"
        + "+ defaultcontent:date (hippostd:date)"
        + "- defaultcontent:introduction (string)"
        + ""
        + "[defaultcontent:overview] > defaultcontent:basedocument"
        + "- defaultcontent:title (string)"
        + "+ defaultcontent:query (hippo:query)"
        + "- defaultcontent:introduction (string)"
        + "";
    String cnd3 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
        + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>"
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
        + "+ defaultcontent:internallink (hippo:mirror)"
        + "+ defaultcontent:date (hippostd:date)"
        + "- defaultcontent:introduction (string)"
        + ""
        + "[defaultcontent:overview] > defaultcontent:basedocument"
        + "- defaultcontent:title (string)"
        + "+ defaultcontent:query (hippo:query)"
        + "- defaultcontent:introduction (string)"
        + "";
    String cnd4 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
        + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>"
        + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
        + "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.10'>"
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
        + "+ defaultcontent:internallink (hippo:mirror)"
        + "+ defaultcontent:date (hippostd:date)"
        + "- defaultcontent:introduction (string)"
        + ""
        + "[defaultcontent:overview] > defaultcontent:basedocument"
        + "- defaultcontent:title (string)"
        + "+ defaultcontent:query (hippo:query)"
        + "- defaultcontent:introduction (string)"
        + "";
    String cnd5 = "<nt='http://www.jcp.org/jcr/nt/1.0'>"
        + "<hippostd='http://www.onehippo.org/jcr/hippostd/nt/2.0'>"
        + "<hippo='http://www.onehippo.org/jcr/hippo/nt/2.0'>"
        + "<defaultcontent='http://www.hippoecm.org/defaultcontent/nt/1.11'>"
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
        + "+ defaultcontent:internallink (hippo:mirror)"
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
    list.add(new Change(ChangeType.ADDITION,"defaultcontent:ydob","/hippo:namespaces/defaultcontent/article/hipposysedit:prototypes/hipposysedit:prototype[2]/defaultcontent:ydob"));

    Node editorNamespaceFolder = session.getRootNode().getNode("hippo:namespaces/defaultcontent");
    WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
    Workflow wf = workflowManager.getWorkflow("editor", editorNamespaceFolder);
    assertTrue(wf instanceof NamespaceWorkflow);
    System.err.println("START");
    long t1 = System.currentTimeMillis();
    ((NamespaceWorkflow) wf).updateModel(cnd1, cargo1);
    long t2 = System.currentTimeMillis();

    session.logout();
    session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
    System.err.println("\n\n\n\n\n\n\n"+(t2-t1)+"\n\n\n\n\n\n\n\n");
  }
}
