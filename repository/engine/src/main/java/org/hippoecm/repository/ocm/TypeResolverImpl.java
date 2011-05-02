package org.hippoecm.repository.ocm;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.hippoecm.repository.api.HippoNodeType;

public class TypeResolverImpl implements TypeResolver {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ";
    
    private Node types;

    public TypeResolverImpl(Node types) {
        this.types = types;
    }

    public String[] resolve(String className) throws RepositoryException {
        String primaryType = types.getNode(className).getProperty("hipposys:nodetype").getString();
        NodeType nodeType = types.getSession().getWorkspace().getNodeTypeManager().getNodeType(primaryType);
        if (nodeType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            return new String[] {primaryType, HippoNodeType.NT_HARDDOCUMENT};
        } else if (nodeType.isNodeType(HippoNodeType.NT_REQUEST)) {
            return new String[] {primaryType, "mix:referenceable"};
        } else {
            return new String[] {primaryType};
        }
    }
}
