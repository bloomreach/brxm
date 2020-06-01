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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.content.annotations.PageModelAnyGetter;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoAssetBean;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;
import org.hippoecm.hst.content.PageModelEntity;

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

    public HippoBean getImageSetBean() {
        // assertion wrt how image set is serialized
        return getBeanByUUID("db02dde5-0098-4488-a72c-2a4fc6d51beb", HippoGalleryImageSetBean.class);
    }

    public HippoBean getAssetBean() {
        // assertion wrt how asset is serialized
        return getBeanByUUID("5e56d406-b302-445e-9f80-e624ba053bc4", HippoAssetBean.class);
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

    /**
     * <p>
     *     Because of PageModelAnyGetter we expect the Map<String, Object> to be serialized flattened. Since
     *     Bar implements PageModelEntity we also expect bar to be serialized with a $ref
     * </p>
     */
    @PageModelAnyGetter
    public Map<String, Object> getObjectMap() {
        Map<String, Object> overlays = new HashMap<>();

        overlays.put("foo", new Foo("foo", "Foo Text"));
        overlays.put("bar", new Bar("bar", "Bar Text"));

        return overlays;
    }

    public static class Foo {

        final String name;
        final String text;

        public Foo(final String name, final String text) {

            this.name = name;
            this.text = text;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * bar implements PageModelEntity meaning it should get serialized as $ref (opposed to objects Foo)
     */
    public static class Bar implements PageModelEntity {

        final String name;
        final String text;

        public Bar(final String name, final String text) {

            this.name = name;
            this.text = text;
        }

        public String getName() {
            return name;
        }

        public String getText() {
            return text;
        }
    }

}
