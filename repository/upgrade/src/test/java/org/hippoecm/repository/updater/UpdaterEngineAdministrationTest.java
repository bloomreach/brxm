/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.updater;

import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import org.hippoecm.repository.updater.UpdaterEngine.ModuleRegistration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class UpdaterEngineAdministrationTest {

    UpdaterEngine engine;
    Vector<ModuleRegistration> modules;
    Set<String> currentVersion;

    @Before
    public void setUp() {
        engine = new UpdaterEngine();
        modules = new Vector<ModuleRegistration>();
        currentVersion = new TreeSet<String>();
    }

    /** this test the hidden and discouraged ability to go against the API and not set a start tag */
    @Ignore
    public void testNoStartTagWithoutAny() throws Exception {
        // this test the hidden and discouraged ability to go against the API and not set a start tag
        ModuleRegistration module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerEndTag("v2");
        modules.add(module1);
        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(1, modules.size());
        assertTrue(modules.get(0).equals(module1));
    }

    /** this test the hidden and discouraged ability to go against the API and not set a start tag */
    @Ignore
    public void testNoStartTagWithNonMatching() throws Exception {
        // this test the hidden and discouraged ability to go against the API and not set a start tag
        ModuleRegistration module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerEndTag("v2");
        modules.add(module1);
        currentVersion.add("v1");
        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(1, modules.size());
        assertTrue(modules.get(0).equals(module1));
    }

    /** this test the hidden and discouraged ability to go against the API and not set a start tag */
    @Ignore
    public void testNoStartTagWithMatching() throws Exception {
        ModuleRegistration module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerEndTag("v2");
        modules.add(module1);
        currentVersion.add("v2");
        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(0, modules.size());
    }

    @Ignore
    public void testDirectCircularStartEnd() throws Exception {
        ModuleRegistration module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerStartTag("any");
        module1.registerEndTag("any");
        modules.add(module1);
        currentVersion.add("any");
        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(0, modules.size());
    }

    @Test
    public void testBasicOrdering() throws Exception {
        ModuleRegistration module1, module2;
        currentVersion.add("v1");

        modules.clear();
        module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerStartTag("v1");
        module1.registerEndTag("v2");
        modules.add(module1);
        module2 = engine.new ModuleRegistration(null);
        module2.registerName("module2");
        module2.registerStartTag("v1");
        module2.registerEndTag("v2");
        module2.registerAfter("module1");
        modules.add(module2);

        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(2, modules.size());
        assertTrue(modules.get(0).equals(module1));
        assertTrue(modules.get(1).equals(module2));

        modules.clear();
        module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerStartTag("v1");
        module1.registerEndTag("v2");
        modules.add(module1);
        module2 = engine.new ModuleRegistration(null);
        module2.registerName("module2");
        module2.registerStartTag("v1");
        module2.registerEndTag("v2");
        module2.registerBefore("module1");
        modules.add(module2);

        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(2, modules.size());
        assertTrue(modules.get(0).equals(module2));
        assertTrue(modules.get(1).equals(module1));

        modules.clear();
        module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerStartTag("v1");
        module1.registerEndTag("v2");
        module1.registerBefore("module2");
        modules.add(module1);
        module2 = engine.new ModuleRegistration(null);
        module2.registerName("module2");
        module2.registerStartTag("v1");
        module2.registerEndTag("v2");
        modules.add(module2);

        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(2, modules.size());
        assertTrue(modules.get(0).equals(module1));
        assertTrue(modules.get(1).equals(module2));

        modules.clear();
        module1 = engine.new ModuleRegistration(null);
        module1.registerName("module1");
        module1.registerStartTag("v1");
        module1.registerEndTag("v2");
        module1.registerAfter("module2");
        modules.add(module1);
        module2 = engine.new ModuleRegistration(null);
        module2.registerName("module2");
        module2.registerStartTag("v1");
        module2.registerEndTag("v2");
        modules.add(module2);

        UpdaterEngine.prepare(modules, currentVersion);
        assertEquals(2, modules.size());
        assertTrue(modules.get(0).equals(module2));
        assertTrue(modules.get(1).equals(module1));
    }
}
