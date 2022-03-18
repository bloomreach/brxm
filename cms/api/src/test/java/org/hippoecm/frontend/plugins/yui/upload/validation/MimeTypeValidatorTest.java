/*
 * Copyright 2021 Bloomreach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.yui.upload.validation;

import java.io.InputStream;

import org.hippoecm.frontend.editor.plugins.resource.InvalidMimeTypeException;
import org.junit.Test;

public class MimeTypeValidatorTest {

    @Test(expected = InvalidMimeTypeException.class)
    public void uploadExeAsPdf() {
        final String fileNameRelativeToResourcesFolder = "org/hippoecm/frontend/plugins/yui/upload/validation/helloWorld.pdf";
        MimeTypeValidator.validate(getFileFromResourceAsStream(fileNameRelativeToResourcesFolder), "application/pdf",
                null, "helloWorld.pdf");
    }

    @Test(expected = InvalidMimeTypeException.class)
    public void browserMimeTypeDoesNotMatchMimeType() {
        final String fileNameRelativeToResourcesFolder = "org/hippoecm/frontend/plugins/yui/upload/validation/dummy.pdf";
        MimeTypeValidator.validate(getFileFromResourceAsStream(fileNameRelativeToResourcesFolder), "application/ods",
                null, "helloWorld.pdf");
    }

    @Test
    public void browserMimeTypeDoesNotMatchMimeTypeButExplicitlyAllowed() {
        final String fileNameRelativeToResourcesFolder = "org/hippoecm/frontend/plugins/yui/upload/validation/dummy.pdf";
        MimeTypeValidator.validate(getFileFromResourceAsStream(fileNameRelativeToResourcesFolder), "application/ods",
                "application/pdf", "helloWorld.pdf");
    }

    @Test
    public void browserMimeTypeDoesNotMatchMimeTypeButExplicitlyConfiguredForCsv() {
        final String fileNameRelativeToResourcesFolder = "org/hippoecm/frontend/plugins/yui/upload/validation/sample.csv";
        MimeTypeValidator.validate(getFileFromResourceAsStream(fileNameRelativeToResourcesFolder), "foo/bar",
                "text/csv", "sample.csv");
    }

    // get a file from the resources folder
    // works everywhere, IDEA, unit test and JAR file.
    private InputStream getFileFromResourceAsStream(String fileName) {

        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return inputStream;
        }

    }
}
