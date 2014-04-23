/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils.beansmodel;

import java.nio.file.Path;

/**
 * @version "$Id: HippoEssentialsGeneratedObject.java 172822 2013-08-05 14:50:33Z mmilicevic $"
 */
public class HippoEssentialsGeneratedObject {


    private String internalName;
    private String dateGenerated;
    private boolean allowModifications;
    private Path filePath;

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(final String internalName) {
        this.internalName = internalName;
    }

    public String getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(final String dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public boolean isAllowModifications() {
        return allowModifications;
    }

    public void setAllowModifications(final boolean allowModifications) {
        this.allowModifications = allowModifications;
    }

    public Path getFilePath() {
        return filePath;
    }

    public void setFilePath(final Path filePath) {
        this.filePath = filePath;
    }
}
