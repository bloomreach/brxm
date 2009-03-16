package org.hippoecm.hst.ocm;

import java.util.Iterator;



public interface HippoStdNodeIterator extends Iterator {

    /**
     * Returns the next <code>HippoStdNode</code> in the iteration.
     *
     * @return the next <code>HippoStdNode</code> in the iteration.
     * @throws java.util.NoSuchElementException if iteration has no more <code>HippoStdNodes</code>s.
    */
   public HippoStdNode nextHippoStdNode();
   
   /**
    * Skip a number of elements in the iterator.
    *
    * @param skipNum the non-negative number of elements to skip
    * @throws java.util.NoSuchElementException
    *          if skipped past the last element in the iterator.
    */
   public void skip(int skipNum);

   /**
    * Returns the number of elements in the iterator.
    * If this information is unavailable, returns -1.
    *
    * @return a long
    */
   public long getSize();

   /**
    * Returns the current position within the iterator. The number
    * returned is the 0-based index of the next element in the iterator,
    * i.e. the one that will be returned on the subsequent <code>next</code> call.
    * <p/>
    * Note that this method does not check if there is a next element,
    * i.e. an empty iterator will always return 0.
    *
    * @return a long
    */
   public long getPosition();
}
