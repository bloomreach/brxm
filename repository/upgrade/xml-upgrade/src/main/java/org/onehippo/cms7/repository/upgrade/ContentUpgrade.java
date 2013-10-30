/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Namespace;
import de.pdark.decentxml.Node;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLIOSource;
import de.pdark.decentxml.XMLParseException;
import de.pdark.decentxml.XMLParser;

public class ContentUpgrade {

    public static final Namespace SV = new Namespace("sv", "http://www.jcp.org/jcr/sv/1.0");

    public void processDirectory(File directory) throws IOException {
        final File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".xml");
            }
        });
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            process(file);
        }

        final File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        });
        for (File child : directories) {
            processDirectory(child);
        }
    }

    public void process(File file) throws IOException {
        Document document;
        try {
            XMLParser reader = new XMLParser();
            document = reader.parse(new XMLIOSource(new FileInputStream(file)));

            final Element rootElement = document.getRootElement();
            processNode(rootElement, false);

            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
            writer.write(document.toString());
            writer.close();
        } catch (XMLParseException xpe) {
            System.out.println("Unable to process " + file.getAbsolutePath() + " : " + xpe.getMessage());
        }
    }

    private void processNode(Element element, boolean inDocument) {
        String primaryType = null;
        final List<Element> properties = element.getChildren("property", SV);
        Element mixins = null;
        String state = null;
        for (Element property : properties) {
            String propName = property.getAttribute("name", SV).getValue();
            if ("jcr:primaryType".equals(propName)) {
                primaryType = getPrimaryType(property);
            } else if ("jcr:mixinTypes".equals(propName)) {
                mixins = property;
            } else if ("hippostd:state".equals(propName)) {
                state = property.getChild("value", SV).getText();
            }
        }
        if (mixins != null) {
            convertMixins(mixins, inDocument, state);
        }

        if ("hippo:handle".equals(primaryType)) {
            inDocument = true;
        }
        final List<Element> children = element.getChildren("node", SV);
        for (Element child : children) {
            processNode(child, inDocument);
        }
    }

    private String getPrimaryType(Element property) {
        return property.getChild("value", SV).getText();
    }

    private void convertMixins(Element property, boolean doc, final String state) {
        boolean hasSkipIndex = false;
        for (Element value : property.getChildren("value", SV)) {
            String mixin = value.getText();
            if ("hippo:hardhandle".equals(mixin)) {
                value.setText("mix:referenceable");
            } else if ("hippo:harddocument".equals(mixin)) {
                if (doc && "unpublished".equals(state)) {
                    value.setText("mix:versionable");
                } else {
                    value.setText("mix:referenceable");
                }
            } else if ("hippo:skipindex".equals(mixin)) {
                hasSkipIndex = true;
            }
        }
        if ("draft".equals(state) && !hasSkipIndex) {
            addSkipIndex(property);
        }
    }

    private void addSkipIndex(final Element property) {
        final List<Node> children = property.getNodes();
        int lastValue = children.size() - 1;
        for (; lastValue >= 0; lastValue--) {
            if (children.get(lastValue) instanceof Element) {
                break;
            }
        }
        int lastText = lastValue - 1;
        for (; lastText >= 0; lastText--) {
            if (children.get(lastText) instanceof Text) {
                break;
            }
        }
        final Element value = new Element("value", SV);
        value.setBeginName("sv:value");
        value.setEndName("sv:value");
        value.addNode(new Text("hippo:skipindex"));
        if (lastText >= 0) {
            property.addNode(lastValue + 1, children.get(lastText).copy());
        } else {
            property.addNode(lastValue + 1, new Text("\n"));
        }
        lastValue++;
        property.addNode(lastValue + 1, value);
    }

    public static void main(String[] args) throws IOException {
        String path = System.getProperty("user.dir");
        if (args.length >= 1) {
            File test = new File(args[0]);
            if (!test.exists()) {
                throw new IOException("Path " + args[0] + " does not exist");
            }
            path = args[0];
        }
        File cwd = new File(path);
        ContentUpgrade upgrade = new ContentUpgrade();
        upgrade.processDirectory(cwd);
    }

}
