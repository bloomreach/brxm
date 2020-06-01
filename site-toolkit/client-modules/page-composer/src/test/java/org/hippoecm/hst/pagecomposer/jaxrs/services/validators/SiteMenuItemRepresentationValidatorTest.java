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

import com.google.common.base.Predicate;

import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class SiteMenuItemRepresentationValidatorTest {

    private SiteMenuItemRepresentationValidator validator;

    private Predicate<String> externalLinkValidator;
    private SiteMenuItemRepresentation representation;

    @Before
    public void setUp() {
        this.externalLinkValidator = createNiceMock(Predicate.class);
        this.representation = createNiceMock(SiteMenuItemRepresentation.class);

        this.validator = new SiteMenuItemRepresentationValidator(externalLinkValidator, representation);
    }

    @Test
    public void testValidate_valid_different_link_type() {
        expect(representation.getLinkType()).andReturn(LinkType.NONE);
        replay(representation, externalLinkValidator);
        validator.validate(null);
        verify(representation);
    }

    @Test
    public void testValidate_valid_external_link() {
        expect(representation.getLinkType()).andReturn(LinkType.EXTERNAL);
        expect(representation.getLink()).andReturn("link");
        expect(externalLinkValidator.apply("link")).andReturn(true);
        replay(representation, externalLinkValidator);
        validator.validate(null);
    }

    @Test
    public void testValidate_invalid() {
        expect(representation.getLinkType()).andReturn(LinkType.EXTERNAL);
        expect(representation.getLink()).andReturn("link");
        expect(externalLinkValidator.apply("link")).andReturn(false);
        replay(representation, externalLinkValidator);
        try {
            validator.validate(null);
        } catch (ClientException e) {
            assertThat(e.getError(), is(ClientError.INVALID_URL));
        }
    }
}
