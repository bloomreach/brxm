/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
 * Indicates that the annotated method returns the absolute path to the handle of an image set.
 * This annotation should only be used on public getter methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ImageSetPath {

    /**
     * The variant in the image set to use as the preview image in the CMS (i.e. the CND child node name).
     * By default an empty string is returned, which means "use the primary item". If the given variant cannot be
     * found, the primary item is used instead.
     *
     * @return the CND name of the image set variant to use as the preview image in the CMS.
     */
    String previewVariant() default "";

    /**
     * The root path of the CMS configuration to use for the image picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'. The default picker configuration is 'cms-pickers/images'.
     *
     * @return the root path of the CMS configuration to use for the image picker dialog, relative to
     * '/hippo:configuration/hippo:frontend/cms'.
     *
     */
    String pickerConfiguration() default "cms-pickers/images";

    /**
     * The initial path to use in the CMS image picker if nothing has been selected yet. Use the path to a folder
     * to initially open the image picker dialog in that folder. Use the path to the handle of an image set to
     * preselect that image set.
     *
     * @return the initial path to use in the CMS image picker, or an empty string if the default initial path of
     * the image picker should be used.
     */
    String pickerInitialPath() default "";

    /**
     * Whether the image picker remembers the last visited folder and image. The default is 'true'.
     *
     * @return whether the image picker remembers the last visited folder and image.
     */
    boolean pickerRemembersLastVisited() default true;

    /**
     * Types of nodes to be able to select in the CMS image picker. The default node type is 'hippogallery:imageset'.
     *
     * @return the node types to be able to select in the CMS image picker.
     */
    String[] pickerSelectableNodeTypes() default { "hippogallery:imageset" };

}
