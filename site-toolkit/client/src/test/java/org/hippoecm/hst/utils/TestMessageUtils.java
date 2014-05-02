/*
 *  Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.utils;

import static org.junit.Assert.assertEquals;

import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * TestMessageUtils
 */
public class TestMessageUtils {

    private static final String BASIC_TEST_MESSAGE = 
            "The action path is '${ns1.target.environment.url}${header.search.form.action.path.en}' for English users\n"
            + "while the action path is '${ns1.target.environment.url}${header.search.form.action.path.fr}' for French users.";

    private static final String BASIC_TEST_EXPECTED_MESSAGE = 
            "The action path is 'http://web24.ns1.com:9090/PS/en/Redirect.do' for English users\n"
            + "while the action path is 'http://web24.ns1.com:9090/PS/fr/Redirect.do' for French users.";

    @Test
    public void testBasic() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHstRequestContext requestContext = new MockHstRequestContext();
        requestContext.setServletRequest(request);
        ModifiableRequestContextProvider.set(requestContext);

        final String bundleName = getClass().getName();
        String replacedMessage = MessageUtils.replaceMessages(bundleName, BASIC_TEST_MESSAGE);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);
    }

}
