package org.hippoecm.hst.content.beans.query.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PrimaryNodeTypeFilterImpl implements NodeTypeFilter{

    private List<String> primaryNodeTypeNamesList = new ArrayList<String>();
    
    public PrimaryNodeTypeFilterImpl(String primaryNodeTypeName){
        primaryNodeTypeNamesList.add(primaryNodeTypeName);
    }
    
    public PrimaryNodeTypeFilterImpl(String[] primaryNodeTypeNames){
        primaryNodeTypeNamesList.addAll(Arrays.asList(primaryNodeTypeNames));
    }
    
    public void addNodeTypeName(String primaryNodeTypeName) {
        primaryNodeTypeNamesList.add(primaryNodeTypeName);
    }

    public String getJcrExpression() {
        if(primaryNodeTypeNamesList.size() == 0) {
            return null;
        }
        StringBuilder where = new StringBuilder("(");
        boolean first = true;
        for(String name : primaryNodeTypeNamesList) {
            if(first) {
                where.append("@jcr:primaryType='").append(name).append("'");
            } else {
                where.append(" or @jcr:primaryType='").append(name).append("'");
            }
            first = false;;
        }
        where.append(")");
        return where.toString();
    }
}
