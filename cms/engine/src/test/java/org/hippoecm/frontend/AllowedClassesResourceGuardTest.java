/*
 * Copyright 2017-2023 Bloomreach
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
package org.hippoecm.frontend;

import org.apache.wicket.protocol.http.mock.MockHttpSession;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AllowedClassesResourceGuardTest {

    private WicketTester tester;
    private AllowedClassesResourceGuard guard;

    @Before
    public void setUp() {
        tester = new WicketTester();
        guard = new AllowedClassesResourceGuard();
    }

    @Test
    public void denyPublicNonAllowedResource() {
        assertFalse(guard.accept("org/hippoecm/frontend/test/file.js"));
    }

    @Test
    public void nullClassNamePrefixesAreIgnored() {
        guard.addClassNamePrefixes(null);
        assertFalse(guard.accept("org/hippoecm/frontend/test/file.js"));
    }

    @Test
    public void allowPublicAllowedResource() {
        Class allowed = getClass();
        guard.addClassNamePrefixes(allowed.getPackage().getName());
        assertTrue(guard.accept("org/hippoecm/frontend/test/file.js"));
    }

    @Test
    public void allowPrivateResource() {
        login();
        assertTrue(guard.accept("org/hippoecm/frontend/test/file.js"));
    }

    @Test
    public void allowAllowedResourceInJar() {
        String jarPath = "jar:file:/my/file/system/folder/org/apache/wicket/wicket-core/7.10.0/wicket-core-7.10.0.jar!/org/hippoecm/frontend/resource/jquery/jquery-1.12.4.min.js";
        Class allowed = getClass();
        guard.addClassNamePrefixes(allowed.getPackage().getName());
        assertTrue(guard.accept(jarPath));
    }
    
    private void login() {
        final MockHttpSession httpSession = tester.getHttpSession();
        httpSession.setTemporary(false);
        httpSession.setAttribute("hippo:username", "testuser");
    }
}