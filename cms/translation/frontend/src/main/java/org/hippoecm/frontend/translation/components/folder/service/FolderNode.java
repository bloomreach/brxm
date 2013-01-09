/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.translation.components.folder.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.frontend.translation.components.folder.model.T9Node;
import org.wicketstuff.js.ext.tree.ExtTreeNode;
import org.wicketstuff.js.ext.util.ExtProperty;

public final class FolderNode extends ExtTreeNode {

    private static final long serialVersionUID = 1L;

    @ExtProperty
    private final String t9id;

    @ExtProperty
    private final String lang;

    @ExtProperty
    private final String cls = "folder";

    @ExtProperty
    private String iconCls = null;

    @SuppressWarnings("unused")
    @ExtProperty
    private boolean disabled;

    @ExtProperty
    private List<String> t9ns = Collections.emptyList();

    private final String targetLanguage;

    public FolderNode(T9Node ftn, String language) {
        this.t9id = ftn.getT9id();
        this.lang = ftn.getLang();
        this.targetLanguage = language;

        setId(ftn.getId());
        setText(ftn.getName());

        disabled = isDisabled();
    }

    public String getT9id() {
        return t9id;
    }

    public String getLang() {
        return lang;
    }

    public boolean isDisabled() {
        return (lang == null || lang.equals(targetLanguage) || getT9ns().indexOf(targetLanguage) > -1);
    }

    public String getCls() {
        return cls;
    }

    public String getIconCls() {
        return iconCls;
    }

    public List<String> getT9ns() {
        return Collections.unmodifiableList(t9ns);
    }

    public void setT9ns(List<String> t9ns) {
        this.t9ns = new ArrayList<String>(t9ns);
        disabled = isDisabled();
    }

    public void setIconCls(String iconCls) {
        this.iconCls = iconCls;
    }

}
