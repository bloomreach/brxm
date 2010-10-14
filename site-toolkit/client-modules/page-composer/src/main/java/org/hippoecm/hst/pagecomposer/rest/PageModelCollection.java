/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.pagecomposer.rest;

import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
public class PageModelCollection {
    final static String SVN_ID = "$Id$";

    private Collection<BaseModel> models;

    public PageModelCollection(Collection<BaseModel> models) {
        this.models = models;
    }

    public Collection<BaseModel> getModels() {
        return models;
    }

    public void setModels(Collection<BaseModel> models) {
        this.models = models;
    }

}
