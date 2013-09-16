package org.onehippo.cms7.essentials.components.gui.panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class DocumentTemplateModel {

    private static Logger log = LoggerFactory.getLogger(DocumentTemplateModel.class);
    private String namespace;
    private String name;

    public DocumentTemplateModel(final String namespace, final String name) {

        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(final String namespace) {
        this.namespace = namespace;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
