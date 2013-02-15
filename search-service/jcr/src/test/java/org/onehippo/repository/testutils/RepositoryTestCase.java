/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.testutils;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: delete this class
 * Code copied from the repository-testutils 2.23.05-SNAPSHOT
 * When the testutils can be included as a dependency, this class can be deleted.
 */

/**
 * Abstract base class for writing tests against repository.
 *
 * Your unit test should follow the following pattern:
 *
        <code>
        public class SampleTest extends org.onehippo.repository.testutils.RepositoryTestCase {
            public void setUp() throws Exception {
                super.setUp();
                // your code here
            }
            public void tearDown() throws Exception {
                // your code here
                super.tearDown();
            }
        }
        </code>
 */
public abstract class RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(RepositoryTestCase.class);

    /**
     * System property indicating whether to use the same repository server across all
     * test invocations. If this property is false or not present a new repository will be created
     * for every test. Sometimes this is unavoidable because you need a clean repository. If you don't
     * then your test performance will benefit greatly by setting this property.
     */
    private static final String KEEPSERVER_PROP = "org.onehippo.repository.test.keepserver";

    protected static final String SYSTEMUSER_ID = "admin";
    protected static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected static HippoRepository external = null;
    protected static HippoRepository background = null;
    protected HippoRepository server = null;
    protected Session session = null;

    public RepositoryTestCase() {
    }

    static private void delete(File path) {
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                for (final File file : files) {
                    delete(file);
                }
            }
            path.delete();
        }
    }

    public static void clear() {
        if (background != null) {
            background.close();
            background = null;
        }
        String[] files = new String[] { ".lock", "repository", "version", "workspaces" };
        for (final String file : files) {
            delete(new File(file));
        }
    }

    static public void setRepository(HippoRepository repository) {
        external = repository;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpClass(false);
    }

    protected static void setUpClass(boolean clearRepository) throws Exception {
        if (clearRepository)
            clear();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        tearDownClass(false);
    }

    public static void tearDownClass(boolean clearRepository) throws Exception {
        if (clearRepository)
            clear();
    }

    @Before
    public void setUp() throws Exception {
        setUp(false);
    }

    protected void setUp(boolean clearRepository) throws Exception {
        if (external != null) {
            if (clearRepository) {
                throw new IllegalArgumentException("Cannot clear the repository in a remote test");
            }
            server = external;
        } else {
            if (clearRepository) {
                clear();
            }
            if (Boolean.getBoolean(KEEPSERVER_PROP)) {
                if (background != null) {
                    server = background;
                } else {
                    server = background = HippoRepositoryFactory.getHippoRepository();
                }
            } else {
                server = HippoRepositoryFactory.getHippoRepository();
            }
        }
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
            session.refresh(false);
        }
    }

    @After
    public void tearDown() throws Exception {
        tearDown(false);
    }

    public void tearDown(boolean clearRepository) throws Exception {
        if (session != null) {
            session.refresh(false);
            while (session.getRootNode().hasNode("test")) {
                session.getRootNode().getNode("test").remove();
                session.save();
            }
            session.logout();
            session = null;
        }
        if (external == null && server != null) {
            if (!Boolean.getBoolean(KEEPSERVER_PROP)) {
                server.close();
            }
            server = null;
        }
        if (clearRepository) {
            clear();
        }
    }

    protected void build(Session session, String[] contents) throws RepositoryException {
        Node node = null;
        for (int i = 0; i < contents.length; i += 2) {
            if (contents[i].startsWith("/")) {
                String path = contents[i].substring(1);
                node = session.getRootNode();
                if (path.contains("/")) {
                    node = node.getNode(path.substring(0, path.lastIndexOf("/")));
                    path = path.substring(path.lastIndexOf("/") + 1);
                }
                node = node.addNode(path, contents[i + 1]);
            } else {
                PropertyDefinition propDef = null;
                PropertyDefinition[] propDefs = node.getPrimaryNodeType().getPropertyDefinitions();
                for (final PropertyDefinition pd : propDefs) {
                    if (pd.getName().equals(contents[i])) {
                        propDef = pd;
                        break;
                    }
                }
                if ("jcr:mixinTypes".equals(contents[i])) {
                    node.addMixin(contents[i + 1]);
                } else {
                    if (propDef != null && propDef.isMultiple()) {
                        Value[] values;
                        if (node.hasProperty(contents[i])) {
                            values = node.getProperty(contents[i]).getValues();
                            Value[] newValues = new Value[values.length + 1];
                            System.arraycopy(values, 0, newValues, 0, values.length);
                            values = newValues;
                        } else {
                            if (contents[i + 1] != null)
                                values = new Value[1];
                            else
                                values = new Value[0];
                        }
                        if (values.length > 0) {
                            if (propDef.getRequiredType() == PropertyType.REFERENCE) {
                                String uuid = session.getRootNode().getNode(contents[i + 1]).getIdentifier();
                                values[values.length - 1] = session.getValueFactory().createValue(uuid,
                                        PropertyType.REFERENCE);
                            } else {
                                values[values.length - 1] = session.getValueFactory().createValue(contents[i + 1]);
                            }
                        }
                        node.setProperty(contents[i], values);
                    } else {
                        if (propDef != null && propDef.getRequiredType() == PropertyType.REFERENCE) {
                            node.setProperty(
                                    contents[i],
                                    session.getValueFactory()
                                            .createValue(
                                                    session.getRootNode().getNode(contents[i + 1].substring(1))
                                                            .getIdentifier(), PropertyType.REFERENCE));
                        } else if ("hippo:docbase".equals(contents[i])) {
                            String docbase;
                            if (contents[i + 1].startsWith("/")) {
                                if (contents[i + 1].substring(1).equals("")) {
                                    docbase = session.getRootNode().getIdentifier();
                                } else {
                                    docbase = session.getRootNode().getNode(contents[i + 1].substring(1))
                                            .getIdentifier();
                                }
                            } else {
                                docbase = contents[i + 1];
                            }
                            node.setProperty(contents[i], session.getValueFactory().createValue(docbase),
                                    PropertyType.STRING);
                        } else {
                            node.setProperty(contents[i], contents[i + 1]);
                        }
                    }
                }
            }
        }
    }

    protected Node traverse(Session session, String path) throws RepositoryException {
        if (path.startsWith("/"))
            path = path.substring(1);
        return ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }
}
