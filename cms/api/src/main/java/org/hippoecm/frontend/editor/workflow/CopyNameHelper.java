/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.workflow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.StringCodec;

public class CopyNameHelper {

    private final Pattern firstcopy;
    private final Pattern othercopies;
    private final StringCodec codec;
    private final String copyOf;

    public CopyNameHelper(StringCodec codec, String copyOf) {
        this.codec = codec;
        this.copyOf = copyOf;
        this.firstcopy = Pattern.compile(".* \\(" + copyOf + "\\)$");
        this.othercopies = Pattern.compile(".* \\(" + copyOf + " ([0-9]*?)\\)$");
    }

    public String getCopyName(String name, Node folder) throws RepositoryException {
        String base;
        int number;
        if (firstcopy.matcher(name).matches()) {
            base = name.substring(0, name.lastIndexOf(" (" + copyOf + ")"));
            number = 2;
        } else if (othercopies.matcher(name).matches()) {
            Matcher matcher = othercopies.matcher(name);
            matcher.find();
            String match = matcher.group(1);
            base = name.substring(0, name.lastIndexOf(" ("));
            number = Integer.parseInt(match) + 1;
        } else {
            base = name;
            number = 1;
        }
        if (folder != null) {
            String nodeName;
            do {
                if (number == 1) {
                    name = base + " (" + copyOf + ")";
                } else {
                    name = base + " (" + copyOf + " " + (number) + ")";
                }
                nodeName = codec.encode(name);
                number++;
            } while (folder.hasNode(nodeName));
        } else {
            name = base + "(" + (number + 1) + ")";
        }
        return name;
    }
}
