/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClasspathResourceAnnotationScannerTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Anno {
    }

    @Anno
    public class Clazz1 { }

    @Anno
    public class Clazz2 { }

    @Test
    public void scan_classes_with_annotation_by_classpath() throws Exception {
        Set<String> classes = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(Anno.class, "classpath*:org/onehippo/cm/model/**/*.class");
        Set<String> expected = new HashSet<>();
        expected.add(Clazz1.class.getName());
        expected.add(Clazz2.class.getName());
        assertTrue(expected.equals(classes));
    }

    @Test
    public void scan_classes_with_annotation_by_non_existing_classpath() throws Exception {
        Set<String> classes = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(Anno.class, "classpath*:foo/bar/onehippo/cm/model/**/*.class");
        assertTrue(classes.isEmpty());
    }

    @Test
    public void scan_classes_with_annotation_by_path() throws Exception {
        Set<String> classes = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(Anno.class, "org/onehippo/cm/model/**/*.class");
        Set<String> expected = new HashSet<>();
        expected.add(Clazz1.class.getName());
        expected.add(Clazz2.class.getName());
        assertTrue(expected.equals(classes));
    }

    @Test
    public void scan_classes_with_annotation_by_non_existing_path() throws Exception {
        try {
            new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(Anno.class, "foo/bar/onehippo/cm/model/**/*.class");
            fail("Expected runtime exception");
        } catch (RuntimeException e) {
            assertEquals("Cannot load resource(s) from the classpath.", e.getMessage());
        }
    }

    @Test
    public void scan_classes_with_annotation_by_comma_separated_classpath() throws Exception {
        Set<String> classes = new ClasspathResourceAnnotationScanner().scanClassNamesAnnotatedBy(Anno.class, "classpath*:bar/foo/cm/model/**/*.class", "classpath*:org/onehippo/cm/model/**/*.class");
        Set<String> expected = new HashSet<>();
        expected.add(Clazz1.class.getName());
        expected.add(Clazz2.class.getName());
        assertTrue(expected.equals(classes));
    }

}
