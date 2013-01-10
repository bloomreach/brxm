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
package org.onehippo.cms7.channelmanager.model;

import org.apache.wicket.model.IModel;

/**
 * Model that stores a JCR path in a delegate model. The path can be absolute or relative to a certain root.
 * When relative paths are used, relative paths returned by the delegate are converted to absolute paths,
 * and absolute paths are converted to relative paths before being set in the delegate.
 */
public class AbsoluteRelativePathModel implements IModel<String> {

    private final IModel<String> delegate;
    private final boolean isRelative;
    private final String rootPath;

    public AbsoluteRelativePathModel(final IModel<String> delegate, final String path, final boolean isRelative, final String rootPath) {
        this.delegate = delegate;
        this.isRelative = isRelative;
        this.rootPath = rootPath;
        setObject(path);
    }

    @Override
    public String getObject() {
        String pickedPath = delegate.getObject();

        if (isRelative) {
            // picked path is relative; prepend the root path
            StringBuilder absPath = new StringBuilder(rootPath);
            if (!pickedPath.isEmpty() && !pickedPath.startsWith("/")) {
                absPath.append("/");
            }
            absPath.append(pickedPath);
            return absPath.toString();
        } else {
            // picked path is absolute; return as-is
            return pickedPath;
        }
    }

    @Override
    public void setObject(String absPath) {
        String setPath = absPath;

        if (isRelative && absPath.startsWith(rootPath)) {
            // convert absolute path to relative path
            setPath = absPath.substring(rootPath.length());
            if (setPath.startsWith("/")) {
                setPath = setPath.substring(1);
            }
        }

        delegate.setObject(setPath);
    }

    @Override
    public void detach() {
        delegate.detach();
    }

}