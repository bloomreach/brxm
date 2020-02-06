/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagemodelapi.common.beans;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType="unittestproject:textpage")
public class TextBean extends HippoDocument {

    public HippoBean getNewsDocument() {
        // hard-coded uuid of news bean to make sure serialized of linked bean also works
        HippoBean beanByUUID = getBeanByUUID("303d40eb-f98c-4d61-84c7-a1ba14b5ceb3", HippoBean.class);
        return beanByUUID;
    }

    public HippoBean getFolderBean() {
        // assertion wrt how FolderBean is serialized
        return this.getParentBean();
    }

    /**
     * <p>
     *    Proof that self referencing does not break serialization but just gets replaced with a $ref
     * </p>
     */
    public HippoBean getSelfBean() {
        return getBeanByUUID(getValueProvider().getIdentifier(), TextBean.class);
    }

    /**
     * <p>
     *    Proof that self instance referencing does not break serialization but just gets replaced with a $ref
     * </p>
     */
    public HippoBean getSelfInstance() {
        return this;
    }


    /**
     * Proof: Below the ChildObject should get a 'parent' serialized as $ref and no recursion!
     */
    public ChildObject getChildObject() {
        return new ChildObject(this);
    }

    public List<HippoBean> getListContainingSelf() {
        ArrayList<HippoBean> list = new ArrayList<>();
        list.add(getSelfBean());
        list.add(getSelfInstance());
        return list;
    }

    private static class ChildObject {
        private TextBean textBean;

        public ChildObject(final TextBean textBean) {
            this.textBean = textBean;
        }

        public String getName() {
            return "just-some-name";
        }

        /**
         * Proof: Below should be serialized as $ref
         */
        public TextBean getParent() {
            return textBean;
        }
    }
}
