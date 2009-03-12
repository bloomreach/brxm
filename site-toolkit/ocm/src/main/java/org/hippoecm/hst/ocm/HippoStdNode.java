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

import java.util.Collections;
import java.util.Map;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;

public class HippoStdNode implements NodeAware, SimpleObjectConverterAware {

    protected String path;

    private transient SimpleObjectConverter simpleObjectConverter;
    private JCRValueProvider valueProvider;

    public javax.jcr.Node getNode() {
        javax.jcr.Node node = null;
        
        if (this.valueProvider != null) {
            node = this.valueProvider.getJcrNode();
        }
        
        return node;
    }

    public void setNode(javax.jcr.Node node) {
        if (this.valueProvider == null) {
            this.valueProvider = new JCRValueProviderImpl(node);
        }
    }

    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }

    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }

    @Field(path = true)
    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        String name = "";

        if (this.valueProvider != null) {
            name = this.valueProvider.getName();
        }

        return name;
    }

    public Map<String, Object> getProperty() {
        Map<String, Object> properties = null;

        if (this.valueProvider == null) {
            properties = Collections.emptyMap();
        } else {
            properties = this.valueProvider.getProperties();
        }
        
        return properties;
    }
    
}
