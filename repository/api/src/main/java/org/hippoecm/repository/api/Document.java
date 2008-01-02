/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.api;

import java.io.Serializable;

public abstract class Document implements Serializable, Cloneable {
    private transient Document jcrIsCloned = null;
    public Object clone() throws CloneNotSupportedException {
        Document document = (Document) super.clone();
        document.setJcrCloned(this);
        return document;
    }
    private void setJcrCloned(Document document) {
        jcrIsCloned = document;
    }

    public String getJcrIdentity() {
        return null;
    }

    public Document getJcrCloned() {
        return jcrIsCloned;
    }
}
