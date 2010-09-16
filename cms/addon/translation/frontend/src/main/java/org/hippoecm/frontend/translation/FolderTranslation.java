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

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.hippoecm.repository.translation.TranslationWorkflow;

public final class FolderTranslation implements IDetachable {
    private static final long serialVersionUID = 1L;

    private JcrNodeModel originalFolder;
    private String localeName;
    private String name;
    private String url;
    private boolean mutable = false;

    public FolderTranslation(Node node, String localeName) throws RepositoryException {
        this.originalFolder = new JcrNodeModel(node);
        this.localeName = localeName;

        this.url = node.getName();
        this.name = this.url;
        if (node instanceof HippoNode) {
            this.name = ((HippoNode) node).getLocalizedName();
        }
    }

    public void setUrl(String url) {
        if (!mutable) {
            throw new UnsupportedOperationException("Translation is immutable");
        }
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setName(String name) {
        if (!mutable) {
            throw new UnsupportedOperationException("Translation is immutable");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean persist() {
        if (!mutable) {
            throw new UnsupportedOperationException("Translation is immutable");
        }
        Node node = originalFolder.getNode();
        if (node != null) {
            try {
                WorkflowManager manager = ((HippoWorkspace) node.getSession().getWorkspace()).getWorkflowManager();
                TranslationWorkflow tw = (TranslationWorkflow) manager.getWorkflow("translation", node);
                Document translationDoc = tw.addTranslation(localeName, url);
                if (name != null && !url.equals(name)) {
                    DefaultWorkflow defaultWorkflow = (DefaultWorkflow) manager.getWorkflow("core", translationDoc);
                    defaultWorkflow.localizeName(name);
                }
                return true;
            } catch (RepositoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (WorkflowException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void detach() {
        originalFolder.detach();
    }
}