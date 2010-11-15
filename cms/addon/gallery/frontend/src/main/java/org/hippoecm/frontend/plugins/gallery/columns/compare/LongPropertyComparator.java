package org.hippoecm.frontend.plugins.gallery.columns.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class LongPropertyComparator extends PropertyComparator {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    public LongPropertyComparator(String prop) {
        super(prop);
    }

    public LongPropertyComparator(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(Property p1, Property p2) {
        try {
            Long l1 = p1.getLong();
            Long l2 = p2.getLong();
            return l1.compareTo(l2);
        } catch (RepositoryException e) {
        }
        return 0;
    }
}
