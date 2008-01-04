/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.migration.xml;

/**
 * Class to encapsulate an instraction for the xmlextractor class
 */
public class ExtractorInstruction {

    /** The name of the extracted property */
    private final String name;

    /** The xpath for extracting the property */
    private final String XPath;

    /**
     * Create a new instruction for the XmlExtractor
     * @param name the name of the property
     * @param XPath
     */
    public ExtractorInstruction(String name, String XPath) {
        this.name = name;
        this.XPath = XPath;
    }

    /**
     * Get the name of the property to set
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the xpath for the property to extract
     * @return
     */
    public String getXPath() {
        return XPath;
    }
}
