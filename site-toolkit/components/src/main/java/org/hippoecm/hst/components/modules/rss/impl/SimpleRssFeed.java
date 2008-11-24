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
