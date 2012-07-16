package org.hippoecm.repository.query.lucene;

import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;

/**
 * Created with IntelliJ IDEA. User: unico Date: 7/14/12 Time: 1:59 PM To change this template use File | Settings |
 * File Templates.
 */
public class HippoIndexReader extends FilterIndexReader {

    /**
     * <p>Construct a FilterIndexReader based on the specified base reader. Directory locking for delete, undeleteAll, and
     * setNorm operations is left to the base reader.</p> <p>Note that base reader is closed if this FilterIndexReader is
     * closed.</p>
     *
     * @param in specified base reader.
     */
    public HippoIndexReader(IndexReader in) {
        super(in);
    }


}
