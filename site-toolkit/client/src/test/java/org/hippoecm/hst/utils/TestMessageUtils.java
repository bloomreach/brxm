/*
 *  Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ResourceBundle;

import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TestMessageUtils
 */
public class TestMessageUtils {

    private static final String BUNDLE_NAME = TestMessageUtils.class.getName();

    private ResourceBundle bundle;

    private static final String BASIC_TEST_MESSAGE = 
            "The action path is '${ns1.target.environment.url}${header.search.form.action.path.en}' for English users\n"
            + "while the action path is '${ns1.target.environment.url}${header.search.form.action.path.fr}' for French users.";

    private static final String BASIC_TEST_EXPECTED_MESSAGE = 
            "The action path is 'http://web24.ns1.com:9090/PS/en/Redirect.do' for English users\n"
            + "while the action path is 'http://web24.ns1.com:9090/PS/fr/Redirect.do' for French users.";

    private static final String PREFIX_SUFFIX_TEST_MESSAGE = 
        "The action path is '@ns1.target.environment.url@@header.search.form.action.path.en@' for English users\n"
        + "while the action path is '@ns1.target.environment.url@@header.search.form.action.path.fr@' for French users.";

    private static final String PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE = 
        "The action path is 'http://web24.ns1.com:9090/PS/en/Redirect.do' for English users\n"
        + "while the action path is 'http://web24.ns1.com:9090/PS/fr/Redirect.do' for French users.";

    private static final String DOLLAR_SIGN_TEST_MESSAGE = 
        "The annual payment is $${annual.payment}.";

    private static final String DOLLAR_SIGN_TEST_EXPECTED_MESSAGE = 
        "The annual payment is $500.";

    private static final String ESCAPABLE_GREETING_VALUE = "<script>alert('hi there!');</script>";

    private static final String ESCAPABLE_TEST_MESSAGE_EXPR = "<h1>${escapable.greeting}</h1>";

    private static final String EXPECTED_UNESCAPED_TEST_MESSAGE = "<h1>" + ESCAPABLE_GREETING_VALUE + "</h1>";

    private static final String EXPECTED_ESCAPED_TEST_MESSAGE = "<h1>" + HstRequestUtils.escapeXml(ESCAPABLE_GREETING_VALUE) + "</h1>";

    @Before
    public void before() throws Exception {
        bundle = ResourceBundleUtils.getBundle(BUNDLE_NAME, null);
    }

    @Test
    public void testBasic() throws Exception {
        String replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, BASIC_TEST_MESSAGE);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, BASIC_TEST_MESSAGE, "${", null, null);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, BASIC_TEST_MESSAGE, "${", "}", null);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, BASIC_TEST_MESSAGE, "${", "}", '$');
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, BASIC_TEST_MESSAGE, "${", "}", '\\');
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, BASIC_TEST_MESSAGE);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, BASIC_TEST_MESSAGE, "${", null, null);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, BASIC_TEST_MESSAGE, "${", "}", null);
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, BASIC_TEST_MESSAGE, "${", "}", '$');
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, BASIC_TEST_MESSAGE, "${", "}", '\\');
        assertEquals(BASIC_TEST_EXPECTED_MESSAGE, replacedMessage);
    }

    @Test
    public void testCustomVariablePrefixSuffix() throws Exception {
        String replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@");
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@", '$');
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@", '\\');
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@");
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@", '$');
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, PREFIX_SUFFIX_TEST_MESSAGE, "@", "@", '\\');
        assertEquals(PREFIX_SUFFIX_TEST_EXPECTED_MESSAGE, replacedMessage);
    }

    @Test
    public void testDollarSignPrefixedVariable() throws Exception {
        String replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, DOLLAR_SIGN_TEST_MESSAGE);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, DOLLAR_SIGN_TEST_MESSAGE, "${", null, null);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, DOLLAR_SIGN_TEST_MESSAGE, "${", "}", null);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, DOLLAR_SIGN_TEST_MESSAGE, "${", "}", '\\');
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, DOLLAR_SIGN_TEST_MESSAGE);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, DOLLAR_SIGN_TEST_MESSAGE, "${", null, null);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, DOLLAR_SIGN_TEST_MESSAGE, "${", "}", null);
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessagesByBundle(bundle, DOLLAR_SIGN_TEST_MESSAGE, "${", "}", '\\');
        assertEquals(DOLLAR_SIGN_TEST_EXPECTED_MESSAGE, replacedMessage);
    }

    @Test
    public void testEscaping() throws Exception {
        String replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, ESCAPABLE_TEST_MESSAGE_EXPR, "${", "}", '\\', true);
        assertEquals(EXPECTED_ESCAPED_TEST_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, ESCAPABLE_TEST_MESSAGE_EXPR, "${", "}", '\\', false);
        assertEquals(EXPECTED_UNESCAPED_TEST_MESSAGE, replacedMessage);

        replacedMessage = MessageUtils.replaceMessages(BUNDLE_NAME, ESCAPABLE_TEST_MESSAGE_EXPR, "${", "}", '\\');
        assertEquals(EXPECTED_UNESCAPED_TEST_MESSAGE, replacedMessage);
    }
}

