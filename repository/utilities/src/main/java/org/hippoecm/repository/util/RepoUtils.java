/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.repository.util;

/**
 * Utility methods used by the repository
 */
public class RepoUtils {

    /**
     * If the path starts with a file: protocol prefix convert it to an absolute filesystem path
     *
     * @param path  the path to stripped
     * @return  the path stripped of the file: protocol prefix
     */
    public static String stripFileProtocol(String path) {
        if (path.startsWith("file://")) {
            return path.substring(6);
        } else if (path.startsWith("file:/")) {
            return path.substring(5);
        } else if (path.startsWith("file:")) {
            return "/" + path.substring(5);
        }
        return path;
    }

}
