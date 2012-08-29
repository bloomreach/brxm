/*
 *  Copyright 2012 Hippo.
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

    @Test
    public void testSystemExitIsUnauthorized() {
        final String script = "class Test { void test() { System.exit(0) } }";
        try {
            GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
            fail("Script can call System.exit");
        } catch (MultipleCompilationErrorsException expected) {
        }
    }

    @Test
    public void testClassForNameIsUnauthorized() {
        final String script = "class Test { void test() { Class.forName('java.lang.Object') }}";
        try {
            GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
            fail("Script can call Class.forName");
        } catch (MultipleCompilationErrorsException expected) {
        }
    }

    @Test
    public void testImportJavaIOFileIsUnauthorized() {
        final String script = "import java.io.File; class Test { }";
        try {
            GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
            fail("Script can import java.io.File");
        } catch (MultipleCompilationErrorsException expected) {
        }
    }

    @Test
    public void testIndirectImportJavaIOFileIsUnauthorized() {
        final String script = "class Test { void test() { java.io.File file = new java.io.File('/'); } }";
        try {
            GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
            fail("Script can use java.io.File");
        } catch (MultipleCompilationErrorsException expected) {
        }
    }

    @Test
    public void testJavaNetImportIsUnauthorized() {
        final String script = "import java.net.URL; class Test { }";
        try {
            GroovyUpdaterClassLoader.createClassLoader().parseClass(script);
            fail("Script can import java.net class");
        } catch (MultipleCompilationErrorsException expected) {
        }
    }

}
