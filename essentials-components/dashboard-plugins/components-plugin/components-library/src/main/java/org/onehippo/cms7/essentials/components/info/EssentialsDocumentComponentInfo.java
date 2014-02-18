package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id: EssentialsDocumentComponentInfo.java 164011 2013-05-11 14:05:01Z mmilicevic $"
 */
public interface EssentialsDocumentComponentInfo {

    @Parameter(name = "document", required = false, displayName = "Document")
    @JcrPath(
            pickerConfiguration = "cms-pickers/documents",
            pickerSelectableNodeTypes = {"hippo:document"},
            pickerInitialPath = "/content/documents"
    )
    String getDocument();

}
