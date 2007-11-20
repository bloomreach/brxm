/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.dropbox;

import java.io.File;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

public class DropboxExt extends Dropbox {

    public DropboxExt(String repoLoc, String dropbox) throws RepositoryException {
        super(repoLoc, dropbox);
    }

    protected Node createFile(Node folder, File f) throws RepositoryException {
        Node n = super.createFile(folder, f);
        Property prop = n.getProperty("mimeType");
        String mimeType = prop.getString();

        if (mimeType.equals("image/png") || mimeType.equals("image/jpg")) {
            n.setProperty("DocType", "picture");
        } else if (mimeType.equals("application/msword") || mimeType.equals("application/vnd.oasis.opendocument.text")
                || mimeType.equals("application/pdf")) {
            n.setProperty("DocType", "document");
        } else if (mimeType.equals("text/html")) {
            n.setProperty("DocType", "webpage");
        } else if (mimeType.equals("text/plain")) {
            n.setProperty("DocType", "text");
        }
        return n;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 4) {
            usage();
        } else {
            try {
                DropboxExt box = new DropboxExt(args[0], args[1]);
                box.setCredentials(new SimpleCredentials(args[2], args[3].toCharArray()));
                box.drop("dropbox");
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }
    }
}