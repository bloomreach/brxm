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
package org.hippoecm.frontend.plugins.yui.upload.validation;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;
import org.hippoecm.frontend.editor.plugins.resource.InvalidMimeTypeException;
import org.onehippo.repository.tika.TikaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MimeTypeValidator {

    private static final Tika tika = TikaFactory.newTika();
    public static final Logger log = LoggerFactory.getLogger(MimeTypeValidator.class);
    private final InputStream inputStream;
    private final String browserMimeType;
    private final String allowedMimeType;
    private final String fileName;

    private MimeTypeValidator(final InputStream inputStream, final String browserMimeType, final String allowedMimeType, final String fileName){
       this.inputStream = inputStream;
       this.browserMimeType = browserMimeType;
       this.fileName = fileName;
       this.allowedMimeType = allowedMimeType;
    }


    /**
     * @return The best matching mimetype for this fileItem
     */
    private String resolveMimeType() {
        try {
            final String extensionBasedMediaType = tika.detect(fileName);
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(extensionBasedMediaType, MediaType.TEXT_PLAIN)) {
                return extensionBasedMediaType;
            }

            String resolvedMediaType = tika.detect(inputStream, fileName);
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(resolvedMediaType, MediaType.APPLICATION_ZIP)) {
                return extensionBasedMediaType;
            }

            return resolvedMediaType;
        } catch (IOException e) {
            log.warn("Tika failed to detect mime-type, falling back on browser provided mime-type", e);
        }
        return null;
    }

    void validate(){
        if (fileName == null){
            throw new InvalidMimeTypeException("File name has not been supplied");
        }
        final String tikaDetectedContentType = resolveMimeType();
        if (tikaDetectedContentType == null){
            throw new InvalidMimeTypeException("Could not detect mimetype from content", "unknown");
        }
        if (tikaDetectedContentType.equalsIgnoreCase(allowedMimeType)) {
            log.debug("Explicit matched mapping found for browser provided mimetype '{}' to Tika detected mimetype '{}'",
                    fileName, tikaDetectedContentType);
            return;
        }

        // by default, executable files are not allowed
        if (tikaDetectedContentType.equalsIgnoreCase("application/x-dosexec")) {
            log.debug("Detected executable. Only if the extension is .exe and .exe is explicitly allowed as content type," +
                    "the upload is allowed. Otherwise, an InvalidMimeTypeException will be thrown");
            throw new InvalidMimeTypeException("Executable file upload not allowed", tikaDetectedContentType);
        }
        if (!tikaDetectedContentType.equalsIgnoreCase(browserMimeType)) {
            // it might be that Tika returns a more specific child contentType or super contentType than the one provided
            // by the browser. We now have to validate this and if this is the case, the mimetype check still passes
            if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(browserMimeType, MediaType.parse(tikaDetectedContentType))) {
                log.debug("Browser provided content type '{}' is a subtype of '{}'", browserMimeType, tikaDetectedContentType);
            } else if (MediaTypeRegistry.getDefaultRegistry().isInstanceOf(tikaDetectedContentType, MediaType.parse(browserMimeType))) {
                log.debug("Browser provided content type '{}' is a super type of '{}'", browserMimeType, tikaDetectedContentType);
            } else {
                log.debug("Detected mimetype by Tika '{}' does not match the provided mimetype by the browser '{}'", tikaDetectedContentType, browserMimeType);
                throw new InvalidMimeTypeException(String.format("Could not detect mimetype or detected mimetype different than " +
                        "request mimetype '%s'. Tika detected mimetype was '%s'", browserMimeType, tikaDetectedContentType),
                        tikaDetectedContentType);
            }
        }


    }

    public static void validate(InputStream inputStream, final String browserMimeType, final String allowedMimeType, final String fileName){
        new MimeTypeValidator(inputStream, browserMimeType, allowedMimeType, fileName).validate();
    }


}
