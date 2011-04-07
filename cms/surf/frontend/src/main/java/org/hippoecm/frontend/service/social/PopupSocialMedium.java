package org.hippoecm.frontend.service.social;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ResourceReference;
import org.hippoecm.frontend.service.popup.IPopupService;

/**
 * Provides access to a social medium. URLs are shared by opening a medium-specific URL in a popup window.
 */
public class PopupSocialMedium implements ISocialMedium {

    private final String displayName;
    private final String shareUrlPrefix;
    private final IPopupService popupService;

    public PopupSocialMedium(String displayName, String shareUrlPrefix, IPopupService popupService) {
        this.displayName = displayName;
        this.shareUrlPrefix = shareUrlPrefix;
        this.popupService = popupService;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void shareUrl(final String url) {
        popupService.openPopupWindow(shareUrlPrefix + url);
    }

    @Override
    public ResourceReference getIcon16() {
        final String iconName = StringUtils.lowerCase(displayName) + "-icon-16.png";
        return new ResourceReference(getClass(), iconName);
    }

    @Override
    public int compareTo(final ISocialMedium medium) {
        return displayName.compareTo(medium.getDisplayName());
    }
}
