package org.hippoecm.hst.components.modules.rss;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RssFeed {

    public Logger log = LoggerFactory.getLogger(RssFeed.class);	
	
	/**
	 * Adds a item to this feed.
	 *
	 * @param item    an RssItem instance
	 */
	public abstract void addItem(RssItem item);

	/**
	 * Get the number of rss items within this RSS feed
	 * @return number of rss items
	 */
	public abstract int getSize();
	
	/**
	 * Gets a collection of all items within this RSS feed.
	 *
	 * @return  a Collection of RssItem instances
	 */
	public abstract Collection<RssItem> getItems();



}