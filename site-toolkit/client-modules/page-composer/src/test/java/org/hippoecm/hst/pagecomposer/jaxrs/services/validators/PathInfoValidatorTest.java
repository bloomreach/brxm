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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

    @Test
    public void test_validate_fails_on_xss_markup() {
        final HstRequestContext context = createNiceMock(HstRequestContext.class);
        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(context.getServletRequest()).andReturn(request);
        replay(context, request);

        final String pathInfo = "abc<def>";
        final PathInfoValidator validator = new PathInfoValidator(pathInfo);
        try {
            validator.validate(context);
            fail("Expected an exception of type " + ClientException.class.getSimpleName());
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_PATH_INFO));
        }
    }


    @Test
    public void test_validate_fails_on_wrong_character_encoding() {
        final HstRequestContext context = createNiceMock(HstRequestContext.class);
        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(context.getServletRequest()).andReturn(request);
        expect(request.getCharacterEncoding()).andReturn("UNKNOWN_CHARACTER_ENCODING");
        replay(context, request);

        final String pathInfo = "%24";
        final PathInfoValidator validator = new PathInfoValidator(pathInfo);
        try {
            validator.validate(context);
            fail("Expected an exception of type " + ClientException.class.getSimpleName());
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_PATH_INFO));
        }
    }
}
