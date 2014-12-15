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

package org.hippoecm.frontend.plugins.jquery.upload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception containing violation messages in uploading files
 */
public class FileUploadViolationException extends Exception{

    private final List<String> violationMessages = new ArrayList<>();

    public FileUploadViolationException(final List<String> messages){
        setViolations(messages);
    }

    public FileUploadViolationException(final String message) {
        violationMessages.add(message);
    }

    public List<String> getViolationMessages() {
        return Collections.unmodifiableList(violationMessages);
    }

    public void setViolations(final List<String> messages) {
        violationMessages.clear();
        violationMessages.addAll(messages);
    }
}
