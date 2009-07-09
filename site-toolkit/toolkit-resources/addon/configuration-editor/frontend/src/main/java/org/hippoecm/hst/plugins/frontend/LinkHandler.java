package org.hippoecm.hst.plugins.frontend;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;

public abstract class LinkHandler implements IClusterable {

    public void handle(AjaxRequestTarget target) {
        onHandle(target);
    }

    protected abstract void onHandle(AjaxRequestTarget target);

}
