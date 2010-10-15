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
package org.hippoecm.frontend.translation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FolderTranslation implements IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(FolderTranslation.class);

    private int id;
    private final JcrNodeModel originalFolder;
    private final String localeName;
    private final String name;
    private final String url;
    private final String type;

    private String namefr;
    private String urlfr;
    private boolean mutable = false;

    public FolderTranslation(Node node, String localeName) throws RepositoryException {
        this.originalFolder = new JcrNodeModel(node);
        this.localeName = localeName;
        this.url = node.getName();
        if (node instanceof HippoNode) {
            this.name = ((HippoNode) node).getLocalizedName();
        } else {
            this.name = this.url;
        }
        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            this.type = "folder";
        } else {
            this.type = "doc";
        }

        this.urlfr = node.getName();
        this.namefr = this.urlfr;
        if (node instanceof HippoNode) {
            this.namefr = ((HippoNode) node).getLocalizedName();
        } else {
            this.namefr = this.urlfr;
        }
    }

    JcrNodeModel getOriginalFolder() {
        return originalFolder;
    }

    void setId(int id) {
        this.id = id;
    }
    
    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUrlfr() {
        return urlfr;
    }

    public void setUrlfr(String url) {
        if (!mutable) {
            throw new UnsupportedOperationException("Translation is immutable");
        }
        this.urlfr = url;
    }

    public String getNamefr() {
        return namefr;
    }

    public void setNamefr(String name) {
        if (!mutable) {
            throw new UnsupportedOperationException("Translation is immutable");
        }
        this.namefr = name;
    }

    public void setEditable(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isEditable() {
        return mutable;
    }

    public void detach() {
        getOriginalFolder().detach();
    }

}