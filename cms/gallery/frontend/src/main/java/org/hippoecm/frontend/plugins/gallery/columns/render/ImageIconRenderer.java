package org.hippoecm.frontend.plugins.gallery.columns.render;

import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.plugins.standards.icon.BrowserStyle;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IconRenderer;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.repository.api.HippoNodeType;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class ImageIconRenderer extends IconRenderer {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    @Override
    protected ResourceReference getResourceReference(Node node) throws RepositoryException {
        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return BrowserStyle.getIcon("image", IconSize.TINY);
        }
        return super.getResourceReference(node);
    }
}
