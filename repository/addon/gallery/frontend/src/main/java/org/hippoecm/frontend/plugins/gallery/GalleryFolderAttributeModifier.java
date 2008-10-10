package org.hippoecm.frontend.plugins.gallery;

import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeAttributeModifier;
import org.hippoecm.frontend.plugins.standards.list.resolvers.CssClassAppender;
import org.hippoecm.frontend.plugins.standards.list.resolvers.IListAttributeModifier;
import org.hippoecm.repository.api.HippoNode;

public class GalleryFolderAttributeModifier extends AbstractNodeAttributeModifier implements IListAttributeModifier {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: $";
    private static final long serialVersionUID = 1L;

    @Override
    protected AttributeModifier getCellAttributeModifier(HippoNode node) throws RepositoryException {
        String cssClass = "";
        if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
            cssClass = "folder-48";
        }
        return new CssClassAppender(new Model(cssClass));
    }

    @Override
    protected AttributeModifier getColumnAttributeModifier(HippoNode node) throws RepositoryException {
        return new CssClassAppender(new Model("icon-48"));
    }

}
