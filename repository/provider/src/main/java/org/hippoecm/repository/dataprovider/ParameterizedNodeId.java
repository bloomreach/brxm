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
package org.hippoecm.repository.dataprovider;

import org.apache.jackrabbit.core.id.NodeId;

public class ParameterizedNodeId extends NodeId
{

    private static final long serialVersionUID = 1L;

    private NodeId original;
    private String parameter;

    public ParameterizedNodeId(NodeId original, String parameter) {
        super(original.getRawBytes());
        this.original = original;
        this.parameter = parameter;
    }

    public String getParameterString() {
        return parameter;
    }

    public NodeId getUnparameterizedNodeId() {
        return original;
    }

    @Override
    public String toString() {
        return "ParameterizedNodeId[uuid="+original+",hash="+hashCode()+",parameter=\""+parameter+"\"]";
    }
}
