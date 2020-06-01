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
package org.hippoecm.hst.jackrabbit.ocm.hippo;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType="hippo:document", discriminator=false)
public class HippoStdDocument extends HippoStdNode {

    private String stateSummary;
    private String state;


    @Field(jcrName="hippostd:stateSummary") 
    public String getStateSummary() {
        return this.stateSummary;
    }
    
    public void setStateSummary(String stateSummary) {
        this.stateSummary = stateSummary;
    }
    
    @Field(jcrName="hippostd:state") 
    public String getState() {
        return this.state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public HippoStdFolder getFolder() {
        return this.getParentFolder();
    }
    
}