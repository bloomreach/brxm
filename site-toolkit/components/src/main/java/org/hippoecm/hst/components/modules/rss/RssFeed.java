/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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