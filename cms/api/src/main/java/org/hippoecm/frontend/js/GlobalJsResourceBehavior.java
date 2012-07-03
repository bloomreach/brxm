package org.hippoecm.frontend.js;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.html.JavascriptPackageResource;

public class GlobalJsResourceBehavior extends AbstractBehavior {

    public static final String GLOBAL = "global.js";

    public void bind(Component component) {

        component.add(JavascriptPackageResource.getHeaderContribution(GlobalJsResourceBehavior.class, GLOBAL));

    }

}
