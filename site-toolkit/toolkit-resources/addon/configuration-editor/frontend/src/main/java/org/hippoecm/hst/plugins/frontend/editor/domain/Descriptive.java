package org.hippoecm.hst.plugins.frontend.editor.domain;

import java.io.Serializable;

import org.apache.wicket.Resource;

public interface Descriptive extends Serializable {

    String getDescription();

    void setDescription(String description);

    void setIconResource(Resource resource);

    Resource getIconResource();

}
