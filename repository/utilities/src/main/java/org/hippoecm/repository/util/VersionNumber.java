/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

public class VersionNumber implements Comparable<VersionNumber> {

    private int[] numbers;
    private String classifier;

    private VersionNumber(VersionNumber source, int index) {
        if (source.classifier != null && source.classifier.equals("SNAPSHOT")) {
            numbers = source.numbers;
        } else {
            numbers = new int[source.numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                if (i < index) {
                    numbers[i] = source.numbers[i];
                } else if (i > index) {
                    numbers[i] = 0;
                } else {
                    numbers[i] = source.numbers[i] + 1;
                }
            }
        }
        classifier = null;
    }

    public VersionNumber(String version) throws NumberFormatException {
        String[] elements = version.split("-");
        switch (elements.length) {
            case 0:
                numbers = new int[0];
                classifier = "";
                return;
            case 1:
                // nothing to do
                break;
            case 2:
                classifier = elements[1];
                break;
            default:
                throw new NumberFormatException();
        }
        elements = elements[0].split("\\.");
        numbers = new int[elements.length];
        int i = 0;
        for (String number : elements) {
            numbers[i++] = Integer.parseInt(number);
        }
    }

    public VersionNumber next() {
        return new VersionNumber(this, numbers.length - 1);
    }

    public int compareTo(VersionNumber other) {
        for (int i = 0; i < other.numbers.length && i < numbers.length; i++) {
            int compare = numbers[i] - other.numbers[i];
            if (compare != 0)
                return compare;
        }
        if (numbers.length != other.numbers.length)
            return numbers.length - other.numbers.length;
        if (classifier != null && other.classifier != null)
            return classifier.compareTo(other.classifier);
        else if (classifier != null)
            return -1;
        else if (other.classifier != null)
            return 1;
        return 0;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < numbers.length; i++) {
            if (i > 0)
                sb.append(".");
            sb.append(Integer.toString(numbers[i]));
        }
        if (classifier != null) {
            sb.append("-");
            sb.append(classifier);
        }
        return new String(sb);
    }

    public static VersionNumber versionFromURI(String uri) {
        return new VersionNumber(uri.substring(uri.lastIndexOf("/") + 1));
    }

    public String versionToURI(String uri) {
        return uri.substring(0, uri.lastIndexOf("/")+1) + toString();
    }
}
