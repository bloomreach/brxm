/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.apache.tika.detect.NameDetector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.pdf.PDFParser;

public class PdfParser {
    
    final static PdfParser instance = new PdfParser();
    
    final Tika tika;
    
    private PdfParser() {
        Map<Pattern, MediaType> patterns = new HashMap<Pattern, MediaType>();
        patterns.put(Pattern.compile(".*\\.pdf", Pattern.CASE_INSENSITIVE),
                MediaType.application("pdf"));
        NameDetector detector = new NameDetector(patterns);
        tika = new Tika(detector, new PDFParser());
    }
    
    private String doParse(final InputStream inputStream) {
        try {
            // tika parseToString already closes the inputStream
            return tika.parseToString(inputStream);
        } catch (TikaException e) {
            throw new IllegalStateException("Unexpected TikaException processing failure", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unexpected IOException processing failure", e);
        }
    }

    /**
     * @param inputStream the pdf <code>inputStream</code> that gets parsed, *and* that gets closed when finished
     * @return the first 100*1000 chars from the pdf as String. Also see {@link org.apache.tika.Tika#setMaxStringLength(int)}
     * @throws IllegalStateException in case of a {@link org.apache.tika.exception.TikaException} or {@link java.io.IOException}
     * @deprecated use parse instead, tika and its parsers are thread-safe
     */
    public synchronized static String synchronizedParse(final InputStream inputStream) {
        return instance.doParse(inputStream);
    }

    /**
     * @param inputStream the pdf <code>inputStream</code> that gets parsed, *and* that gets closed when finished
     * @return the first 100*1000 chars from the pdf as String. Also see {@link org.apache.tika.Tika#setMaxStringLength(int)}
     * @throws IllegalStateException in case of a {@link org.apache.tika.exception.TikaException} or {@link java.io.IOException}
     */
    public static String parse(final InputStream inputStream) {
        return instance.doParse(inputStream);
    }

}
