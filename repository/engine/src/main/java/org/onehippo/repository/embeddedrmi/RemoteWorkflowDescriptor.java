package org.onehippo.repository.embeddedrmi;

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;

public class RemoteWorkflowDescriptor implements Serializable {
    String category;
    String identifier;
    String display;
    Map<String, String> attributes;
    Map<String, Serializable> hints;
    String[] interfaces;

    RemoteWorkflowDescriptor(WorkflowDescriptor workflowDescriptor, String category, String identifier) throws RepositoryException {
        this.category = category;
        this.identifier = identifier;
        this.display = workflowDescriptor.getDisplayName();
        this.attributes = new TreeMap<String, String>();
        for (String attributeKey : workflowDescriptor.getAttribute(null).split(" "))
            this.attributes.put(attributeKey, workflowDescriptor.getAttribute(attributeKey));
        this.hints = workflowDescriptor.hints();
        try {
            Class<Workflow>[] workflowInterfaces = workflowDescriptor.getInterfaces();
            if (workflowInterfaces != null) {
                this.interfaces = new String[workflowInterfaces.length];
                for (int i = 0; i < workflowInterfaces.length; i++) {
                    this.interfaces[i] = workflowInterfaces[i].getName();
                }
            } else {
                this.interfaces = null;
            }
        } catch (ClassNotFoundException ex) {
            this.interfaces = null;
        }
    }
}
