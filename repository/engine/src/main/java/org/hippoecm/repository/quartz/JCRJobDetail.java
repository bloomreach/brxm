package org.hippoecm.repository.quartz;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.quartz.JobDetail;

public abstract class JCRJobDetail extends JobDetail {

    private static final String HIPPOSCHED_DATA = "hipposched:data";

    protected JCRJobDetail(Node jobNode, Class jobClass) throws RepositoryException {
        super(jobNode.getIdentifier(), jobClass);
    }

    public String getIdentifier() {
        return getName();
    }

    public void persist(Node node) throws RepositoryException {
        node.setProperty(HIPPOSCHED_DATA, JcrUtils.createBinaryValueFromObject(node.getSession(), this));
    }

}
