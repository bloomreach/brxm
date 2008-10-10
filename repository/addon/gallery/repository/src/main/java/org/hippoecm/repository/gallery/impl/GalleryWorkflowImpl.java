/*
 *  Copyright 2008 Hippo.
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

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.ISO9075Helper;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.ext.WorkflowImpl;
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

    public List<String> getGalleryTypes() throws RemoteException, RepositoryException {
        List<String> list = new LinkedList<String>();
        Value[] values = subject.getProperty("hippostd:gallerytype").getValues();
        for(int i=0; i<values.length; i++) {
            list.add(values[i].getString());
        }
        return list;
    }

    public Document createGalleryItem(String name, String type) throws RemoteException, RepositoryException {
        Node document, node, folder = rootSession.getNodeByUUID(subject.getUUID());
        Date date = new Date();
        Calendar timestamp = Calendar.getInstance();
        timestamp.setTime(new Date());
        name = ISO9075Helper.encodeLocalName(name);
        node = folder.addNode(name, "hippo:handle");
        node.addMixin("hippo:hardhandle");
        node.setProperty("hippo:discriminator", new Value[0]);
        node = document = node.addNode(name, type);
        node.addMixin("hippo:harddocument");
        node = (Node) node.getPrimaryItem();
        node.setProperty("jcr:data", "");
        node.setProperty("jcr:mimeType", "text/plain");
        node.setProperty("jcr:lastModified", timestamp);
        folder.save();
        return new Document(document.getUUID());
    }
}
