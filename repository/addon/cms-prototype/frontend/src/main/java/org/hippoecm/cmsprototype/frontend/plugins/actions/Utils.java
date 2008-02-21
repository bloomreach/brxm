package org.hippoecm.cmsprototype.frontend.plugins.actions;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;


//Temporary utility class, these kind of methods
//should be centralized

class Utils {

    static Node findHandle(Node node) throws RepositoryException {
        Node result = node;
        while (result != null && !result.isNodeType("rep:root") && !result.isNodeType(HippoNodeType.NT_HANDLE)) {
            result = result.getParent();
        }
        if (result != null && !result.isNodeType(HippoNodeType.NT_HANDLE)) {
            result = node;
        }
        return result;

    }

}
