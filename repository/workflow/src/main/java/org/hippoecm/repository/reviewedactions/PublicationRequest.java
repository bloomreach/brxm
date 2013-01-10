/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import java.util.Date;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.Document;

@PersistenceCapable
public class PublicationRequest extends Document {

    final public static String REJECTED = "rejected"; // zombie
    final public static String PUBLISH = "publish";
    final public static String DEPUBLISH = "depublish";
    final public static String SCHEDPUBLISH = "scheduledpublish";
    final public static String SCHEDDEPUBLISH = "scheduleddepublish";
    final public static String DELETE = "delete";

    @Persistent(column="hippostdpubwf:type")
    public String type;

    @Persistent(column="hippostdpubwf:reason")
    public String reason;

    @Persistent(column="hippostdpubwf:username")
    public String username;

    @Persistent(column="hippostdpubwf:document")
    public Document reference;

    @Persistent(column="hippostdpubwf:reqdate")
    public long reqdate; // FIXME: use Date or Calendar object

    public PublicationRequest(String type, PublishableDocument document, String username) {
        this.username = username;
        this.type = type;
        reason = "";
        if (document != null) {
            reference = document;
        }
    }

    public PublicationRequest(String type, PublishableDocument document, String username, Date scheduledDate) {
        this.username = username;
        this.type = type;
        reason = "";
        if (document != null) {
            reference = document;
        }
        reqdate = scheduledDate.getTime();
    }

    String getType() {
        return type;
    }

    String getOwner() {
        return username;
    }

    Date getScheduledDate() {
        return new Date(reqdate);
    }

    void setRejected(PublishableDocument stale, String reason) {
        type = REJECTED;
        reference = stale;
        this.reason = reason;
    }

    void setRejected(String reason) {
        type = REJECTED;
        reference = null;
        this.reason = reason;
    }
    
    Document getReference() {
        return reference;
    }
}
