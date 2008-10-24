/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.tagging;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagCollection extends TreeMap<String, Tag> implements Iterable<IModel>, Serializable{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagCollection.class);

    private int multiplier = 1;

    public int getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(int multiplier) {
        this.multiplier = multiplier;
    }

    public boolean add(Tag t) {
        boolean exists = containsKey(t.getName());
        if (exists) {
            Tag tag = get(t.getName());
            tag.addScore(t.getScore());
        } else {
            put(t.getName(), t);
        }
        return !exists;
    }

    public void addAll(TagCollection col) {
        TagCollection collection = (TagCollection) col.clone();
        collection.normalizeScores();
        while (collection.size() > 0){
            String name = collection.firstKey();
            Tag tag = collection.get(name);
            tag.setScore(tag.getScore() * col.getMultiplier());
            add(tag);
            collection.remove(name);
        }
    }

    public Tag getTag(Tag t) {
        return get(t.getName());
    }

    public Iterator<IModel> iterator() {
        //return this.values().iterator();
        // werkt niet Tag vs IModel probleem
        TreeSet<IModel> set = new TreeSet<IModel>();
        for (Iterator<Map.Entry<String, Tag>> i = this.entrySet().iterator(); i.hasNext(); ){
            set.add(i.next().getValue());
        }
        return set.iterator();
    }
    
    public String toString(){
        StringBuffer buffer = new StringBuffer();
        for (Iterator<Tag> i = this.values().iterator(); i.hasNext();){
            Tag t = i.next();
            buffer.append(t.getName());
            buffer.append(", ");
        }
        return buffer.toString();
    }
    
    /**
     * Iterate over tags and normalize to a score between 0 and 100
     * @todo should this manipulate the current collection or return a new one???
     */
    public void normalizeScores(){
        double highest = -100.0;
        double lowest = -100.0;
        // detect highest and lowest score (needed to calculate the factor for normalization)
        for (Iterator<Tag> i = this.values().iterator(); i.hasNext();){
            Tag tag = i.next();
            // set to values of first tag
            if (highest == -100.0){
                highest = tag.getScore();
            }
            if (lowest == -100.0){
                lowest = tag.getScore();
            }
            // check if this tag exceeds the values
            if (tag.getScore() > highest){
                highest = tag.getScore();
            }
            if (tag.getScore() < lowest){
                lowest = tag.getScore();
            }
        }
        double factor = (highest)/100;
        for (Iterator<Tag> i = this.values().iterator(); i.hasNext();){
            Tag tag = i.next();
            tag.setScore((tag.getScore()) / factor);
        }
    }
}
