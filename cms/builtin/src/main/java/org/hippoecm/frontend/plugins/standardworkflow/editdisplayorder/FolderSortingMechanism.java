/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.plugins.standardworkflow.editdisplayorder;

import java.util.Set;

import org.hippoecm.frontend.FrontendNodeType;
import org.hippoecm.repository.api.Folder;

/**
 * Adapter for folder, so that folder can act as POJO
 * to be used in a {@link org.apache.wicket.model.PropertyModel}.
 * See https://cwiki.apache.org/confluence/display/WICKET/Using+RadioGroups.
 *
 * The {@link #alphabetically} property is needed so that it can be used in a
 * {@link org.apache.wicket.model.PropertyModel}.
 */
public class FolderSortingMechanism implements Folder {

    /**
     * Folder instance that is wrapped by this adapter.
     */
    private final Folder folder;
    /**
     * Needed for wicket propertyModel, in sync with {@link FrontendNodeType#FRONTEND_ORDER_CHILD_FOLDERS).
     */
    private Boolean alphabetically;

    /**
     * Constructs a object that is a {@link Folder} and acts an a POJO with a "alphabetically" property.
     * @param folder {@link Folder} instance to be wrapped
     */
    public FolderSortingMechanism(final Folder folder) {
        this.folder = folder;
        this.alphabetically = getMixins().contains(FrontendNodeType.FRONTEND_ORDER_CHILD_FOLDERS);
    }

    /**
     *
     * @return true if the subfolders are displayed in an alphabetical order
     */
    public Boolean getAlphabetically() {
        return alphabetically;
    }

    /**
     * Sets if the folder should be display alphabetically.
     * @param alphabetically true if the subfolders are displayed alphabetically
     */
    public void setAlphabetically(final Boolean alphabetically) {
        this.alphabetically = alphabetically;
        if (getAlphabetically()) {
            addMixin(FrontendNodeType.FRONTEND_ORDER_CHILD_FOLDERS);
        } else {
            removeMixin(FrontendNodeType.FRONTEND_ORDER_CHILD_FOLDERS);
        }
    }

    /**
     * Returns the identifier of the folder.
     *
     * @return identifier
     */
    @Override
    public String getIdentifier() {
        return folder.getIdentifier();
    }

    /**
     * Add a mixin by name and return {@code true} if it was not present yet and {@code false} otherwise.
     *
     * @param mixin the name of the mixin
     * @return if the mixin being added was not present yet
     */
    @Override
    public boolean addMixin(final String mixin) {
        return folder.addMixin(mixin);
    }

    /**
     * Remove a mixin by name and return {@code true} if it was present and {@code false} otherwise.
     *
     * @param mixin the name of the mixin
     * @return if the mixin being removed was present
     */
    @Override
    public boolean removeMixin(final String mixin) {
        return folder.removeMixin(mixin);
    }

    /**
     * @return {@link Set} containing mixin names
     */
    @Override
    public Set<String> getMixins() {
        return folder.getMixins();
    }
}
