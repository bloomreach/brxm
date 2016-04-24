/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.l10n;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "testsuite")
public class Report {
    
    @XmlAttribute
    public String name;
    @XmlAttribute(name = "failures")
    public int failureCount;
    @XmlAttribute(name = "errors")
    public int errorCount;
    @XmlElement(name = "testcase")
    public Collection<TestCase> testCases = new ArrayList<>();
    private TestCase currentTestCase;

    public Report() {
    }

    Report(final String name) {
        this.name = name;
    }

    void addTestCase(final String name) {
        currentTestCase = new TestCase(name);
        testCases.add(currentTestCase);
    }

    void failure(final String message) {
        currentTestCase.addFailure(message);
        failureCount++;
    }

    void error(final String message) {
        currentTestCase.addError(message);
        errorCount++;
    }

    public static class TestCase {
        @XmlAttribute
        public String name;
        @XmlAttribute(name = "failures")
        public int failureCount;
        @XmlAttribute(name = "errors")
        public int errorCount;
        @XmlElement(name = "failure")
        public Collection<Failure> failures = new ArrayList<>();
        @XmlElement(name = "error")
        public Collection<Error> errors = new ArrayList<>();

        public TestCase() {}

        private TestCase(final String name) {
            this.name = name;
        }

        private void addFailure(final String message) {
            failures.add(new Failure(message));
            failureCount++;
        }

        private void addError(final String message) {
            errors.add(new Error(message));
            errorCount++;
        }

    }

    public static class Failure {
        @XmlAttribute
        public String message;

        public Failure() {}

        private Failure(final String message) {
            this.message = message;
        }
    }

    public static class Error {
        @XmlAttribute
        public String message;

        public Error() {}

        private Error(final String message) {
            this.message = message;
        }
    }

}
