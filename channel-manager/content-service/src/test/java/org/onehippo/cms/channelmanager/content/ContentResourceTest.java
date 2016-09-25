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

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.CXFTest;

import static org.hamcrest.core.IsEqual.equalTo;

public class ContentResourceTest extends CXFTest {

    private Session userSession;
    private ContentService contentService;

    @Before
    public void setup() {
        userSession = EasyMock.createMock(Session.class);
        contentService = EasyMock.createMock(ContentService.class);

        final ContentServiceProvider contentServiceProvider = EasyMock.createMock(ContentServiceProvider.class);
        EasyMock.expect(contentServiceProvider.createContext(EasyMock.anyObject())).andReturn(contentService).anyTimes();

        final UserSessionProvider userSessionProvider = EasyMock.createMock(UserSessionProvider.class);
        EasyMock.expect(userSessionProvider.get(EasyMock.anyObject())).andReturn(userSession).anyTimes();

        EasyMock.replay(contentServiceProvider, userSessionProvider);

        final CXFTest.Config config = new CXFTest.Config();
        config.addServerSingleton(contentServiceProvider);
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
        EasyMock.expect(contentService.getDocument(userSession, requestedUuid)).andReturn(testDocument);

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
