/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.jaxrs;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

public class HelloWorldResource {

    private String message;

    public HelloWorldResource() {
        this.message = "Hello world from CXF";
    }

    public HelloWorldResource(String message) {
        this.message = message;
    }

    @Path("/")
    @GET
    public String getHelloWorld() {
        return message;
    }
}
