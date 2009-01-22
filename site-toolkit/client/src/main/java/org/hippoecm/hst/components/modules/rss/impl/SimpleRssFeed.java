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
package org.hippoecm.hst.components.modules.rss.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.hippoecm.hst.components.modules.rss.RssFeed;
import org.hippoecm.hst.components.modules.rss.RssItem;

/**
 * Represents a particular RSS feed.
 *
 */
public class SimpleRssFeed implements RssFeed {

  /** the items contained within the feed */
  private Collection<RssItem> items = new ArrayList<RssItem>();

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.RssFeedI#addItem(org.hippoecm.hst.components.modules.rss.RssItem)
 */
  public void addItem(RssItem item) {
    items.add(item);
  }

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.RssFeedI#getItems()
 */
  public Collection<RssItem> getItems() {
    return Collections.unmodifiableCollection(items);
  }

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.RssFeedI#getSize()
 */
  public int getSize() {
	  return this.items.size();
  }
}
