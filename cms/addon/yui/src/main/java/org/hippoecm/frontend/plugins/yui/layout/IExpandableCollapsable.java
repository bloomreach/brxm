package org.hippoecm.frontend.plugins.yui.layout;

import java.io.Serializable;

public interface IExpandableCollapsable extends Serializable {
    final static String SVN_ID = "$Id$";

    boolean isSupported();

    void expand();

    void collapse();
}
