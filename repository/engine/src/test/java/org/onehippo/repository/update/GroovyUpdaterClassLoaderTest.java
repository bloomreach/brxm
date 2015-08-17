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
package org.onehippo.repository.update;

import javax.jcr.RepositoryException;

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;


public class GroovyUpdaterClassLoaderTest {

    @Before
    public void setUp() throws RepositoryException {
        final MockNode configuration = new MockNode("config");
        configuration.setProperty("illegalPackages", new String[]{"java.text"});
        configuration.setProperty("illegalClasses", new String[]{"java.util.Collections"});
        configuration.setProperty("illegalMethods", new String[]{"java.util.Arrays#asList"});
        UpdaterExecutor.setConfiguration(new UpdaterExecutorConfiguration(configuration));
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testDefaultIllegalPackageIsUnauthorized() {
        final String script = "class Test { void test() { java.net.URL url = new java.net.URL('file://'); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can use class in default illegal package");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testDefaultIllegalClassIsUnauthorized() {
        final String script = "class Test { void test() { java.io.File file = new java.io.File('/'); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can use default illegal class");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testDefaultIllegalMethodIsUnauthorized() {
        final String script = "class Test { void test() { def c = System; c.exit(0) } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can use default illegal method");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testCustomIllegalPackageIsUnauthorized() throws Exception {
        final String script = "class Test { void test() { java.text.DateFormat.getDateInstance(); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call unauthorized class");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testCustomIllegalClassIsUnauthorized() throws Exception {
        final String script = "class Test { void test() { java.util.Collections.emptyList(); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call unauthorized class");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testCustomIllegalMethodIsUnauthorized() throws Exception {
        final String script = "class Test { void test() { java.util.Arrays.asList(); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call unauthorized method");
    }

}
