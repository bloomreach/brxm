/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.testutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.commons.io.FileUtils;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * System property indicating whether to use the same repository server across all
     * test invocations. If this property is false or not present a new repository will be created
     * for every test. Sometimes this is unavoidable because you need a clean repository. If you don't
     * then your test performance will benefit greatly by setting this property.
     */
    private static final String KEEPSERVER_SYSPROP = "org.onehippo.repository.test.keepserver";

    protected static final String SYSTEMUSER_ID = "admin";
    protected static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();
    protected static final Credentials CREDENTIALS = new SimpleCredentials(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

    protected static HippoRepository external = null;
    protected static HippoRepository background = null;

    private static final String repoPath;

    static {
        String location = System.getProperty("repo.path");
        if (location == null || location.isEmpty()) {
            final File tmpdir = new File(System.getProperty("java.io.tmpdir"));
            final File storage = new File(tmpdir, "repository-" + UUID.randomUUID().toString());
            if (!storage.exists()) {
                storage.mkdir();
            }
            repoPath = storage.getAbsolutePath();
        } else {
            repoPath = location;
        }
    }

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected HippoRepository server = null;
    protected Session session = null;

    /**
     * Check whether repository content & configuration are the same before & after the test.
     */
    private Set<String> topLevelNodes;
    private Collection<JcrState> jcrStates;


    @BeforeClass
    public static void setUpClass() throws Exception {
        if (background == null && external == null) {
            clearRepository();
            background = HippoRepositoryFactory.getHippoRepository(repoPath);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (!Boolean.getBoolean(KEEPSERVER_SYSPROP)) {
            clearRepository();
        }
    }

    public static void clearRepository() {
        if (background != null) {
            background.close();
            background = null;
        }
        final File storage = new File(repoPath);
        String[] paths = new String[] { ".lock", "repository", "version", "workspaces" };
        for (final String path : paths) {
            FileUtils.deleteQuietly(new File(storage, path));
        }
    }

    public static void setRepository(HippoRepository repository) {
        external = repository;
    }

    @Before
    public void setUp() throws Exception {
        this.setUp(false);
    }

    protected void setUp(boolean clearRepository) throws Exception {
        if (clearRepository) {
            clearRepository();
        }
        if (background == null && external == null) {
            background = HippoRepositoryFactory.getHippoRepository();
        }
        if (external != null) {
            server = external;
        } else {
            server = background;
        }
        session = server.login(CREDENTIALS);
        removeNode("/test");

        saveState();
    }

    @After
    public void tearDown() throws Exception {
        this.tearDown(false);
    }

    protected void tearDown(boolean clearRepository) throws Exception {
        removeNode("/test");
        checkState();

        session.refresh(false);
        session.logout();
        session = null;

        if (clearRepository) {
            clearRepository();
        }
    }

    protected void removeNode(final String path) throws RepositoryException {
        while (session != null && session.nodeExists(path)) {
            session.getNode(path).remove();
            session.save();
        }
    }

    protected Collection<String> getPathsToCheck() {
        return Arrays.asList(
                "/hippo:configuration/hippo:derivatives",
                "/hippo:configuration/hippo:domains",
                "/hippo:configuration/hippo:frontend",
                "/hippo:configuration/hippo:groups",
                "/hippo:configuration/hippo:queries",
                "/hippo:configuration/hippo:security",
                "/hippo:configuration/hippo:workflows",
                "/hippo:configuration/hippo:users",
                "/hippo:configuration/hippo:roles");
    }

    private void saveState() throws RepositoryException {
        // save top-level nodes for tearDown validation
        topLevelNodes = new HashSet<String>();
        for (Node node : new NodeIterable(session.getRootNode().getNodes())) {
            topLevelNodes.add(node.getName() + "[" + node.getIndex() + "]");
        }

        jcrStates = new ArrayList<>();
        for (String pathToCheck : getPathsToCheck()) {
            jcrStates.add(new JcrState(pathToCheck, session, log));
        }

    }

    private void checkState() throws Exception {
        session.refresh(false);
        for (Node node : new NodeIterable(session.getRootNode().getNodes())) {
            final boolean removed = topLevelNodes.remove(node.getName() + "[" + node.getIndex() + "]");
            if (!removed) {
                throw new Exception("tearDown found node with name '" + node.getName() + "' in session; this node should be removed in subclass");
            }
        }
        if (topLevelNodes.size() > 0) {
            throw new Exception("tearDown found nodes " + topLevelNodes + " missing");
        }
        for (JcrState jcrState : jcrStates) {
            jcrState.check();
        }

    }

    /**
     * @deprecated since 2.26.00 : use {@link #build(String[], javax.jcr.Session)} instead. Kept for now for the use case
     * that a subclass extended this method
     */
    @Deprecated
    protected void build(Session session,String[] contents) throws RepositoryException {
         build(contents, session);
    }

    public static void build(String[] contents, Session session) throws RepositoryException {
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
                    final String mixins = contents[i + 1];
                    for (String mixin : mixins.split(",")) {
                        node.addMixin(mixin);
                    }
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

    public static String[] mount(String path, String[] content) {
        String[] result = new String[content.length];
        for (int i = 0; i < content.length; i++) {
            String value = content[i];
            if (value.startsWith("/")) {
                result[i] = path + value;
            } else {
                result[i] = value;
            }
        }
        return result;
    }

    public static String[] instantiate(String[] content, Map<String, String> parameters) {
        String[] result = new String[content.length];
        for (int i = 0; i < content.length; i++) {
            String value = content[i];
            while (value.contains("${")) {
                String parameter = value.substring(value.indexOf('{') + 1, value.indexOf('}'));
                if (parameters.containsKey(parameter)) {
                    value = value.substring(0, value.indexOf('$')) + parameters.get(parameter)
                            + value.substring(value.indexOf('}') + 1);
                } else {
                    throw new IllegalArgumentException("parameters does not contain variable " + parameter);
                }
            }
            result[i] = value;
        }
        return result;
    }

    protected Node traverse(Session session, String path) throws RepositoryException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return ((HippoWorkspace) session.getWorkspace()).getHierarchyResolver().getNode(session.getRootNode(), path);
    }

}
