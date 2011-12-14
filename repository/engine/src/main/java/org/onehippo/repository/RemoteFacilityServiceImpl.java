package org.onehippo.repository;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.hippoecm.repository.HierarchyResolverImpl;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.impl.DocumentManagerImpl;
import org.hippoecm.repository.impl.WorkflowManagerImpl;

public class RemoteFacilityServiceImpl implements ManagerService {
    Session session, rootSession;
    DocumentManagerImpl documentManager = null;
    WorkflowManagerImpl workflowManager = null;
    HierarchyResolver hierarchyResolver = null;
    //WeakHashMap<Node,String> nodes = new WeakHashMap<Node,String>();

    public RemoteFacilityServiceImpl(Session session) {
        this.session = session;
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        if (documentManager == null) {
            documentManager = new DocumentManagerImpl(session);
        }
        return documentManager;
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        try {
            if (workflowManager == null) {
                rootSession = session.impersonate(new SimpleCredentials("workflowuser", new char[] {}));
                workflowManager = new WorkflowManagerImpl(session, rootSession);
            }
            return workflowManager;
        } catch (LoginException ex) {
            throw ex;
        } catch (RepositoryException ex) {
            throw ex;
        }
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        if (hierarchyResolver == null) {
            hierarchyResolver = new HierarchyResolverImpl();
        }
        return hierarchyResolver;
    }

    @Override
    public void close() {
        if (workflowManager != null) {
            workflowManager.close();
        }
        if (documentManager != null) {
            documentManager.close();
        }
        if (rootSession != null) {
            rootSession.logout();
        }
        session = rootSession = null;
        workflowManager = null;
        hierarchyResolver = null;
    }

    static class RemoteWorkflowDescriptor implements Serializable {
        String category;
        String identifier;
        String display;
        Map<String,String> attributes;
        Map<String,Serializable> hints;
        String[] interfaces;
        RemoteWorkflowDescriptor(WorkflowDescriptor workflowDescriptor, String category, String identifier) throws RepositoryException {
            this.category = category;
            this.identifier = identifier;
            this.display = workflowDescriptor.getDisplayName();
            this.attributes = new TreeMap<String,String>();
            for(String attributeKey : workflowDescriptor.getAttribute(null).split(" "))
                this.attributes.put(attributeKey, workflowDescriptor.getAttribute(attributeKey));
            this.hints = workflowDescriptor.hints();
        }
    }

    class ClientWorkflowDescriptor implements WorkflowDescriptor {
        RemoteWorkflowDescriptor remote;
        Class<Workflow>[] interfaces;
        public ClientWorkflowDescriptor(RemoteWorkflowDescriptor workflowDescriptor) throws MappingException {
            this.remote = workflowDescriptor;
            this.interfaces = new Class[workflowDescriptor.interfaces.length];
            for(int i=0; i<interfaces.length; i++) {
                try {
                    interfaces[i] = (Class<Workflow>) Class.forName(workflowDescriptor.interfaces[i]);
                } catch(ClassNotFoundException ex) {
                    throw new MappingException(workflowDescriptor.interfaces[i]+" class not found", ex);
                }
            }
        }
        public String getDisplayName() throws RepositoryException {
            return remote.display;
        }
        public Class<Workflow>[] getInterfaces() throws ClassNotFoundException, RepositoryException {
            return interfaces;
        }
        public String getAttribute(String name) throws RepositoryException {
            return remote.attributes.get(name);
        }
        public Map<String, Serializable> hints() throws RepositoryException {
            return remote.hints;
        }
    }

    static interface RemoteWorkflowManager {
        public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String identifier) throws MappingException, RepositoryException, RemoteException;
        public RemoteWorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException, RemoteException;
        public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws MappingException, RepositoryException, RemoteException;
    }

    class ClientWorkflowManager implements WorkflowManager {
        RemoteWorkflowManager remote;
        ClientWorkflowManager(RemoteWorkflowManager workflowManager) {
            remote = workflowManager;
        }
        public Session getSession() throws RepositoryException {
            return session;
        }
        public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
            try {
                return new ClientWorkflowDescriptor(remote.getWorkflowDescriptor(category, item.getIdentifier()));
            } catch(RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
        public WorkflowDescriptor getWorkflowDescriptor(String category, Document document) throws RepositoryException {
            try {
                return new ClientWorkflowDescriptor(remote.getWorkflowDescriptor(category, document.getIdentity()));
            } catch(RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
        public Workflow getWorkflow(String category, Node item) throws MappingException, RepositoryException {
            try {
                return remote.getWorkflow(remote.getWorkflowDescriptor(category, item.getIdentifier()));
            } catch (RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
        public Workflow getWorkflow(String category, Document document) throws MappingException, RepositoryException {
            try {
                return remote.getWorkflow(remote.getWorkflowDescriptor(category, document.getIdentity()));
            } catch (RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
        public Workflow getWorkflow(WorkflowDescriptor descriptor) throws MappingException, RepositoryException {
            try {
                return remote.getWorkflow((RemoteWorkflowDescriptor)descriptor);
            } catch (RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
        public WorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException {
            try {
                return new ClientWorkflowManager(remote.getContextWorkflowManager(specification));
            } catch (RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    class ServerWorkflowManager implements RemoteWorkflowManager {
        WorkflowManager local;
        ServerWorkflowManager(WorkflowManager workflowManager) {
            local = workflowManager;
        }
        public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String identifier) throws MappingException, RepositoryException, RemoteException {
            return new RemoteWorkflowDescriptor(local.getWorkflowDescriptor(category, local.getSession().getNodeByIdentifier(identifier)), category, identifier);
        }

        @Override
        public RemoteWorkflowManager getContextWorkflowManager(Object specification) throws MappingException, RepositoryException, RemoteException {
            return new ServerWorkflowManager(local.getContextWorkflowManager(specification));
        }

        @Override
        public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor) throws MappingException, RepositoryException, RemoteException {
            return local.getWorkflow(descriptor.category, local.getSession().getNodeByIdentifier(descriptor.identifier));
        }
        
    }
    
    static interface RemoteDocumentManager {
        public Document getDocument(String category, String identifier) throws RepositoryException, RemoteException;
    }

    class ClientDocumentManager implements DocumentManager {
        RemoteDocumentManager remote;
        public Session getSession() throws RepositoryException {
            return session;
        }
        public Document getDocument(String category, String identifier) throws RepositoryException {
            try {
                return remote.getDocument(category, identifier);
            } catch(RemoteException ex) {
                throw new RepositoryException(ex);
            }
        }
    }

    class ServerDocumentManager implements RemoteDocumentManager {
        DocumentManager local;
        ServerDocumentManager(DocumentManager documentManager) {
            local = documentManager;
        }
        public Document getDocument(String category, String identifier) throws RepositoryException, RemoteException {
            return local.getDocument(category, identifier);
        }
    }
    
    class RemoteHierarchyResolver implements HierarchyResolver {
        public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public Item getItem(Node ancestor, String path) throws InvalidItemStateException, RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public Property getProperty(Node node, String field) throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
