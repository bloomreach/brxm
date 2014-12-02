/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.order;

import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * TestObjectOrderer
 */
public class TestObjectOrderer {

    private static Logger log = LoggerFactory.getLogger(TestObjectOrderer.class);

    @Test
    public void testNoDependencies() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, null);
        o.add("BARNEY", "barney", null, null);
        o.add("WILMA", "wilma", null, null);
        o.add("BETTY", "betty", null, null);

        List<String> l = o.getOrderedObjects();
        assertArrayEquals(new String [] { "FRED", "BARNEY", "WILMA", "BETTY" }, l.toArray());
    }

    @Test
    public void testPrereq() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", "wilma", null);
        o.add("BARNEY", "barney", "betty", null);
        o.add("BETTY", "betty", null, null);
        o.add("WILMA", "wilma", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "WILMA", "FRED", "BETTY", "BARNEY" }, l.toArray());
    }

    @Test
    public void testPostreq() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, "barney,wilma");
        o.add("BARNEY", "barney", null, "betty");
        o.add("BETTY", "betty", null, null);
        o.add("WILMA", "wilma", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "FRED", "BARNEY", "BETTY", "WILMA" }, l.toArray());
    }

    @Test
    public void testPrePostreq() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, "barney,wilma");
        o.add("BARNEY", "barney", "wilma", "betty");
        o.add("BETTY", "betty", null, null);
        o.add("WILMA", "wilma", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "FRED", "WILMA", "BARNEY", "BETTY" }, l.toArray());
    }

    @Test
    public void testUnknownPrereq() throws Exception {
        log.error("[INFO] Unknown cartoon character dependency 'charlie' (for 'fred').");

        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", "charlie", "barney,wilma");
        o.add("BARNEY", "barney", "wilma", "betty");
        o.add("BETTY", "betty", null, null);
        o.add("WILMA", "wilma", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "FRED", "WILMA", "BARNEY", "BETTY" }, l.toArray());
    }

    @Test
    public void testUnknownPostreq() throws Exception {
        log.error("[INFO] Unknown cartoon character dependency 'dino' (for 'betty').");

        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, "barney,wilma");
        o.add("BARNEY", "barney", "wilma", "betty");
        o.add("BETTY", "betty", null, "dino");
        o.add("WILMA", "wilma", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "FRED", "WILMA", "BARNEY", "BETTY" }, l.toArray());
    }

    @Test
    public void testCyclePre() throws Exception {
        log.error("[INFO] Unable to order cartoon character 'wilma' due to dependency cycle:"
                + " A cycle has been detected from the initial object [wilma]");

        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");
        o.setIgnoreExceptions(true);

        o.add("FRED", "fred", "wilma", null);
        o.add("BARNEY", "barney", "betty", null);
        o.add("BETTY", "betty", "fred", null);
        o.add("WILMA", "wilma", "barney", null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "WILMA", "FRED", "BETTY", "BARNEY" }, l.toArray());
    }

    @Test
    public void testCyclePost() throws Exception {
        log.error("[INFO] Unable to order cartoon character 'betty' due to dependency cycle: A cycle has been detected from the initial object [fred]");

        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");
        o.setIgnoreExceptions(true);

        o.add("WILMA", "wilma", null, "betty");
        o.add("FRED", "fred", null, "barney");
        o.add("BARNEY", "barney", null, "wilma");
        o.add("BETTY", "betty", null, "fred");

        List<String> l = o.getOrderedObjects();
        assertArrayEquals(new String[] { "FRED", "BARNEY", "WILMA", "BETTY" }, l.toArray());
    }

    @Test
    public void testDupe() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "flintstone", null, null);
        o.add("BARNEY", "rubble", null, null);
        o.add("WILMA", "flintstone", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "FRED", "BARNEY" }, l.toArray());
    }

    @Test
    public void testPreStar() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", "*", null);
        o.add("BARNEY", "barney", "betty", null);
        o.add("WILMA", "wilma", "betty", null);
        o.add("BETTY", "betty", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "BETTY", "BARNEY", "WILMA", "FRED" }, l.toArray());
    }

    @Test
    public void testPreStartDupe() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", "*", null);
        o.add("BARNEY", "barney", "*", null);
        o.add("WILMA", "wilma", "betty", null);
        o.add("BETTY", "betty", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "BARNEY", "BETTY", "WILMA", "FRED" }, l.toArray());
    }

    @Test
    public void testPostStar() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, "wilma");
        o.add("BARNEY", "barney", null, "*");
        o.add("WILMA", "wilma", null, "betty");
        o.add("BETTY", "betty", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new String[] { "BARNEY", "FRED", "WILMA", "BETTY" }, l.toArray());
    }

    @Test
    public void testPostStarDupe() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        o.add("FRED", "fred", null, "wilma");
        o.add("BARNEY", "barney", null, "*");
        o.add("WILMA", "wilma", null, "*");
        o.add("BETTY", "betty", null, null);

        List<String> l = o.getOrderedObjects();

        assertArrayEquals(new Object[] { "BARNEY", "FRED", "WILMA", "BETTY" }, l.toArray());
    }

    @Test
    public void testNoObjects() throws Exception {
        ObjectOrderer<String> o = new ObjectOrderer<String>("cartoon character");

        List<String> l = o.getOrderedObjects();

        assertEquals(0, l.size());
    }

}