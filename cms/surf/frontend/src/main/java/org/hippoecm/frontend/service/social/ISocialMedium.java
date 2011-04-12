package org.hippoecm.frontend.service.social;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ResourceReference;

/**
 * Provides access to a social medium.
 */
public interface ISocialMedium extends IClusterable, Comparable<ISocialMedium> {

    /**
     * @return the name of this social medium as displayed in a frontend (including the right capitalization etc.)
     */
    public String getDisplayName();

    /**
     * Shares a URL on this social medium.
     *
     * @param url the URL to share
     */
    public void shareUrl(String url);

    /**
     * Returns an icon for this social medium. The icon's size is 16x16 pixels.
     *
     * @return a reference to the icon
     */
    public ResourceReference getIcon16();

}
