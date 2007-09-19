/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
    final public String REJECTED = "rejected"; // zombie
    final public String PUBLISH = "publish";
    final public String DEPUBLISH = "depublish";
    final public String DELETE = "delete";
    public String type;
    public String username;
    public String reason;
    public PublicationRequest(String type, String username) {
        this.username = username;
        this.type = type;
        reason = "";
    }
}
