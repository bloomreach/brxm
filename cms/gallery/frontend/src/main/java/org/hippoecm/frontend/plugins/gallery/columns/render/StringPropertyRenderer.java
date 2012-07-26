package org.hippoecm.frontend.plugins.gallery.columns.render;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class StringPropertyRenderer extends PropertyRenderer<String> {

    public StringPropertyRenderer(String prop, String relPath) {
        super(prop, relPath);
    }

    public StringPropertyRenderer(String prop) {
        super(prop);
    }

    @Override
    protected String getValue(Property p) throws RepositoryException {
        return p.getString();

    }

}
