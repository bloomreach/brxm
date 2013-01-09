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
