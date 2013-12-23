/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;

import org.apache.jackrabbit.core.query.lucene.SearchIndex;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;

public class SearchTransactionalityTest extends RepositoryTestCase {

    private Set<String> previousResult = new HashSet<>();
    private Set<String> previouslyAdded = new HashSet<>();
    private Map<String, Integer> nodes = new HashMap<>();
    private int run;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        removeNode("/test");
        session.getRootNode().addNode("test");
        session.save();

        final NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();
        final NodeTypeTemplate nodeType = nodeTypeManager.createNodeTypeTemplate();
        nodeType.setName("hippo:mydocument");
        nodeType.setDeclaredSuperTypeNames(new String[]{ "nt:unstructured" });
        nodeTypeManager.registerNodeType(nodeType, true);

    }

    @Test
    @Ignore
    public void testSearchRepeatedly() throws Exception {
        for (int i = 0; i < 1000; i++) {
            run = i + 1;
            testSearch();
        }
    }

    public void testSearch() throws Exception {

        final Node test = getSession().getRootNode().getNode("test");
        final Set<String> removed = removeRandomNodes(test);
        getSession().save();
        getSearchIndex().flush();

        Set<String> result = getSearchResults();
        try {
            assertEquals(previousResult.size()-removed.size(), result.size());
        } catch (AssertionError e) {
            System.out.println("Failure during run " + run);
            System.out.println("Checking failure conditions");
            if (intersection(removed, result).size() > 0) {
                System.out.println("Result still contains removed nodes");
            }
            Set<String> remaining = new HashSet<>(previousResult);
            remaining.removeAll(removed);
            if (!isFullSubSet(remaining, result)) {
                System.out.println("Previously existing nodes not in result:");
                final Set<String> difference = difference(remaining, result);
                for (String s : difference) {
                    System.out.println("Node " + s + " added during run " + nodes.get(s));
                }
            }
            Thread.sleep(300l);
            result = getSearchResults();
            if (previousResult.size()-removed.size() == result.size()) {
                System.out.println("Result back to normal after small wait");
            } else {
                System.out.println("Result still not as expected");
            }
            throw e;
        }

        int initialSize = result.size();
        System.out.println("Initial number of hippo:mydocument nodes: " + initialSize);

        final Random random = new Random();
        Stack<Integer> levels = new Stack<>();
        for (int count : new int[] { random.nextInt(12)+4, random.nextInt(12)+4, random.nextInt(12)+4 }) {
            levels.push(count);
        }

        Set<String> added = new HashSet<>();
        createDocuments(test, levels, added);
        getSearchIndex().flush();

        System.out.println(added.size() + " documents created");
        result = getSearchResults();
        try {
            assertEquals(added.size(), result.size() - initialSize);
        } catch (AssertionError e) {
            System.out.println("Failure during run " + run);
            System.out.println("Checking failure conditions");
            if (intersection(removed, result).size() > 0) {
                System.out.println("Result still contains removed nodes");
            }
            if (!isFullSubSet(added, result)) {
                System.out.println("Newly added nodes not in result");
            }
            Set<String> remaining = new HashSet<>(previousResult);
            remaining.removeAll(removed);
            if (!isFullSubSet(remaining, result)) {
                System.out.println("Previously existing nodes not in result");
            }
            Set<String> all = union(remaining, added);
            Set<String> difference = difference(all, result);
            for (String s : difference) {
                System.out.println("Node " + s + " added during run " + nodes.get(s));
            }
            throw e;
        }
        previousResult = result;
        previouslyAdded = added;
    }

    private Set<String> removeRandomNodes(final Node test) throws RepositoryException {
        final RemoveDocumentsItemVisitor documentRemover = new RemoveDocumentsItemVisitor();
        test.accept(documentRemover);
        test.getSession().save();
        System.out.println(documentRemover.deleted.size() + " documents deleted");
        for (String s : documentRemover.deleted) {
            nodes.remove(s);
        }
        return documentRemover.deleted;
    }

    private Set<String> getSearchResults() throws Exception {
        Set<String> result = new HashSet<>();
        final NodeIterator nodes = getSession().getWorkspace().getQueryManager().createQuery("//element(*,hippo:mydocument)", "xpath").execute().getNodes();
        while (nodes.hasNext()) {
            final Node node = nodes.nextNode();
            result.add(node.getIdentifier());
        }
        return result;
    }

    private int createDocuments(Node folder, Stack<Integer> levels, final Set<String> added) throws RepositoryException {
        int numof = 0;
        Stack<Integer> subLevels = new Stack<>();
        subLevels.addAll(levels);
        int count = subLevels.pop();
        for (int i = 0; i < count; i++) {
            if (subLevels.size() > 0) {
                final String folderName = "folder" + i;
                final Node childFolder;
                if (!folder.hasNode(folderName)) {
                    childFolder = folder.addNode(folderName);
                } else {
                    childFolder = folder.getNode(folderName);
                }
                numof += createDocuments(childFolder, subLevels, added);
            } else {
                final Node document = folder.addNode("document" + i, "hippo:mydocument");
                document.setProperty("html", "<html/>");
                added.add(document.getIdentifier());
                nodes.put(document.getIdentifier(), run);
                numof += 1;
            }
        }
        getSession().save();
        return numof;
    }

    private static class RemoveDocumentsItemVisitor implements ItemVisitor {
        private final Random random = new Random();
        private double p = random.nextGaussian();
        private final Set<String> deleted = new HashSet<>();

        @Override
        public void visit(final Property property) throws RepositoryException {
        }

        @Override
        public void visit(final Node node) throws RepositoryException {
            if (node.getName().startsWith("document")) {
                if (random.nextGaussian() < p) {
                    deleted.add(node.getIdentifier());
                    node.remove();
                }
            } else {
                for (Node child : new NodeIterable(node.getNodes())) {
                    visit(child);
                }
            }
        }
    }

    private Set<String> intersection(Set<String> set1, Set<String> set2) {
        Set<String> result = new HashSet<>();
        for (String s : set1) {
            if (set2.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }

    private Set<String> union(Set<String> set1, Set<String> set2) {
        Set<String> result = new HashSet<>(set1);
        result.addAll(set2);
        return result;
    }

    private boolean isFullSubSet(Set<String> subset, Set<String> superset) {
        for (String s : subset) {
            if (!superset.contains(s)) {
                return false;
            }
        }
        return true;
    }

    private Set<String> difference(Set<String> set1, Set<String> set2) {
        final Set<String> result = new HashSet<>();
        for (String s : set1) {
            if (!set2.contains(s)) {
                result.add(s);
            }
        }
        for (String s : set2) {
            if (!set1.contains(s)) {
                result.add(s);
            }
        }
        return result;
    }

    private SearchIndex getSearchIndex() throws RepositoryException {
        return (SearchIndex)((RepositoryImpl) RepositoryDecorator.unwrap(session.getRepository())).getSearchManager("default").getQueryHandler();
    }

    private Session getSession() {
        return session;
    }
}
