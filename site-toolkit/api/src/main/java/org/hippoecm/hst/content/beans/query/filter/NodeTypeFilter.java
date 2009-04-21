package org.hippoecm.hst.content.beans.query.filter;


public interface NodeTypeFilter extends BaseFilter{
    
    static final int AND = 0;
    static final int OR = 1;
    
    void addNodeTypeName(String nodeTypeName);
    
}
