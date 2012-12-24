/*
 *  Copyright 2010 Hippo.
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
package org.onehippo.repository.testutils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

public class CreateFixture {

    public void checkFixture() throws Exception {
        ByteArrayOutputStream fixtureDump;
        ByteArrayOutputStream setupDump;
        {
            RepositoryTestCase testCase = new RepositoryTestCase() { };
            RepositoryTestCase.setUpClass(true);
            RepositoryTestCase.prepareFixture();
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            Session session = repository.login("admin", "admin".toCharArray());
            fixtureDump = new ByteArrayOutputStream();
            dump(new PrintStream(fixtureDump), session.getRootNode());
            repository.close();
            RepositoryTestCase.tearDownClass(true);
        } {
            RepositoryTestCase testCase = new RepositoryTestCase() { };
            RepositoryTestCase.setUpClass(true);
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            Session session = repository.login("admin", "admin".toCharArray());
            setupDump = new ByteArrayOutputStream();
            dump(new PrintStream(setupDump), session.getRootNode());
            repository.close();
            RepositoryTestCase.tearDownClass(true);
        }
        assertEquals("Fixture has become out of date", setupDump.toString("UTF-8"), fixtureDump.toString("UTF-8"));
    }

    public void createFixture() throws RepositoryException, IOException {
        HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
        repository.close();
        FileOutputStream ostream = new FileOutputStream("../src/test/fixtures/dump.zip");
        dump(ostream);
        ostream.close();
    }

    static protected void dump(OutputStream output) throws IOException {
        JarOutputStream ostream = new JarOutputStream(output);
        for (String path : new String[] {".lock", "repository", "version", "workspaces"}) {
            dump(ostream, path);
        }
        ostream.close();
    }

    static private void dump(JarOutputStream ostream, String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) {
                ZipEntry ze = new ZipEntry(path + file.separator);
                ostream.putNextEntry(ze);
                ostream.closeEntry();
                for (String item : file.list()) {
                    dump(ostream, path + file.separator + item);
                }
            } else {
                ZipEntry ze = new ZipEntry(path);
                ostream.putNextEntry(ze);
                FileInputStream istream = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int len;
                do {
                    len = istream.read(buffer);
                    if (len >= 0) {
                        ostream.write(buffer, 0, len);
                    }
                } while (len >= 0);
                ostream.closeEntry();
            }
        }
    }

    private void dump(PrintStream out, Node node) throws RepositoryException {
        out.println(node.getPath()+"\t"+node.getPrimaryNodeType().getName());
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if(prop.getName().equals("jcr:primaryType") ||
               prop.getName().equals("jcr:uuid") ||
               prop.getName().equals(HippoNodeType.HIPPO_PATHS)) {
                continue;
            }
            if (prop.getDefinition().isMultiple()) {
                Value[] values = prop.getValues();
                for (int i = 0; i < values.length; i++) {
                    out.print(prop.getName()+"\t");
                    if (values[i].getType() == PropertyType.BINARY || prop.getName().equals("jcr:data")) {
                        out.println("<<binary>>");
                    } else if(values[i].getString().contains("\n")) {
                        out.println("<<multi-line>>");
                    } else {
                        out.println(values[i].getString());
                    }
                }
            } else {
                    out.print(prop.getName()+"\t");
                if (prop.getType() == PropertyType.BINARY || prop.getName().equals("jcr:data")) {
                    out.println("<<binary>>");
                } else if(prop.getString().contains("\n")) {
                    out.println("<<multi-line>>");
                } else {
                    out.println(prop.getString());
                }
            }
        }
        List<Node> children = new LinkedList<Node>();
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            children.add(iter.nextNode());
        }
        if(!node.getPrimaryNodeType().hasOrderableChildNodes()) {
            Collections.sort(children, new Comparator<Node>() {
                public int compare(Node o1, Node o2) {
                    try {
                        return o1.getPath().compareTo(o2.getPath());
                    } catch(RepositoryException ex) {
                        return 0;
                    }
                }
            });
        }
        for (Node child : children) {
            if (child.getPath().equals("/jcr:system"))
                continue;
            if (child.getPath().equals("/jcr:system/jcr:versionStorage"))
                continue;
            if (!(child instanceof HippoNode) || ( ((HippoNode)child).getCanonicalNode() != null && child.isSame(((HippoNode)child).getCanonicalNode()))) {
                dump(out, child);
            }
        }
    }
}
