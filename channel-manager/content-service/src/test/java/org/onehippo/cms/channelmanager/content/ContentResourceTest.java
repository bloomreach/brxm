/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content;

import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.CXFTest;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.core.IsEqual.equalTo;

public class ContentResourceTest extends CXFTest {

    private Session userSession;
    private ContentService contentService;

    @Before
    public void setup() {
        userSession = createMock(Session.class);
        contentService = createMock(ContentService.class);

        final UserSessionProvider userSessionProvider = createMock(UserSessionProvider.class);
        expect(userSessionProvider.get(anyObject())).andReturn(userSession).anyTimes();
        replay(userSessionProvider);

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(new ContentServiceProvider(contentService));
        config.addServerSingleton(new ContentResource(userSessionProvider));

        setup(config);
    }

/* In order to write such tests, @Context injection must be enabled (how?).
    @Test
    public void contentResourceRelaysUserSessionAndDocumentUuidToContentService() {
        final String requestedUuid = "requested-uuid";
        final String returnedUuid = "returned-uuid";
        final Document testDocument = new Document();
        testDocument.setId(returnedUuid);
        expect(contentService.getDocument(userSession, requestedUuid)).andReturn(testDocument);

        when()
                .get("/documents/" + requestedUuid)
        .then()
                .statusCode(200)
                .body(equalTo("TBD"));
    }
*/

    @Test
    public void callingHelloWorldMustSucceed() {
        when()
                .get("/")
        .then()
                .statusCode(200)
                .body(equalTo("Hello World!"));
    }
}
