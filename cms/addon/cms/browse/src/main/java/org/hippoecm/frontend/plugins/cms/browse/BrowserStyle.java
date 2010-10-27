package org.hippoecm.frontend.plugins.cms.browse;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.CSSPackageResource;

public final class BrowserStyle {

    private BrowserStyle() {
    }

    public static HeaderContributor getStyleSheet() {
        return CSSPackageResource.getHeaderContribution(BrowserStyle.class, "res/style.css");
    }

}
