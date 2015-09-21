/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.junit.Test;

import static junit.framework.Assert.fail;


public class GroovyUpdaterClassLoaderTest {

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testSystemExitMethod1IsUnauthorized() {
        final String script = "class Test { void test() { System.exit(0) } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call System.exit");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testSystemExitMethod2IsUnauthorized() {
        final String script = "class Test { void test() { def s = System; s.exit(0); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call System.exit");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testSystemExitMethod3IsUnauthorized() {
        final String script = "class Test { void test() { System.methods.find{ it.name == \"exit\"}.invoke( null, 1 ) } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call System.exit");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testRuntimeIsUnauthorized() {
        String script = "class Test { void test() { def r = Runtime; r.getRuntime(); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call Runtime");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testClassForNameIsUnauthorized() {
        final String script = "class Test { void test() { Class.forName('java.lang.Object') }}";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can call Class.forName");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testImportJavaIOFileIsUnauthorized() {
        final String script = "import java.io.File; class Test { }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can import java.io.File");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testIndirectImportJavaIOFileIsUnauthorized() {
        final String script = "class Test { void test() { java.io.File file = new java.io.File('/'); } }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can use java.io.File");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testJavaNetImportIsUnauthorized() {
        final String script = "import java.net.URL; class Test { }";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can import java.net class");
    }

    @Test (expected = MultipleCompilationErrorsException.class)
    public void testProcessBuilderIsUnauthorized() {
        final String script = "class Test { void test() { new ProcessBuilder('ls').start() }}";
        GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
        fail("Script can execute shell commands");
    }

}
