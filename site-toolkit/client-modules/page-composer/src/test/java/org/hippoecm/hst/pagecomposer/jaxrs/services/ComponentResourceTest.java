/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;


import javax.ws.rs.Path;

import org.junit.Before;
import org.junit.Test;

import io.restassured.http.ContentType;
import static org.hamcrest.CoreMatchers.equalTo;

public class ComponentResourceTest extends AbstractResourceTest {

    static final String MOCK_PATH = "/test";

    @Path(MOCK_PATH)
    private static final class ComponentResourceMock extends ComponentResource {

    }

    @Before
    public void setUp() {
        setup(createDefaultConfig()
                .addServerSingleton(new ComponentResourceMock()));
    }

    @Test
    public void test_get_menu() {
        given().when()
                .get(MOCK_PATH)
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("data.channel.subItems.discard-changes.enabled", equalTo(true))
                .body("data.page.subItems.tools.enabled", equalTo(true))
                .body("data.xpage.subItems.new.enabled", equalTo(true))
        ;
    }
}
