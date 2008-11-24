package org.hippoecm.hst.components.modules.rss;

public interface RssItem {

	/**
	 * Gets the title of this item.
	 *
	 * @return  the title as a String
	 */
	public abstract String getTitle();

	/**
	 * Sets the title of this item.
	 *
	 * @param title   the title as a String
	 */
	public abstract void setTitle(String title);

	/**
	 * Gets the url (link) to this item.
	 *
	 * @return  the url as a String
	 */
	public abstract String getLink();

	/**
	 * Sets the url (link) to this item.
	 *
	 * @param link    the link as a String
	 */
	public abstract void setLink(String link);

}