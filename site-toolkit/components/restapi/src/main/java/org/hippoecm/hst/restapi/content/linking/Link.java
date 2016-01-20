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
package org.hippoecm.hst.restapi.content.linking;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Link {

    private static enum Type {

        LOCAL("local"),
        EXTERNAL("external"),
        BINARY("binary"),
        INVALID("invalid");

        private String value;

        private Type(final String value) {
            this.value = value;
        }

        /**
         * @return string representation for json serialization
         */
        private String getString() {
            return value;
        }
    }

    @JsonProperty("type")
    public final String type;

    @JsonProperty("id")
    public final String id;
    @JsonProperty("url")
    public final String url;


    public static final Link invalid = new Link(null, null, Type.INVALID);

    public static final Link external(final String id) {
        return new Link(id, null, Type.EXTERNAL);
    }

    public static final Link local(final String id, final String url) {
        return new Link(id, url, Type.LOCAL);
    }

    public static final Link binary(final String url) {
        return new Link(url, Type.BINARY);
    }

    private Link(final String url, final Type type) {
        this(null, url, type);
    }

    private Link(final String id, final String url, final Type type) {
        this.type = type.getString();
        this.id = id;
        this.url = url;
    }
}
