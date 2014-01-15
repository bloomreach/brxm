package org.onehippo.cms7.essentials.rest.model.contentblocks;

import java.io.Serializable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.onehippo.cms7.essentials.dashboard.contentblocks.ContentBlocksPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class ContentBlockModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(ContentBlockModel.class);

    private String provider;
    private ContentBlocksPlugin.Type type;
    private String documentType;
    private ContentBlocksPlugin.Prefer prefer;
    private String name;

    public ContentBlockModel(final String provider, final ContentBlocksPlugin.Prefer prefer, final ContentBlocksPlugin.Type type, final String name, final String documentType) {
        this.provider = provider;
        this.type = type;
        this.documentType = documentType;
        this.prefer = prefer;
        this.name = name;
    }

    public ContentBlockModel(Node node) {
        try {
            final String myProvider = getProperty(node, "cpItemsPath").getString();
            final Node _default_ = node.getParent();
            final Property myDocumentType = getProperty(_default_, "type");
            final String myName = node.getName();
            setName(myName);
            setDocumentType(myDocumentType.getString());
            setProvider(myProvider);
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to populate content blocks added list. Check if the property \"type\" exists on your _default_ node in you namespace template {}", e);
        }
    }

    private Property getProperty(final Node node, final String property) throws RepositoryException {
        if (node.hasProperty(property)) {
            return node.getProperty(property);
        }
        return null;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public ContentBlocksPlugin.Type getType() {
        return type;
    }

    public void setType(final ContentBlocksPlugin.Type type) {
        this.type = type;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(final String documentType) {
        this.documentType = documentType;
    }

    public ContentBlocksPlugin.Prefer getPrefer() {
        return prefer;
    }

    public void setPrefer(final ContentBlocksPlugin.Prefer prefer) {
        this.prefer = prefer;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ContentBlockModel");
        sb.append("{provider='").append(provider).append('\'');
        sb.append(", type=").append(type);
        sb.append(", documentType='").append(documentType).append('\'');
        sb.append(", prefer=").append(prefer);
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
