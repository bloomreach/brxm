package org.hippoecm.frontend.plugins.gallery.columns.compare;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Calendar;

public class CalendarComparator extends PropertyComparator {

    public CalendarComparator(String prop) {
        super(prop);
    }

    public CalendarComparator(String prop, String relPath) {
        super(prop, relPath);
    }

    @Override
    protected int compare(Property p1, Property p2) {
        try {
            Calendar c1 = p1.getDate();
            Calendar c2 = p2.getDate();
            if (c1 == null) {
                if (c2 == null) {
                    return 0;
                }
                return 1;
            } else if (c2 == null) {
                return -1;
            }
            return c1.compareTo(c2);
        } catch (RepositoryException e) {
        }
        return 0;
    }
}
