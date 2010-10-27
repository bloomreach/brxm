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
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.util.StringIndex;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseDelegate<T> implements Visitable<T> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BundleReader.class);

    Connection connection;
    String schemaObjectPrefix;
    StringIndex nameIndex = new TrivialStringIndex();
    StringIndex nsIndex = new TrivialStringIndex();

    public DatabaseDelegate(Connection connection, String schemaObjectPrefix) {
        this.connection = connection;
        this.schemaObjectPrefix = schemaObjectPrefix;
    }

    public abstract void accept(Visitor<T> visitor);

    public abstract int getSize();

    protected NodeId readID(DataInputStream in) throws IOException {
        if (in.readBoolean()) {
            byte[] bytes = new byte[16];
            int pos = 0;
            while (pos < 16) {
                pos += in.read(bytes, pos, 16 - pos);
            }
            return new NodeId(bytes);
        } else {
            return null;
        }
    }

    protected Name readIndexedQName(DataInputStream in) throws IOException {
        int index = in.readInt();
        if (index < 0) {
            return null;
        } else {
            String uri = nsIndex.indexToString(index);
            String local = nameIndex.indexToString(in.readInt());
            return NameFactoryImpl.getInstance().create(uri, local);
        }
    }

    protected Name readQName(DataInputStream in) throws IOException {
        String uri = nsIndex.indexToString(in.readInt());
        String local = in.readUTF();
        return NameFactoryImpl.getInstance().create(uri, local);
    }

    protected byte[] getBytes(Blob blob) throws SQLException, IOException {
        InputStream in = null;
        try {
            long length = blob.length();
            byte[] bytes = new byte[(int)length];
            in = blob.getBinaryStream();
            int read, pos = 0;
            while ((read = in.read(bytes, pos, bytes.length - pos)) > 0) {
                pos += read;
            }
            return bytes;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    static UUID nullUUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    static UUID create(NodeId nodeId) {
        if(nodeId == null)
            return nullUUID;
        //return UUID.nameUUIDFromBytes(getUUID(nodeId).getRawBytes());
        return UUID.fromString(UUID.fromString(nodeId.toString()).toString());
    }

    static UUID create(String string) {
        try {
            if(string == null || "".equals(string))
                return nullUUID;
            return UUID.fromString(string);
        } catch(IllegalArgumentException ex) {
            return null;
        }
    }

    static UUID getUUID(NodeId nodeId) {
        return UUID.fromString(nodeId.toString());
    }

    class TrivialStringIndex implements StringIndex {
        public int stringToIndex(String string) throws IllegalArgumentException {
            return 0;
        }

        public String indexToString(int idx) throws IllegalArgumentException {
            return "name";
        }
    }
}
