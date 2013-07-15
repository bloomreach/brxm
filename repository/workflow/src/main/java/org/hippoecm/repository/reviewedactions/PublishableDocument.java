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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;

public class PublishableDocument extends Document {

    final public static String PUBLISHED = "published";
    final public static String UNPUBLISHED = "unpublished";
    final public static String DRAFT = "draft";
    final public static String STALE = "stale";

    public PublishableDocument() {
    }

    public PublishableDocument(Node node) throws RepositoryException {
        super(node);
    }

   public void setState(String state) throws RepositoryException {
       setNodeStringProperty("hippostd:state", state);
    }

    public String getState() throws RepositoryException {
        return getNodeStringProperty("hippostd:state");
    }

    public void setPublicationDate(Date date) throws RepositoryException {
        setNodeDateProperty("hippostdpubwf:publicationDate", date);
    }

    public Date getPublicationDate() throws RepositoryException {
        return getNodeDateProperty("hippostdpubwf:publicationDate");
    }

    public void setOwner(String username) throws RepositoryException {
        setNodeStringProperty("hippostd:holder", username);
    }

    public String getOwner() throws RepositoryException {
        return getNodeStringProperty("hippostd:holder");
    }

    public void setAvailability(String[] availability) throws RepositoryException {
        setNodeStringsProperty("hippo:availability", availability);
    }

    public String[] getAvailability() throws RepositoryException {
        return getNodeStringsProperty("hippo:availability");
    }

    public void setModified(String username) throws RepositoryException{
        setNodeStringProperty("hippostdpubwf:lastModifiedBy", username);
        setNodeDateProperty("hippostdpubwf:lastModificationDate", new Date());
    }
}
