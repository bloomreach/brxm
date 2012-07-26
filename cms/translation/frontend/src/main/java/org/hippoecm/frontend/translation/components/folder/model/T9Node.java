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
package org.hippoecm.frontend.translation.components.folder.model;

import java.io.Serializable;

import org.wicketstuff.js.ext.util.ExtProperty;

/**
 * Node in the translated folder structure.
 */
public final class T9Node implements Serializable {

    private static final long serialVersionUID = 1L;

    private final T9Node parent;

    @ExtProperty
    private final String id;

    @ExtProperty
    private String name;

    @ExtProperty
    private String t9id;

    @ExtProperty
    private String lang;
    
    public T9Node(String id) {
        this.parent = null;
        this.id = id;
        this.setName("Root");
        this.setT9id(null);
        this.setLang(null);
    }

    public T9Node(T9Node parent, String id) {
        this.parent = parent;
        this.id = id;
        this.setName(null);
        this.setT9id(null);
        this.setLang(null);
    }

    public String getId() {
        return id;
    }

    public String getT9id() {
        return t9id;
    }

    public String getLang() {
        return lang;
    }

    public String getName() {
        return name;
    }

    public T9Node getParent() {
        return parent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setT9id(String t9id) {
        this.t9id = t9id;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof T9Node) {
            T9Node that = (T9Node) other;
            if (this.parent == null || that.parent == null) {
                return this.parent == that.parent;
            }
            return this.id.equals(that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 347;
        if (parent != null) {
            hash ^= parent.hashCode();
        }
        hash ^= id.hashCode();
        return hash;
    }

}
