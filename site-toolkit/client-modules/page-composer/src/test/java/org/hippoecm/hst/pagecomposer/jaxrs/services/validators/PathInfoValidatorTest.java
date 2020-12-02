/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*"})
@PrepareForTest(HstRequestUtils.class)
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
        assertTrue("% is not allowed " +
                "deny those requests because of security ", PathInfoValidator.containsInvalidChars("test%", "UTF-8"));
        assertTrue("% is not allowed " +
                "deny those requests because of security ", PathInfoValidator.containsInvalidChars("test%test", "UTF-8"));
    }

    @Test
    public void test_validate_fails_on_xss_markup() {
        final HstRequestContext context = createNiceMock(HstRequestContext.class);
        final HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(context.getServletRequest()).andReturn(request);
        replay(context, request);

        final String pathInfo = "abc<def>";
        final PathInfoValidator validator = new PathInfoValidator(new PageComposerContextService(), pathInfo);
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
        replay(context);

        mockStatic(HstRequestUtils.class);
        expect(HstRequestUtils.getURIEncoding(anyObject())).andReturn("UNKNOWN_CHARACTER_ENCODING").anyTimes();
        replay(HstRequestUtils.class);

        final String pathInfo = "%24";
        final PathInfoValidator validator = new PathInfoValidator(new PageComposerContextService(), pathInfo);
        try {
            validator.validate(context);
            fail("Expected an exception of type " + ClientException.class.getSimpleName());
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_PATH_INFO));
        }
    }
}
