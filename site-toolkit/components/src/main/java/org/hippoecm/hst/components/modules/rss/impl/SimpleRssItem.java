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
