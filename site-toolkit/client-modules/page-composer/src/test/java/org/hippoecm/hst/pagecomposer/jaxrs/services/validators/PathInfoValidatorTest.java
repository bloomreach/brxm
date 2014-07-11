/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathInfoValidatorTest {

    @Test
    public void test_allowed_path_info() {
        assertFalse(PathInfoValidator.containsEncodedDirectoryTraversalChars("test", "UTF-8"));
        assertFalse(PathInfoValidator.containsEncodedDirectoryTraversalChars("/test", "UTF-8"));
        assertFalse(PathInfoValidator.containsEncodedDirectoryTraversalChars("/test/foo", "UTF-8"));
        assertFalse(PathInfoValidator.containsEncodedDirectoryTraversalChars("/test/foo%20/bar%21", "UTF-8"));

        assertTrue("forward slash encoded is not allowed because default tomcat / jboss " +
                "deny those requests because of security ", PathInfoValidator.containsEncodedDirectoryTraversalChars("te%2fst", "UTF-8"));
        assertTrue("forward slash encoded is not allowed because default tomcat / jboss " +
                "deny those requests because of security ", PathInfoValidator.containsEncodedDirectoryTraversalChars("te%5cst", "UTF-8"));
        assertTrue("forward slash encoded is not allowed because default tomcat / jboss " +
                "deny those requests because of security ", PathInfoValidator.containsEncodedDirectoryTraversalChars("te%2est", "UTF-8"));
        assertTrue("forward slash encoded is not allowed because default tomcat / jboss " +
                "deny those requests because of security ", PathInfoValidator.containsEncodedDirectoryTraversalChars("test%", "UTF-8"));
        assertTrue("forward slash encoded is not allowed because default tomcat / jboss " +
                "deny those requests because of security ", PathInfoValidator.containsEncodedDirectoryTraversalChars("test%test", "UTF-8"));
    }

}
