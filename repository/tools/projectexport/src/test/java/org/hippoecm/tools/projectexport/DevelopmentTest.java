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
package org.hippoecm.tools.projectexport;

import java.util.Collection;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.standardworkflow.RepositoryWorkflow;

import org.junit.Test;
import org.junit.Ignore;

public class DevelopmentTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    @Test
    public void dummy() {
    }

    @Ignore
    public void test() throws Exception, NotExportableException {
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
        Session session = repository.login("admin", "admin".toCharArray());
        
        RepositoryWorkflow wf = (RepositoryWorkflow) ((HippoWorkspace)session.getWorkspace()).getWorkflowManager().getWorkflow("internal", session.getRootNode());
        wf.createNamespace("test", "http://www.hippo.nl/test/nt/1.0");
        session.refresh(false);
        session.getRootNode().addNode("newcontent");
        Node n = session.getRootNode().getNode("hippo:configuration/hippo:frontend/login/login").addNode("test","frontend:plugin");
        n.setProperty("wicket.id","test");
        n.setProperty("plugin.class","test");
        session.save();
        
        ExportEngine export = new ExportEngine(session);
        //export.selectProject("Gallery Addon");
        //export.exportProject(new FileOutputStream("test.zip"));
        //print(export.elements, 1);

        session.logout();
        repository.close();
    }
    
    private void print(Collection<Element> elements, int level) {
        for(Element element : elements) {
            for(int i=0; i<level; i++)
                System.err.print("  ");
            if(element instanceof Element.ProjectElement) {
                System.err.print("project "+element.getFullName()+" ");
                System.err.println();
                print(((Element.ProjectElement)element).elements, level+1);
            } else if(element instanceof Element.NamespaceElement) {
                System.err.print("namespace "+element.getFullName());
                Element.NamespaceElement namespace = (Element.NamespaceElement) element;
                System.err.println(" file="+namespace.file+" cnd="+namespace.cnd+" prefix="+namespace.prefix+" uri="+namespace.uri);
            } else if(element instanceof Element.ContentElement) {
                System.err.print("content "+element.getFullName()+" ");
                System.err.println(((Element.ContentElement)element).path);                
            } else {
                System.err.println("unknown "+element.getFullName());
            }
        }
    }
}
