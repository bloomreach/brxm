/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.pagemodel.model;

import java.io.IOException;
import java.io.StringWriter;

import org.hippoecm.hst.util.HstResponseStateUtils;
import org.w3c.dom.Comment;

/**
 * Preamble/epilogue comment node representation model.
 */
public class NodeSpan {

    private String type;
    private Comment comment;
    private String data;

    public NodeSpan(Comment comment) {
        type = "comment";
        this.comment = comment;
    }

    public String getType() {
        return type;
    }

    public String getData() throws IOException {
        if (data == null) {
            StringWriter sw = new StringWriter(256);
            HstResponseStateUtils.printComment(comment, sw);
            data = sw.toString();
        }

        return data;
    }

}
