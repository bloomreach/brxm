/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.rest.custom;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DocumentLink;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;

public enum AnnotationType {
    Color(Color.class),
    DocumentLink(DocumentLink.class),
    DropDownList(DropDownList.class),
    FieldGroup(FieldGroup.class),
    FieldGroupList(FieldGroupList.class),
    ImageSetPath(ImageSetPath.class),
    JcrPath(JcrPath.class),
    Unknown(AnnotationType.class);

    private final Class<?> clazz;

    AnnotationType(final Class<?> clazz) {
        this.clazz = clazz;
    }

    public static AnnotationType fromClass(final Class<?> annotationClass) {
        for (AnnotationType type : AnnotationType.values()) {
            if (type.clazz == annotationClass) {
                return type;
            }
        }
        return Unknown;
    }

}
