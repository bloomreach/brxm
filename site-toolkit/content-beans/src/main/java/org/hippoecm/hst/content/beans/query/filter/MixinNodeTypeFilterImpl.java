/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
