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

package org.onehippo.cms.channelmanager.visualediting;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.onehippo.cms.channelmanager.visualediting.model.Document;
import org.onehippo.cms.channelmanager.visualediting.model.DocumentTypeSpec;
import org.onehippo.cms.channelmanager.visualediting.util.MockResponse;

@Produces("application/json")
@Path("/")
public class VisualEditingResource {

    @GET
    @Path("/")
    public String helloWorld() {
        return "Hello World!";
    }

    @GET
    @Path("documents/{id}")
    public Document getDocument(@PathParam("id") String id) throws IOException {
        return MockResponse.createTestDocument(id);
    }

    @GET
    @Path("documenttypes/{id}")
    public DocumentTypeSpec getDocumentTypeSpec(@PathParam("id") String id) throws IOException {
        return MockResponse.createTestDocumentType();
    }
}
