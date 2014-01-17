/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.regex.Pattern;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

/**
 * @version "$Id$"
 */
@DocumentType("BaseFolder")
@Node(discriminator = false, jcrType = "essentials:folder")
public class BaseFolder extends BaseDocument implements Folder {

    private static final Pattern PATH_SPLITTER = Pattern.compile("/");

    public BaseFolder() {
    }

    public BaseFolder(String path) {
        setPath(path);
        final String[] splitter = PATH_SPLITTER.split(path);
        setName(splitter[splitter.length -1]);
    }

}
