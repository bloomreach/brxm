package org.hippoecm.hst.components.modules.rss.impl;

import org.hippoecm.hst.components.modules.rss.RssItem;

/**
 * Represents an item in an RSS feed.
 *
 */
public class SimpleRssItem implements RssItem {

  /** the title of the item */
  private String title;

  /** a link (url) to the complete item */
  private String link;

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.impl.RssItem#getTitle()
 */
  public String getTitle() {
    return title;
  }

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.impl.RssItem#setTitle(java.lang.String)
 */
  public void setTitle(String title) {
    this.title = title;
  }

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.impl.RssItem#getLink()
 */
  public String getLink() {
    return link;
  }

  /* (non-Javadoc)
 * @see org.hippoecm.hst.components.modules.rss.impl.RssItem#setLink(java.lang.String)
 */
  public void setLink(String link) {
    this.link = link;
  }

}
