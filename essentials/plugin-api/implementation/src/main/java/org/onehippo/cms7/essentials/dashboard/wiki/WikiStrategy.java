package org.onehippo.cms7.essentials.dashboard.wiki;

import java.util.Calendar;

import javax.jcr.Node;

/**
 * @version "$Id$"
 */
public interface WikiStrategy {

    public boolean onText(Node doc, Node currentSubFolder, String text);

    public boolean onTimeStamp(Node doc, Node currentSubFolder, Calendar timestamp);

    public String getType();

    public void onUserName(Node doc, Node currentSubFolder, String username);

    public void onTitle(Node doc, Node currentSubFolder, String docTitle);
}
