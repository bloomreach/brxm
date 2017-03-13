/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository;

public interface HippoStdPubWfNodeType {

    String HIPPO_REQUEST = "hippo:request";
    String NT_HIPPOSTDPUBWF_REQUEST = "hippostdpubwf:request";
    String HIPPOSTDPUBWF_TYPE = "hippostdpubwf:type";
    String HIPPOSTDPUBWF_DOCUMENT = "hippostdpubwf:document";
    String HIPPOSTDPUBWF_PUBLICATION_DATE = "hippostdpubwf:publicationDate";
    String HIPPOSTDPUBWF_LAST_MODIFIED_DATE = "hippostdpubwf:lastModificationDate";
    String HIPPOSTDPUBWF_LAST_MODIFIED_BY = "hippostdpubwf:lastModifiedBy";
    String HIPPOSTDPUBWF_CREATION_DATE = "hippostdpubwf:creationDate";
    String HIPPOSTDPUBWF_CREATED_BY = "hippostdpubwf:createdBy";
    String HIPPOSTDPUBWF_USERNAME = "hippostdpubwf:username";
    String HIPPOSTDPUBWF_REQDATE = "hippostdpubwf:reqdate";
    String HIPPOSTDPUBWF_REASON = "hippostdpubwf:reason";

    String REJECTED = "rejected";
    String PUBLISH = "publish";
    String DEPUBLISH = "depublish";
    String SCHEDPUBLISH = "scheduledpublish";
    String SCHEDDEPUBLISH = "scheduleddepublish";
    String DELETE = "delete";
}
