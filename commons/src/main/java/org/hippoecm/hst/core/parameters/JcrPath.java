/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.String;import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated method returns a JCR path that can be selected via a 'picker'. The path can be
 * absolute, or relative (see {@link #isRelative()}) to the picker content root path (see {@link #pickerRootPath()}).
 *
 * This annotation should only be used on public getter methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface JcrPath {

    /**
     * The root path of the CMS configuration to use for the picker, relative to
     * '/hippo:configuration/hippo:frontend/cms'. The default picker configuration is 'cms-pickers/documents'.
     *
     * @return the root path of the CMS configuration to use for the picker, relative to
     * '/hippo:configuration/hippo:frontend/cms'.
     *
     */
    String pickerConfiguration() default "cms-pickers/documents";

    /**
     * The initial path to use in the picker if nothing has been selected yet. Use the path to a folder
     * to initially open the picker in that folder. Use the path to the handle of a document to
     * preselect that document.
     *
     * @return the initial path to use in the picker, or an empty string if the default initial path of
     * the picker should be used.
     */
    String pickerInitialPath() default "";

    /**
     * Whether the picker remembers the last visited path. The default is 'true'.
     *
     * @return whether the picker remembers the last visited path.
     */
    boolean pickerRemembersLastVisited() default true;

    /**
     * Types of nodes to be able to select in the picker. The default list is empty, resulting in the default
     * behavior of the picker.
     *
     * @return the node types to be able to select in the picker.
     */
    String[] pickerSelectableNodeTypes() default {};

    /**
     * Whether this path is relative to the canonical content root path of the channel in which this annotation is
     * used. The default is 'false', i.e. the path is absolute.
     *
     * @return whether this path is relative to the canonical content root path of the channel in which this annotation
     * is used.
     */
    boolean isRelative() default false;

    /**
     * <p>
     * The root absolute path to use in the picker. This parameter allows the picker to select documents outside the channel
     * content path scope. However it is still restricted by authorization rules. The default value is empty, it means that
     * the picker will use the canonical content path of the channel as the root path.
     * </p>
     * @return the absolute root path to use in the picker, or an empty string if the channel content path is used. If configured
     * it <strong>must</strong> start with a "/".
     */
    String pickerRootPath() default "";

}
