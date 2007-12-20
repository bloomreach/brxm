package org.hippoecm.cmsprototype.frontend.model.content;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;

public class DocumentVariant extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    // TODO: Replace with HippoNodeType when available: HREPTWO-342
    private static final String HIPPO_LANGUAGE = "language";
    private static final String HIPPO_STATE = "state";

    // Default labels
    // TODO: needs i18m
    private static final String NO_STATE = "no workflow";
    private static final String NO_LANGUAGE = "all languages";

    public DocumentVariant(JcrNodeModel nodeModel) {
        super(nodeModel);
    }
    
    public String getName() {
        try {
            return nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String getState() {
        try {
            if (nodeModel.getNode().hasProperty(HIPPO_STATE)) {
                return nodeModel.getNode().getProperty(HIPPO_STATE).getString();
            }
            else {
                return NO_STATE;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String getLanguage() {
        try {
            if (nodeModel.getNode().hasProperty(HIPPO_LANGUAGE)) {
                return nodeModel.getNode().getProperty(HIPPO_LANGUAGE).getString();
            }
            else {
                return NO_LANGUAGE;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
}
