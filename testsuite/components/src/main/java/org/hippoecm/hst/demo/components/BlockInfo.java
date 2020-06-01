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
package org.hippoecm.hst.demo.components;

import org.hippoecm.hst.core.parameters.Color;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;

public interface BlockInfo {
    @Parameter(name = "bgcolor", defaultValue="", displayName = "Background Color")
    @Color
    String getBgColor();

    @Parameter(name = "content", defaultValue="My block", displayName = "Contents")
    String getContent();

    @Parameter(name = "blockPath", displayName = "Block Path")
    @JcrPath(pickerInitialPath = "/content/documents/demosite/news")
    String getBlockPath();


    @Parameter(name = "crossChannelBlockPath", displayName = "Cross channel Block Path")
    @JcrPath(pickerInitialPath = "/content/documents", pickerRootPath = "/content/documents/demosite_fr")
    String getBlockPathOutsideChannel();
}
