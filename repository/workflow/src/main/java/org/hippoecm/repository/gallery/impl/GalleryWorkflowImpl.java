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
package org.hippoecm.repository.gallery.impl;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
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
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.gallery.GalleryWorkflow;

// FIXME: this implementation should be totally rewritten as it should not
// implement InternalWorkflow, but could and should be a plain POJO workflow.

public class GalleryWorkflowImpl implements InternalWorkflow, GalleryWorkflow
{

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
        Value[] values = subject.getProperty("hippostd:gallerytype").getValues();
        for (final Value value : values) {
            list.add(value.getString());
        }
        return list;
    }

    public Document createGalleryItem(String name, String type) throws RemoteException, RepositoryException {
        // FIXME: this implementation is totally hardcoded and unlike the workflow in the FolderWorkflowImpl cannot be
        // customized with auto created properties, like user, current time, and -most importantly- also not the
        // hippo:availability property.  This implementation should be revoked entirely.
        Node document, node, folder = rootSession.getNodeByUUID(subject.getUUID());
        Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(new Date());
        name = NodeNameCodec.encode(name);
        node = folder.addNode(name, "hippo:handle");
        node.setProperty("hippo:discriminator", new Value[0]);
        node = document = node.addNode(name, type);
        node.addMixin("mix:versionable");
        node.setProperty("hippo:availability", new String[] { "live", "preview" });
        node.setProperty("hippo:paths", new String[0]);

        NodeType primaryType = node.getPrimaryNodeType();
        String primaryItemName = primaryType.getPrimaryItemName();
        while (primaryItemName == null && !"nt:base".equals(primaryType.getName())) {
            for (NodeType nt : primaryType.getSupertypes()) {
                if (nt.getPrimaryItemName() != null) {
                    primaryItemName = nt.getPrimaryItemName();
                    break;
                }
                if (nt.isNodeType("nt:base")) {
                    primaryType = nt;
                }
            }
        }
        if (primaryItemName != null) {
            if (!node.hasNode(primaryItemName)) {
                node = node.addNode(primaryItemName);
            } else {
                node = node.getNode(primaryItemName);
            }
            node.setProperty("jcr:data", "");
            node.setProperty("jcr:mimeType", "application/octet-stream");
            node.setProperty("jcr:lastModified", timestamp);
        } else {
            throw new ItemNotFoundException("No primary item definition found");
        }
        folder.save();
        return new Document(document);
    }
}
