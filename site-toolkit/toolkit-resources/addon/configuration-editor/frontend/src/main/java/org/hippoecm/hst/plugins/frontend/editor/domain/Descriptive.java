package org.hippoecm.hst.plugins.frontend.editor.domain;

import org.apache.wicket.Resource;

public interface Descriptive extends IEditorBean {

    String getDescription();

    void setDescription(String description);

    void setIconResource(Resource resource);

    Resource getIconResource();

}
