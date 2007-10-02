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

public class PublicationRequest extends Document {
    final public static String REJECTED = "rejected"; // zombie
    final public static String PUBLISH = "publish";
    final public static String DEPUBLISH = "depublish";
    final public static String DELETE = "delete";
    public String type;
    public String reason;
    public String username;
    public String reference;
    public PublicationRequest(String type, PublishableDocument document, String username) {
        this.username = username;
        this.type = type;
        reason = "";
        reference = document.getJcrIdentity();
    }
}
