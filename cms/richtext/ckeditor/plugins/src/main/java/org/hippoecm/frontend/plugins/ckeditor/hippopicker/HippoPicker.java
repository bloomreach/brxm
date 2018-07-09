/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.ckeditor.hippopicker;

/**
 * Constants for the CKEditor plugin 'hippopicker'.
 */
public class HippoPicker {

    public static final String CONFIG_KEY = "hippopicker";

    public static class Image {

        public static final String CONFIG_KEY = "image";
        public static final String CONFIG_CALLBACK_URL = "callbackUrl";
        public static final String COMMAND_INSERT_IMAGE = "insertImage";

    }

    public static class InternalLink {

        public static final String CONFIG_KEY = "internalLink";
        public static final String CONFIG_CALLBACK_URL = "callbackUrl";
        public static final String COMMAND_INSERT_INTERNAL_LINK = "insertInternalLink";

    }

}
