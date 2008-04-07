/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import org.hippoecm.repository.api.Document;

public class PublishableDocument extends Document {
    final public static String PUBLISHED = "published";
    final public static String UNPUBLISHED = "unpublished";
    final public static String DRAFT = "draft";
    final public static String STALE = "stale";

    String state;
    String username;

    public PublishableDocument() {
        this.state = UNPUBLISHED;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    void setState(String state) {
        this.state = state;
    }

    void setOwner(String username) {
        this.username = username;
    }
}
