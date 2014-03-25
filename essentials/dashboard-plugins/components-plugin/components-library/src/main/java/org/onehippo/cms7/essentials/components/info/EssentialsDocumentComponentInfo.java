package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
public interface EssentialsDocumentComponentInfo {

    @Parameter(name = "document", required = false, displayName = "Document")
    @JcrPath(
            isRelative = true,
            pickerConfiguration = "cms-pickers/documents",
            pickerSelectableNodeTypes = {"hippo:document"},
            pickerInitialPath = "/content/documents"
    )
    String getDocument();

}
