package org.hippoecm.frontend.plugins.xinha.js.editormanager;

import java.util.List;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;

public class AutoSaveBehavior extends AbstractDefaultAjaxBehavior implements XinhaExtension {
    private static final long serialVersionUID = 1L;

    public void populateProperties(List properties) {
        properties.add(new ListEntry("callbackUrl", getCallbackUrl()));
    }

    @Override
    protected void respond(AjaxRequestTarget target) {
        ((FormComponent) getComponent()).processInput();
    }

}
