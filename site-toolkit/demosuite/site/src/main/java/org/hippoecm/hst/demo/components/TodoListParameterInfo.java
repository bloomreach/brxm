package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.pagecomposer.annotations.Parameter;
import org.hippoecm.hst.pagecomposer.annotations.ParameterType;

public class TodoListParameterInfo {

    @Parameter(name = "listSize", required = true, type = ParameterType.INT, label = "Article Count")
    private int listSize;
}
