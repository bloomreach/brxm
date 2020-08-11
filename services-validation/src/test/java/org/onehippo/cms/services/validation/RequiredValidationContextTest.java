/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.services.validation;

import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms.services.validation.validator.TestValidationContext;
import org.onehippo.repository.mock.MockNode;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class RequiredValidationContextTest {

    private Locale locale;
    private TimeZone timeZone;
    private Node parentNode;
    private RequiredValidationContext context;
    private MockNode documentNode;

    @Before
    public void setUp() {
        locale = new Locale("nl");
        timeZone = TimeZone.getDefault();
        parentNode = MockNode.root();
        documentNode = MockNode.root();

        final TestValidationContext delegate = new TestValidationContext("jcrName", "jcrType", "type",
                locale, timeZone, parentNode, documentNode);
        context = new RequiredValidationContext(delegate);
    }

    @Test
    public void delegatedCalls() {
        assertThat(context.getJcrName(), equalTo("jcrName"));
        assertThat(context.getJcrType(), equalTo("jcrType"));
        assertThat(context.getType(), equalTo("type"));
        assertThat(context.getLocale(), equalTo(locale));
        assertThat(context.getTimeZone(), equalTo(timeZone));
        assertThat(context.getParentNode(), equalTo(parentNode));
        assertThat(context.getDocumentNode(), equalTo(documentNode));
    }

    @Test
    public void createViolation() {
        final TranslatedViolation violation = (TranslatedViolation) context.createViolation();
        assertThat(violation.getKeys(), equalTo(Arrays.asList("required#type", "required#jcrType", "required")));
    }

    @Test
    public void createViolationWithSubKey() {
        final TranslatedViolation violation = (TranslatedViolation) context.createViolation("subKey");
        assertThat(violation.getKeys(), equalTo(Arrays.asList("required#type#subKey", "required#jcrType#subKey", "required")));
    }

    @Test
    public void createViolationWithNodeTypeName() {
        final TestValidationContext delegate = new TestValidationContext("jcrName", "jcrType", "node:name",
                locale, timeZone, parentNode, documentNode);
        context = new RequiredValidationContext(delegate);

        final TranslatedViolation violation = (TranslatedViolation) context.createViolation();
        assertThat(violation.getFirstKey(), equalTo("required#node-name"));
    }
}