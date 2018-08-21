/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Date;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.ImageSetPath;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

interface NewstyleInterface {
    @Parameter(name = "00-color", defaultValue = "blue")
    @Color
    String getColor();

    @Parameter(name = "02-image", defaultValue = "/content/gallery/default.png")
    @ImageSetPath
    String getImage();

    @Parameter(name = "03-date")
    Date getDate();

    @Parameter(name = "04-boolean")
    boolean isBoolean();

    @Parameter(name = "05-booleanClass")
    Boolean isBooleanClass();

    @Parameter(name = "06-int")
    int getInt();

    @Parameter(name = "07-integerClass")
    Integer getIntegerClass();

    @Parameter(name = "08-long")
    long getLong();

    @Parameter(name = "09-longClass")
    Long getLongClass();

    @Parameter(name = "10-short")
    short getShort();

    @Parameter(name = "11-shortClass")
    Short getShortClass();

    @Parameter(name = "12-jcrpath")
    @JcrPath(pickerInitialPath = "/content/documents/subdir/foo")
    String getJcrPath();

    @Parameter(name = "13-relativejcrpath")
    @JcrPath(isRelative = true, pickerInitialPath = "subdir/foo", pickerConfiguration = "cms-pickers/mycustompicker")
    String getRelativeJcrPath();

    @Parameter(name = "14-dropdownvalue")
    @DropDownList(value = {"value1", "value2", "value3"})
    String getDropDownValue();

    @Parameter(name = "15-hideInChannelManager", hideInChannelManager = true)
    String getHiddenInChannelManager();
}
