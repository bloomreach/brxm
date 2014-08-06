/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.gallery.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.gallery.GalleryWorkflow;

import static org.hippoecm.repository.HippoStdNodeType.HIPPOSTD_GALLERYTYPE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_AVAILABILITY;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.onehippo.repository.util.JcrConstants.JCR_DATA;
import static org.onehippo.repository.util.JcrConstants.JCR_LAST_MODIFIED;
import static org.onehippo.repository.util.JcrConstants.JCR_MIME_TYPE;
import static org.onehippo.repository.util.JcrConstants.MIX_REFERENCEABLE;
import static org.onehippo.repository.util.JcrConstants.NT_BASE;


public class GalleryWorkflowImpl implements InternalWorkflow, GalleryWorkflow {

    private Session rootSession;
    private Node subject;

    public GalleryWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.rootSession = rootSession;
    }

    public Map<String,Serializable> hints() {
        return null;
    }

    public List<String> getGalleryTypes() throws RemoteException, RepositoryException {
        List<String> list = new LinkedList<String>();
        Value[] values = subject.getProperty(HIPPOSTD_GALLERYTYPE).getValues();
        for (final Value value : values) {
            list.add(value.getString());
        }
        return list;
    }

    public Document createGalleryItem(String name, String type) throws RemoteException, RepositoryException, WorkflowException {
        Node folder = rootSession.getNodeByIdentifier(subject.getIdentifier());
        name = NodeNameCodec.encode(name);
        if (isSameNameSibling(name, folder)){
            throw new WorkflowException(MessageFormat.format(
                    "A node with name {0} already exists in folder {1}. Not allowed to create same-name siblings",
                    name, folder.getPath()));
        }
        final Node handle = folder.addNode(name, NT_HANDLE);
        handle.addMixin(MIX_REFERENCEABLE);
        final Node document = handle.addNode(name, type);
        document.setProperty(HIPPO_AVAILABILITY, new String[] { "live", "preview" });

        final String primaryItemName = getPrimaryItemName(document);
        if (primaryItemName != null) {
            final Node primaryItem;
            if (!document.hasNode(primaryItemName)) {
                primaryItem = document.addNode(primaryItemName);
            } else {
                primaryItem = document.getNode(primaryItemName);
            }
            primaryItem.setProperty(JCR_DATA, "");
            primaryItem.setProperty(JCR_MIME_TYPE, "application/octet-stream");
            primaryItem.setProperty(JCR_LAST_MODIFIED, Calendar.getInstance());
        } else {
            throw new ItemNotFoundException("No primary item definition found");
        }
        rootSession.save();
        return new Document(document);
    }

    private String getPrimaryItemName(final Node document) throws RepositoryException {
        NodeType primaryType = document.getPrimaryNodeType();
        String primaryItemName = primaryType.getPrimaryItemName();
        while (primaryItemName == null && !NT_BASE.equals(primaryType.getName())) {
            for (NodeType nt : primaryType.getSupertypes()) {
                if (nt.getPrimaryItemName() != null) {
                    primaryItemName = nt.getPrimaryItemName();
                    break;
                }
                if (nt.isNodeType(NT_BASE)) {
                    primaryType = nt;
                }
            }
        }
        return primaryItemName;
    }

    private boolean isSameNameSibling(final String name, final Node folder) throws RepositoryException {
        return folder.hasNode(name);
    }
}
