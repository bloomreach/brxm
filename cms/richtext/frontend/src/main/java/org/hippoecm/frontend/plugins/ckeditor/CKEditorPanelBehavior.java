package org.hippoecm.frontend.plugins.ckeditor;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.model.IDetachable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Defines additional behavior for a {@link CKEditorPanel}. A CKEditor behavior can add configuration properties
 * for a CKEditor instance in {@link #addCKEditorConfiguration(org.json.JSONObject)}, and create one or more Ajax
 * behaviors to render additional Wicket components in an Ajax response. The behavior will be detached when the
 * {@link CKEditorPanel} to which it is added is detached.
 */
public interface CKEditorPanelBehavior extends IDetachable {

    /**
     * Adds configuration to a CKEditor instance.
     * @param editorConfig the configuration for a CKEditor instance
     * @throws JSONException
     */
    void addCKEditorConfiguration(JSONObject editorConfig) throws JSONException;

    /**
     * @return all Ajax behaviors needed by this CKEditor behavior.
     */
    Iterable<AbstractAjaxBehavior> getAjaxBehaviors();

}
