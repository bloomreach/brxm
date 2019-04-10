package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

class OpenUiBehaviorHeaderItem extends HeaderItem {

    private static final String OPEN_UI_BEHAVIOR_JS = "OpenUiBehavior.js";
    private static final JavaScriptResourceReference OPEN_UI_BEHAVIOR
            = new JavaScriptResourceReference(OpenUiBehaviorHeaderItem.class, OPEN_UI_BEHAVIOR_JS);

    @Override
    public Iterable<?> getRenderTokens() {
        return Collections.singleton(OPEN_UI_BEHAVIOR_JS);
    }

    @Override
    public List<HeaderItem> getDependencies() {
        return Collections.singletonList(new PenpalHeaderItem());
    }

    @Override
    public void render(final Response response) {
        JavaScriptReferenceHeaderItem.forReference(OPEN_UI_BEHAVIOR).render(response);
    }
}
