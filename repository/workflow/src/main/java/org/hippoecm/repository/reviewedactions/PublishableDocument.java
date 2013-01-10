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
import javax.jdo.annotations.DatastoreIdentity;
import javax.jdo.annotations.Discriminator;
import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;

import org.hippoecm.repository.api.Document;

@PersistenceCapable(identityType=IdentityType.DATASTORE,cacheable="false",detachable="false")
@DatastoreIdentity(strategy=IdGeneratorStrategy.NATIVE)
@Inheritance(strategy=InheritanceStrategy.SUBCLASS_TABLE)
@Discriminator(strategy=DiscriminatorStrategy.CLASS_NAME)
public class PublishableDocument extends Document {

    final public static String PUBLISHED = "published";
    final public static String UNPUBLISHED = "unpublished";
    final public static String DRAFT = "draft";
    final public static String STALE = "stale";

    @Persistent(column="hippostd:state")
    private String state;

    @Persistent(column="hippostd:holder")
    private String username;

    @Persistent(column="hippostdpubwf:publicationDate")
    private Date publicationDate;

    @Persistent(column="hippostdpubwf:lastModifiedBy")
    private String lastModifiedBy;

    @Persistent(column="hippostdpubwf:lastModificationDate")
    private Date lastModificationDate;

    @Persistent(column="hippostdpubwf:creationDate")
    private Date creationDate;

    @Persistent(column="hippostdpubwf:createdBy")
    private String createdBy;

    @Persistent(embedded="true", defaultFetchGroup="true", serialized="true",column="hippo:availability")
    private String[] availability;

    public PublishableDocument() {
        this.state = UNPUBLISHED;
        this.availability = null;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PublishableDocument clonedDocument = (PublishableDocument) super.clone();
        clonedDocument.creationDate = creationDate;
        clonedDocument.createdBy = createdBy;
        clonedDocument.lastModificationDate = lastModificationDate;
        clonedDocument.lastModifiedBy = lastModifiedBy;
        clonedDocument.publicationDate = null;
        clonedDocument.availability = new String[0];
        return clonedDocument;
    }

   public void setState(String state) {
        if (!state.equals(this.state)) {
           this.state = state;
        }
    }

    public String getState() {
        return state;
    }

    public void setPublicationDate(Date date) {
        this.publicationDate = date;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setOwner(String username) {
        this.username = username;
    }

    public String getOwner() {
        return username;
    }

    public void setAvailability(String[] availability) {
        this.availability = availability;
    }

    public String[] getAvailability() {
        return availability;
    }

    public void setModified(String username) {
        lastModifiedBy = username;
        lastModificationDate = new Date();
    }

}
