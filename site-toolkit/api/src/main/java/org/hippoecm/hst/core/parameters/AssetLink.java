/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.core.parameters;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method returns the absolute path to the handle of an asset.
 * This annotation should only be used on public getter methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AssetLink {

    /**
     * The root path of the CMS configuration to use for the asset picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'. The default picker configuration is 'cms-pickers/assets'.
     *
     * @return the root path of the CMS configuration to use for the asset picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'.
     *
     */
    String pickerConfiguration() default "cms-pickers/assets";

    /**
     * The initial path to use in the CMS asset picker if nothing has been selected yet. Use the path to a folder
     * to initially open the asset picker dialog in that folder. Use the path to the handle of an asset to
     * preselect that asset.
     *
     * @return the initial path to use in the CMS asset picker, or an empty string if the default initial path of
     * the asset picker should be used.
     */
    String pickerInitialPath() default "";

    /**
     * Whether the asset picker remembers the last visited folder and image. The default is 'true'.
     *
     * @return whether the asset picker remembers the last visited folder and image.
     */
    boolean pickerRemembersLastVisited() default true;

    /**
     * Types of nodes to be able to select in the CMS asset picker. The default node type is
     * 'hippogallery:exampleAssetSet'.
     *
     * @return the node types to be able to select in the CMS asset picker.
     */
    String[] pickerSelectableNodeTypes() default { "hippogallery:exampleAssetSet" };

}
