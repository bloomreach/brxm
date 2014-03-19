/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.test;

import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

@Ignore
public class TraversePerfTestCase extends RepositoryTestCase {
    private String[] content = {
        "/test", "nt:unstructured",
        "/test/aap", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/mies", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/wim", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/gijs", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/lam", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/kees", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/bok", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/does", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/hok", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/schapen", "nt:unstructured",
        "foo", "",
        "bar", "",
        "/test/noot/zus/jet/teun/vuur/weide/hok/duif", "nt:unstructured",
        "foo", "",
        "bar", ""
    };

    @Test
    public void testLocal() throws Exception {
        build(content, session);
        session.save();
        long duration = test(session, 100);
        System.out.println("traversal " + Double.toString(duration / 100.0) + "ms");
    }

    private long test(Session session, int count) throws RepositoryException {
        Node root = session.getRootNode();
        long tAfter, tBefore = System.currentTimeMillis();
        for (int i = 0; i <= count; i++) {
            if (i == 1)
                tBefore = System.currentTimeMillis();
            Node node = root;
            StringTokenizer st = new StringTokenizer("test/noot/zus/jet/teun/vuur/weide/hok/duif", "/");
            while (st.hasMoreTokens()) {
                node = node.getNode(st.nextToken());
                for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                    Property property = iter.nextProperty();
                    property.getString();
                }
            }
        }
        tAfter = System.currentTimeMillis();
        return tAfter - tBefore;
    }
}
