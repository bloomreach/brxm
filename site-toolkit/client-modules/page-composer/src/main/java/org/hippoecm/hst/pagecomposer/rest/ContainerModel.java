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

import javax.jcr.Node;
import javax.servlet.http.HttpSession;

public class ContainerModel extends BaseModel{
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ContainerModel.java 96565 2010-10-05 02:14:11Z abogaart $";

    private String[] children;

    public ContainerModel() {
    }

    public ContainerModel(Node node) {
        super(node);
    }

    @Override
    protected String getTypeValue() {
        return "hst:containercomponent";
    }

    public String[] getChildren() {
        return children;
    }

    public void setChildren(String[] children) {
        this.children = children;
    }
}
