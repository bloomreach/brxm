/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.modules;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ModuleRegistryTest {

    @Test (expected = RepositoryException.class)
    public void testDetectCircularDependencies() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule("test1", new Test1Module());
        registry.registerModule("test2", new Test2Module());
        registry.registerModule("circularTest3", new CircularTest3Module());
    }

    @Test
    public void testOrderModulesAfterAnnotation() throws Exception {
        final ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule("test", new TestModule());
        registry.registerModule("test4", new Test4Module());

        registry.checkDependencyGraph(true);
        final List<ModuleRegistration> registrations = registry.getModuleRegistrations();

        assertEquals(2, registrations.size());
        assertEquals("test", registrations.get(0).getModuleName());
        assertEquals("test4", registrations.get(1).getModuleName());
    }

    @Test
    public void testOrderModulesServiceDependencies() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule("test1", new Test1Module());
        registry.registerModule("test2", new Test2Module());
        registry.registerModule("test3", new Test3Module());

        registry.checkDependencyGraph(true);
        final List<ModuleRegistration> registrations = registry.getModuleRegistrations();

        assertEquals(3, registrations.size());
        assertEquals("test3", registrations.get(0).getModuleName());
        assertEquals("test2", registrations.get(1).getModuleName());
        assertEquals("test1", registrations.get(2).getModuleName());

        final List<ModuleRegistration> reverse = registry.getModuleRegistrationsReverseOrder();
        registry.checkDependencyGraph(true);

        assertEquals(3, registrations.size());
        assertEquals("test1", reverse.get(0).getModuleName());
        assertEquals("test2", reverse.get(1).getModuleName());
        assertEquals("test3", reverse.get(2).getModuleName());
    }

    @Test (expected = RepositoryException.class)
    public void testUnsatisfiedNonOptionalDependency() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule("test1", new Test1Module());
        registry.getModuleRegistrations();
        registry.checkDependencyGraph(true);
    }

    @Test
    public void testUnsatisfiedOptionalDependency() throws Exception {
        ModuleRegistry registry = new ModuleRegistry();
        registry.registerModule("test2", new Test2Module());
        registry.getModuleRegistrations();
        registry.checkDependencyGraph(true);
    }

    public static class TestModule implements DaemonModule {
        @Override
        public void initialize(final Session session) throws RepositoryException {}
        @Override
        public void shutdown() {}
    }

    @ProvidesService(types = Test1.class)
    @RequiresService(types = Test2.class)
    public static class Test1Module extends TestModule {}

    @ProvidesService(types = Test2.class)
    @RequiresService(types = Test3.class, optional = true)
    public static class Test2Module extends TestModule {}

    @ProvidesService(types = Test3.class)
    @RequiresService(types = Test1.class)
    public static class CircularTest3Module extends TestModule {}

    @ProvidesService(types = Test3.class)
    public static class Test3Module extends TestModule {}

    @After(modules = TestModule.class)
    public static class Test4Module extends TestModule {}

    public interface Test1 {}

    public interface Test2 {}

    public interface Test3 {}
}
