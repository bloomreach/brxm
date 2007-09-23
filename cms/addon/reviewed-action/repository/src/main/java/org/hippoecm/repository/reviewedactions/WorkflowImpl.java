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
import javax.jcr.Session;

import org.hippoecm.repository.Utilities;
import org.hippoecm.repository.api.HippoWorkspace;
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

    protected Node node;
    protected Node handle;
    private ReviewedActionsWorkflowImpl document = null;
    private PublishableDocument draft = null;
    private PublishableDocument unpublished = null;
    private PublishableDocument published = null;
    private PublicationRequest current = null;

    /* These are used to overcome shortcomings in the mapping layer
     * implementation at this time.
     */
    public void pre() throws RepositoryException {
        /* Because these objects seem to be recycled (fucking early optimizations)
         * we need to clear the objects outselves.
         */
        document = null;
        draft = null;
        unpublished = null;
        published = null;
        current = null;
        Session session = getWorkflowContext().session;
        try {
            Node n;
            Field[] fields = getClass().getFields();
            fields = getClass().getFields();
            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("a")) {
                    node = session.getNodeByUUID((String)(fields[i].get(this)));
                    break;
                }
            }
            handle = (ServicingNodeImpl.unwrap(((WorkflowImpl)this).node)).getParent();

            if((n = Utilities.getNode(handle, handle.getName()+"[state='unpublished']")) != null) {
                unpublished = new PublishableDocument();
                unpublished.state = n.getProperty("state").getString();
            }

            if((n = Utilities.getNode(handle, handle.getName()+"[state='draft']")) != null) {
                draft = new PublishableDocument();
                draft.state = n.getProperty("state").getString();
            }

            if((n = Utilities.getNode(handle, handle.getName()+"[state='published']")) != null) {
                published = new PublishableDocument();
                published.state = n.getProperty("state").getString();
            }

            Node currentNode;
            if((currentNode = Utilities.getNode(handle, "request[type='publish']")) == null)
                if((currentNode = Utilities.getNode(handle, "request[type='depublish']")) == null)
                    currentNode = Utilities.getNode(handle, "request[type='delete']");
                
            if(currentNode != null) {
                String type = currentNode.getProperty("type").getString();
                String username = currentNode.getProperty("username").getString();
                current = new PublicationRequest(type, draft, username);
                current.reason = currentNode.getProperty("reason").getString();
            }

            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("current")) {
                    fields[i].set(this, current);
                } else if(fields[i].getName().equals("published"))
                    fields[i].set(this, published);
                else if(fields[i].getName().equals("unpublished"))
                    fields[i].set(this, unpublished);
                else if(fields[i].getName().equals("draft"))
                    fields[i].set(this, draft);
                /*else if(fields[i].getName().equals("document")) {
                    HippoWorkspace wsp = (HippoWorkspace) (((WorkflowImpl)this).node).getSession().getWorkspace();
                    n = Utilities.getNode(handle, handle.getName()+"[state='draft']");
                    if(n == null)
                      n = Utilities.getNode(handle, handle.getName()+"[state='stale']");
                    ReviewedActionsWorkflowImpl document = (ReviewedActionsWorkflowImpl) wsp.getWorkflowManager().getWorkflow("default", n);
((RequestWorkflowImpl)this).document = document;
                    fields[i].set(this, document);
                }*/
            }

        } catch(IllegalAccessException ex) {
            throw new RepositoryException("implementation problem", ex);
        }
    }
    public void post() throws RepositoryException {
        Session session = getWorkflowContext().session;
        try {
            Field[] fields = getClass().getFields();

            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("a")) {
                    node = session.getNodeByUUID((String) fields[i].get(this));
                    break;
                }
            }
            handle = (ServicingNodeImpl.unwrap(((WorkflowImpl)this).node)).getParent();

            PublicationRequest current = null;
            PublishableDocument draft = null;
            PublishableDocument unpublished = null;
            PublishableDocument published = null;
            boolean hasCurrent = false;
            boolean hasUnpublished = false;
            boolean hasDraft = false;
            boolean hasPublished = false;

            for(int i=0; i<fields.length; i++) {
                if(fields[i].getName().equals("current")) {
                    current = (PublicationRequest) fields[i].get(this);
                    hasCurrent = true;
                } else if(fields[i].getName().equals("published")) {
                    published = (PublishableDocument) fields[i].get(this);
                    hasPublished = true;
                } else if(fields[i].getName().equals("unpublished")) {
                    unpublished = (PublishableDocument) fields[i].get(this);
                    hasUnpublished = true;
                } else if(fields[i].getName().equals("draft")) {
                    draft = (PublishableDocument) fields[i].get(this);
                    hasDraft = true;
                }
            }

            if(unpublished != null && unpublished.cloned != null) {
                if(unpublished.cloned == this.draft) {
                    Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='unpublished']"),
                                   handle.getPath()+"/"+handle.getName());
                } else if(unpublished.cloned == this.published) {
                } else
                    throw new RepositoryException("Internal error");
            }
            if(draft != null && draft.cloned != null) {
            }
            if(published != null && published.cloned != null) {
            }

            if(current != null && current.cloned != null) {
                throw new RepositoryException("Internal error");
            }
            if(current != null) {
                Node currentNode;
                if((currentNode = Utilities.getNode(handle, "request[type='publish']")) == null &&
                   (currentNode = Utilities.getNode(handle, "request[type='depublish']")) == null &&
                   (currentNode = Utilities.getNode(handle, "request[type='delete']")) == null) {
                    currentNode = handle.addNode("request","hippo:request");
                }
                currentNode.setProperty("type", current.type);
                currentNode.setProperty("username", current.username);
                currentNode.setProperty("reason", current.reason);
            } else if(hasCurrent) {
                /*
                Node currentNode;
                if((currentNode = Utilities.getNode(handle, "request[type='publish']")) != null ||
                   (currentNode = Utilities.getNode(handle, "request[type='depublish']")) != null ||
                   (currentNode = Utilities.getNode(handle, "request[type='delete']")) != null) {
                    currentNode.remove();
                }
                */
            }

            if(unpublished != null) {
                Node newNode = null, oldNode = Utilities.getNode(handle, handle.getName()+"[state='unpublished']");
                if(unpublished.cloned != null)
                    if(unpublished.cloned == this.draft) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='draft']"),
                                       handle.getPath()+"/"+handle.getName());
                    } else if(unpublished.cloned == this.published) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='published']"),
                                                 handle.getPath()+"/"+handle.getName());
                    } else
                        throw new RepositoryException("Internal error");
                if(newNode != null) {
                    if(oldNode != null)
                        oldNode.remove();
                    oldNode = newNode;
                }
                if(oldNode != null)
                    oldNode.getProperty("state").setValue(unpublished.state);
            } else if(Utilities.getNode(handle, handle.getName()+"[state='unpublished']") != null && hasUnpublished)
                Utilities.getNode(handle, handle.getName()+"[state='unpublished']").remove();

            if(draft != null) {
                Node newNode = null, oldNode = Utilities.getNode(handle, handle.getName()+"[state='draft']");
                if(draft.cloned != null) {
                    if(draft.cloned == this.unpublished) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='unpublished']"),
                                                 handle.getPath()+"/"+handle.getName());
                    } else if(draft.cloned == this.published) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='published']"),
                                                 handle.getPath()+"/"+handle.getName());
                    } else
                        throw new RepositoryException("Internal error");
                }
                if(newNode != null) {
                    if(oldNode != null)
                        oldNode.remove();
                    oldNode = newNode;
                }
                if(oldNode != null)
                    oldNode.getProperty("state").setValue(draft.state);
            } else if(Utilities.getNode(handle, handle.getName()+"[state='draft']") != null && hasDraft)
                Utilities.getNode(handle, handle.getName()+"[state='draft']").remove();


            if(published != null) {
                Node newNode = null, oldNode = Utilities.getNode(handle, handle.getName()+"[state='published']");
                if(published.cloned != null)
                    if(published.cloned == this.unpublished) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='unpublished']"),
                                                 handle.getPath()+"/"+handle.getName());
                    } else if(published.cloned == this.draft) {
                        newNode = Utilities.copy(Utilities.getNode(handle, handle.getName()+"[state='draft']"),
                                                 handle.getPath()+"/"+handle.getName());
                    } else
                        throw new RepositoryException("Internal error");
                if(newNode != null) {
                    if(oldNode != null)
                        oldNode.remove();
                    oldNode = newNode;
                }
                if(oldNode != null)
                    oldNode.getProperty("state").setValue(published.state);
            } else if(Utilities.getNode(handle, handle.getName()+"[state='published']") != null && hasPublished)
                Utilities.getNode(handle, handle.getName()+"[state='published']").remove();

        } catch(IllegalAccessException ex) {
            throw new RepositoryException("implementation problem", ex);
        }
    }
}
