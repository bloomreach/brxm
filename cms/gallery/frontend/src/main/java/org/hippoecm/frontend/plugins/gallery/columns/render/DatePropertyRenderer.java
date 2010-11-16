package org.hippoecm.frontend.plugins.gallery.columns.render;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Date;

public class DatePropertyRenderer extends PropertyRenderer<Date> {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public DatePropertyRenderer(String prop) {
        super(prop);
    }

    public DatePropertyRenderer(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected Date getValue(Property p) throws RepositoryException {
        return p.getDate().getTime();
    }
}
