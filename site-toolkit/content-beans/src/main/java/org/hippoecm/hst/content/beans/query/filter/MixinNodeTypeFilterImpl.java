package org.hippoecm.hst.content.beans.query.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MixinNodeTypeFilterImpl implements NodeTypeFilter{

    private List<String> mixinNodeTypeNamesList = new ArrayList<String>();
    private int occur;
    
    public MixinNodeTypeFilterImpl(String mixinNodeType, int occur){
        mixinNodeTypeNamesList.add(mixinNodeType);
        this.occur = occur;
    }
    
    public MixinNodeTypeFilterImpl(String[] mixinNodeTypes, int occur){
        mixinNodeTypeNamesList.addAll(Arrays.asList(mixinNodeTypes));
        this.occur = occur;
    }

    public void addNodeTypeName(String nodeTypeName) {
        mixinNodeTypeNamesList.add(nodeTypeName);
    }

    public String getJcrExpression() {
        if(mixinNodeTypeNamesList.size() == 0) {
            return null;
        }
        StringBuilder where = new StringBuilder("(");
        boolean first = true;
        for(String name : mixinNodeTypeNamesList) {
            if(first) {
                where.append("@jcr:mixinTypes='").append(name).append("'");
            } else {
                if(occur == NodeTypeFilter.AND) {
                    where.append(" and ");
                } else {
                    where.append(" or ");
                }
                where.append("@jcr:mixinTypes='").append(name).append("'");
            }
            first = false;;
        }
        where.append(")");
        return where.toString();
    }
}
