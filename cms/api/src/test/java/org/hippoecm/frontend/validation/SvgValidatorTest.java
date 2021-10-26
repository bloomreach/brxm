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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;


public class SvgValidatorTest {

    @Test
    public void validate_onload() throws
            Exception {
        final SvgValidationResult validationResult = SvgValidator.validate(
                getFileFromResourceAsStream("org/hippoecm/frontend/validation/script.svg"));
        Assert.assertTrue(validationResult.getOffendingElements().contains("script"));
    }

    @Test
    public void validate_script() throws
            Exception {
        final SvgValidationResult validate = SvgValidator.validate(
                getFileFromResourceAsStream("org/hippoecm/frontend/validation/onload.svg"));
        Assert.assertTrue(validate.getOffendingAttributes().contains("onload"));
    }

    @Test(expected = SAXException.class)
    public void validateScriptInStyleUrl() throws
            Exception {
        final SvgValidationResult validate = SvgValidator.validate(
                getFileFromResourceAsStream("org/hippoecm/frontend/validation/scriptInStyleUrl.svg"));
    }

    @Test()
    public void validateSampleSvg() throws
            Exception {
        final SvgValidationResult validate = SvgValidator.validate(
                getFileFromResourceAsStream("org/hippoecm/frontend/validation/sample_640×426.svg"));
        Assert.assertTrue(validate.getOffendingAttributes().isEmpty());
        Assert.assertTrue(validate.getOffendingElements().isEmpty());
    }

    @Test()
    public void validateWorldMapSvg() throws
            Exception {
        final SvgValidationResult validate = SvgValidator.validate(
                getFileFromResourceAsStream("org/hippoecm/frontend/validation/Worldmap.svg"));
        Assert.assertTrue(validate.getOffendingAttributes().isEmpty());
        Assert.assertTrue(validate.getOffendingElements().isEmpty());
    }

    /**
     * Test svg's in bulk, e.g. against the sample set of apache batik ( 354 svg files )
     * See https://xmlgraphics.apache.org/batik/, https://github.com/apache/xmlgraphics-batik/tree/trunk/samples
     */
    @Ignore
    @Test()
    public void testAllSvgSampleFiles() throws
            Exception {
        // path to samples on local machine
        final String pathToSampleSet = "/home/mrop/github.com/apache/xmlgraphics-batik/samples";
        Stream<Path> walk = Files.walk(Paths.get(pathToSampleSet));

        Set<String> offendingAttributes = new HashSet<>();
        Set<String> offendingElements = new HashSet<>();
        Set<String> offendingFiles = new HashSet<>();
        final Set<Path> collect = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".svg"))
                .collect(Collectors.toSet());
        int count = 0;
        for (Path path : collect) {
            final SvgValidationResult validate = SvgValidator.validate(Files.newInputStream(path));
            offendingFiles.add(path.toString());
            offendingAttributes.addAll(validate.getOffendingAttributes());
            offendingElements.addAll(validate.getOffendingElements());
            count++;
        }

        System.out.println("offendingFiles:" + offendingFiles);
        System.out.println("offendingAttributes:" + offendingAttributes);
        System.out.println("offendingElements:" + offendingElements);
        System.out.println("number of files:" + count);
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
