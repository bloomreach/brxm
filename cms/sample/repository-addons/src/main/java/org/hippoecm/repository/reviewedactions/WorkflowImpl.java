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

import java.lang.reflect.Field;
import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;

import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowMappingException;
import org.hippoecm.repository.servicing.ServicingNodeImpl;

// FIXME: workaround for current mapping issues
public abstract class WorkflowImpl extends org.hippoecm.repository.workflow.WorkflowImpl
{
    public WorkflowImpl() throws RemoteException {
        super();
    }
    /* These are used to overcome shortcomings in the mapping layer
     * implementation at this time.
     */
    public void pre() throws RepositoryException {
        try {
            Node handle = (ServicingNodeImpl.unwrap(((WorkflowImpl)this).node)).getParent();

            String type = handle.getNode("request").getProperty("type").getString();
            String username = handle.getNode("request").getProperty("username").getString();
            PublicationRequest request = new PublicationRequest(type, username);
            request.reason = handle.getNode("request").getProperty("reason").getString();

            PublishableDocument current = new PublishableDocument();
            current.state = handle.getNode(handle.getName()+"[@state='unpublished']").getProperty("type").getString();

            PublishableDocument editing = new PublishableDocument();
            editing.state = handle.getNode(handle.getName()+"[@state='draft']").getProperty("type").getString();

            PublishableDocument published = new PublishableDocument();
            published.state = handle.getNode(handle.getName()+"[@state='published']").getProperty("type").getString();

            Field[] fields = getClass().getFields();
            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("request"))
                    fields[i].set(this, request);
                else if(fields[i].getName().equals("published"))
                    fields[i].set(this, published);
                else if(fields[i].getName().equals("current"))
                    fields[i].set(this, current);
                else if(fields[i].getName().equals("editing"))
                    fields[i].set(this, editing);
            }
        } catch(IllegalAccessException ex) {
            throw new RepositoryException("implementation problem", ex);
        }
    }
    public void post() throws RepositoryException {
        try {
            Node handle = (ServicingNodeImpl.unwrap(((WorkflowImpl)this).node)).getParent();

            PublicationRequest request = null;
            PublishableDocument current = null;
            PublishableDocument editing = null;
            PublishableDocument published = null;

            Field[] fields = getClass().getFields();
            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("request"))
                    request = (PublicationRequest) fields[i].get(this);
                else if(fields[i].getName().equals("published"))
                    published = (PublishableDocument) fields[i].get(this);
                else if(fields[i].getName().equals("current"))
                    current = (PublishableDocument) fields[i].get(this);
                else if(fields[i].getName().equals("editing"))
                    editing = (PublishableDocument) fields[i].get(this);
            }

            // FIXME: This doesn't take into account the cloning of node trees

            if(request.type != null)
                handle.getNode("request").getProperty("type").setValue(request.type);
            else
                handle.getNode("request").remove();

            if(current.state != null)
                handle.getNode(handle.getName()+"[@state='unpublished']").getProperty("type").setValue(current.state);
            else
                handle.getNode(handle.getName()+"[@state='unpublished']").remove();

            if(editing.state != null)
                handle.getNode(handle.getName()+"[@state='draft']").getProperty("type").setValue(editing.state);
            else
                handle.getNode(handle.getName()+"[@state='draft']").remove();

            if(published.state != null)
                handle.getNode(handle.getName()+"[@state='published']").getProperty("type").setValue(published.state);
            else
                handle.getNode(handle.getName()+"[@state='published']").remove();
        } catch(IllegalAccessException ex) {
            throw new RepositoryException("implementation problem", ex);
        }
    }
}
