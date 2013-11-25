/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.scxml.reviewedactions;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;

/**
 * DocumentVariant
 */
public class DocumentVariant {

    private PublishableDocument document;

    public DocumentVariant(PublishableDocument document) {
        this.document = document;
    }

    public PublishableDocument getDocument() {
        return document;
    }

    public void setState(String state) throws RepositoryException {
        document.setState(state);
    }

    public String getState() throws RepositoryException {
        return document.getState();
    }

    public String getStateSummary() throws RepositoryException {
        if (document.getNode() != null) {
            return JcrUtils.getStringProperty(document.getNode(), "hippostd:stateSummary", null);
        }

        return null;
    }

    public void setPublicationDate(Date date) throws RepositoryException {
        document.setPublicationDate(date);
    }

    public Date getPublicationDate() throws RepositoryException {
        return document.getPublicationDate();
    }

    public void setOwner(String username) throws RepositoryException {
        document.setOwner(username);
    }

    public String getOwner() throws RepositoryException {
        return document.getOwner();
    }

    public void setAvailability(String[] availability) throws RepositoryException {
        if (availability != null) {
            document.setAvailability(availability);
        }
        else {
            document.setAvailability(new String[0]);
        }
    }

    public String[] getAvailability() throws RepositoryException {
        return document.getAvailability();
    }

    public boolean isAvailable(String environment) throws RepositoryException {
        return document.isAvailable(environment);
    }

    public void setModified(String username) throws RepositoryException {
        document.setModified(username);
    }

    public Node getNode() {
        return document.getNode();
    }

}
