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
package org.hippoecm.repository.servicing;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import junit.framework.TestCase;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNodeType;

public class PathsTest extends TestCase {
    private final static String SVN_ID = "$Id$";

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository repository;
    protected Session session;
    protected Exception firstException = null;
    protected boolean needCleanUp = true;
    
    public void testPaths() throws Exception {
        Exception firstException = null;
        repository = null;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            session = repository.login();
            // the login without credentials cannot save, so no cleanup needed
            needCleanUp = false; 
            Node root = session.getRootNode();
            
            Node sub1 = root.addNode("sub");
            Node sub2 = sub1.addNode("subsub");
            
            assertTrue(root instanceof ServicingNodeImpl);
            assertTrue(sub1 instanceof ServicingNodeImpl);
            assertTrue(sub2 instanceof ServicingNodeImpl);
           
        } catch (RepositoryException ex) {
            fail("unexpected repository exception " + ex.getMessage());
            firstException = ex;
        }
    }

    public void testPathProperty() throws Exception {
        repository = null;
        try {
            repository = HippoRepositoryFactory.getHippoRepository();
            assertNotNull(repository);
            session = repository.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            Node root = session.getRootNode();
            Node sub1 = root.addNode("test");
            Node sub2 = sub1.addNode("sub");
            Node sub3 = sub2.addNode("node", HippoNodeType.NT_DOCUMENT);
            session.save();
            Node node = session.getRootNode().getNode("test/sub/node");
           
            Property prop = node.getProperty(HippoNodeType.HIPPO_PATHS);
            Value[] values = prop.getValues();
            Set valuesSet = new HashSet();
            for (int i = 0; i < values.length; i++) {
                valuesSet.add(values[i].getString());
            }
            assertTrue(values.length == 3);
            assertTrue(valuesSet.contains("/test"));
            assertTrue(valuesSet.contains("/test/sub"));
            assertTrue(valuesSet.contains("/test/sub/node"));
        } catch (RepositoryException ex) {
            fail("unexpected repository exception " + ex.getMessage());
            firstException = ex;
        } 
    }
    
public void tearDown() throws Exception {
        
        if(needCleanUp){
            Node node = session.getRootNode();
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                if (!child.getPath().equals("/jcr:system")) {
                    child.remove();
                }
            }
            session.save();
            if(session != null) {
                session.logout();
            }
        }
        
        boolean exceptionOccurred = false;
        try {
            if (repository != null) {
                repository.close();
                repository = null;
            }
        } catch (Exception ex) {
            if (firstException == null) {
                firstException = ex;
                exceptionOccurred = true;
            }
        }
        
        if (exceptionOccurred || firstException != null){
            throw firstException;
        }
      
    }
    
}
