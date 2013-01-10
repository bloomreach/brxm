/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.yui.flash;

import org.apache.wicket.IClusterable;

public class FlashVersion implements IClusterable {
    int major = 0;
    int minor = 0;
    int revision = 0;

    public FlashVersion() {
    }

    public FlashVersion(int major, int minor, int revision) {
        this.major = major;
        this.minor = minor;
        this.revision = revision;
    }

    public int getMajorVersion() {
        return major;
    }

    public int getMinorVersion() {
        return minor;
    }

    public int getRevisionVersion() {
        return revision;
    }

    public void setMajorVersion(int major) {
        this.major = major;
    }

    public void setMinorVersion(int minor) {
        this.minor = minor;
    }

    public void setRevisionVersion(int revision) {
        this.revision = revision;
    }

    public boolean isAvailable() {
        return major > 0;
    }

    public boolean isValid(int major, int minor, int revision) {
        if (this.major < major) {
            return false;
        }
        if (this.major > major) {
            return true;
        }
        if (this.minor < minor) {
            return false;
        }
        if (this.minor > minor) {
            return true;
        }
        if (this.revision < revision) {
            return false;
        }
        return true;
    }

    public boolean isValid(FlashVersion validFlash) {
        return isValid(validFlash.major, validFlash.minor, validFlash.revision);
    }
}
