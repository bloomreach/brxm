package org.hippoecm.frontend.plugins.standards.list;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;

public interface IJcrNodeViewerFactory extends IClusterable {

    Component getViewer(String id, JcrNodeModel node);
}
