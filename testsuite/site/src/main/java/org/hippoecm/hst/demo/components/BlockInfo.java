package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.Parameter;

public interface BlockInfo {
    @Parameter(name = "bgcolor", defaultValue="", displayName = "Background Color")
    @Color
    String getBgColor();

}
