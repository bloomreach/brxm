/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.ocm;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.ocm.NodeAware;
import org.hippoecm.hst.ocm.SimpleObjectConverter;
import org.hippoecm.hst.ocm.SimpleObjectConverterAware;

public class HippoStdNode implements NodeAware, SimpleObjectConverterAware {

    protected transient javax.jcr.Node node;
    protected transient SimpleObjectConverter simpleObjectConverter;
    protected String path;

    public javax.jcr.Node getNode() {
        return this.node;
    }
    
    public void setNode(javax.jcr.Node node) {
        this.node = node;
    }
    
    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }
    
    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }
    
    @Field(path=true)
    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        String name = "";
        
        if (this.node != null) {
            try {
                name = this.node.getName();
            } catch (Exception e) {
            }
        }

        return name;
    }

}
