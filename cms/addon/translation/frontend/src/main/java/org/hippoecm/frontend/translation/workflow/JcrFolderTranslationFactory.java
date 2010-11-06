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
package org.hippoecm.frontend.translation.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public final class JcrFolderTranslationFactory {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private JcrFolderTranslationFactory() {
    }

    public static FolderTranslation createFolderTranslation(Node original, Node node) throws RepositoryException {
        FolderTranslation ft = new FolderTranslation(node.getIdentifier());

        ft.setUrl(original.getName());
        if (original instanceof HippoNode) {
            ft.setName(((HippoNode) original).getLocalizedName());
        } else {
            ft.setName(ft.getUrl());
        }

        ft.setUrlfr(node.getName());
        if (node instanceof HippoNode) {
            ft.setNamefr(((HippoNode) node).getLocalizedName());
        } else {
            ft.setNamefr(ft.getUrlfr());
        }
        
        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            ft.setType("folder");
        } else {
            ft.setType("doc");
        }
        return ft;
    }

    public static FolderTranslation createFolderTranslation(Node node) throws RepositoryException {
        FolderTranslation ft = new FolderTranslation(node.getIdentifier());

        ft.setUrl(node.getName());
        if (node instanceof HippoNode) {
            ft.setName(((HippoNode) node).getLocalizedName());
        } else {
            ft.setName(ft.getUrl());
        }
        if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            ft.setType("folder");
        } else {
            ft.setType("doc");
        }
        return ft;
    }

}
