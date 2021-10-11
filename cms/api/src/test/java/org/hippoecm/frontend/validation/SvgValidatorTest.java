/*
 *  Copyright 2021 Bloomreach Inc. (http://www.bloomreach.com)
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
package org.hippoecm.frontend.validation;

import java.io.InputStream;

import org.apache.tika.io.IOUtils;
import org.hippoecm.frontend.plugins.gallery.model.SvgOnLoadGalleryException;
import org.hippoecm.frontend.plugins.gallery.model.SvgScriptGalleryException;
import org.junit.Test;


public class SvgValidatorTest {

    @Test(expected=SvgScriptGalleryException.class)
    public void validate_onload() throws Exception {
        try {
            SvgValidator.validate(IOUtils.toString(getFileFromResourceAsStream("org/hippoecm/frontend/validation/script.svg")));
        } catch (SvgValidationException e){
            throw e.getGalleryException();
        }
    }

    @Test(expected = SvgOnLoadGalleryException.class)
    public void validate_script() throws Exception {
        try {
            SvgValidator.validate(IOUtils.toString(getFileFromResourceAsStream("org/hippoecm/frontend/validation/onload.svg")));
        } catch (SvgValidationException e){
            throw e.getGalleryException();
        }
    }


    private InputStream getFileFromResourceAsStream(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found! " + fileName);
        } else {
            return inputStream;
        }
    }
}
