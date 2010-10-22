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
package org.hippoecm.checker;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class IndicesReader implements Visitable<NodeIndexed> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    File basedir;

    IndicesReader(File basedir) {
        this.basedir = basedir;
    }

    public void accept(Visitor<NodeIndexed> visitor) {
        try {
            System.err.println("Reading indices " + basedir.getPath());
            FileInputStream in = new FileInputStream(new File(basedir, "indexes"));
            DataInputStream di = new DataInputStream(in);
            int counter = di.readInt();
            int numof = di.readInt();
            while (numof-- > 0) {
                String indexName = di.readUTF();
                readIndex(new File(basedir, indexName), visitor);
            }
            in.close();
        } catch (IOException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
    }

    void readIndex(File directory, Visitor<NodeIndexed> visitor) throws IOException {
        System.err.println("Reading index " + directory.getPath());
        Directory luceneDirectory = FSDirectory.getDirectory(directory);
        IndexReader reader = IndexReader.open(luceneDirectory);
        for (int i = 0; i < reader.maxDoc() - 1; i++) {
            if (!reader.isDeleted(i)) {
                Document document = reader.document(i);
                if (document != null) {
                    //System.err.println("DOCUMENT");
                    final String nodeUUID = document.getField("_:UUID").stringValue();
                    final String parentUUID = document.getField("_:PARENT").stringValue();
                    for (Iterator iter = document.getFields().iterator(); iter.hasNext();) {
                        Field field = (Field) iter.next();
                        if ("_:PARENT".equals(field.name())) {
                        } else if ("_:UUID".equals(field.name())) {
                        } else if ("_:PROPERTIES".equals(field.name())) {
                        }
                    }
                    visitor.visit(new NodeIndexed() {

                        public UUID getNode() {
                            return DatabaseDelegate.create(nodeUUID);
                        }

                        public UUID getParent() {
                            return DatabaseDelegate.create(parentUUID);
                        }
                    });
                }
            }
        }
    }

    void writeIndex(File directory, UUID uuid, UUID parent) throws IOException {
        System.err.println("Reading index " + directory.getPath());
        Directory luceneDirectory = FSDirectory.getDirectory(directory);
        Document document = null;

        IndexReader reader = IndexReader.open(luceneDirectory);
        for (int i = 0; i < reader.maxDoc() - 1; i++) {
            if (!reader.isDeleted(i)) {
                document = reader.document(i);
                if (document != null) {
                    String nodeUUID = document.getField("_:UUID").stringValue();
                    if (uuid.toString().equals(nodeUUID)) {
                        break;
                    }
                }
            }
        }
        reader.close();
        if (document != null) {
            IndexWriter writer = new IndexWriter(luceneDirectory, new StandardAnalyzer());
            if (parent != null) {
                document.removeField("_:PARENT");
                document.add(new Field("_:PARENT",uuid.toString().getBytes(), Field.Store.YES));
                writer.updateDocument(new Term("_:UUID", uuid.toString()), document);
            } else {
                writer.deleteDocuments(new Term("_:UUID", uuid.toString()));
            }
            writer.flush();
            writer.close();
        }
    }
}
