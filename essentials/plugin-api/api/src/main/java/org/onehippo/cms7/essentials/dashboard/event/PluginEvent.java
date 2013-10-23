package org.onehippo.cms7.essentials.dashboard.event;

import java.io.Serializable;

/**
 * @version "$Id$"
 */
public interface PluginEvent extends Serializable {

    /**
     * in case of change events, indicates if plugin itself can undo changes
     *
     * @return true if change event and change can be reverted
     */
    boolean canUndo();

    /**
     *
     */
    void setCanUndo(boolean canUndo);

    /**
     * Human readable message
     *
     * @return message which can be displayed to users
     */
    String getMessage();

    /**
     * Location where to display event message
     *
     * @return DisplayLocation enum
     */
    DisplayLocation getDisplayLocation();

}
