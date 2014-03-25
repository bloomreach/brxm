package org.onehippo.cms7.essentials.components.info;

import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * @version "$Id$"
 */
public interface EssentialsRepositoryMenuComponentInfo {

    @Parameter(name = "root", required = false, defaultValue = "", displayName = "Root Folder")
    @JcrPath(isRelative = true, pickerSelectableNodeTypes = {"hippostd:folder"})
    String getRootFolder();

    @Parameter(name = "folderLinks", required = false, defaultValue = "false", displayName = "Folders are also links")
    Boolean getFolderLinks();

    @Parameter(name = "depth", required = false, defaultValue = "2", displayName = "Depth")
    int getDepth();

}
