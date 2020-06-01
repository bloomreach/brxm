/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.io.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a collection of Tags. The collection contains
 * just one of every tag. When adding existing Tags the score
 * off the new Tag is added to the existing one.
 * You can also add collections to each other.
 * 
 * A Collection also has a multiplier. It can be used to make
 * collections more important than other collections.
 * 
 * @author Jeroen Tietema
 *
 */
public class TagCollection extends TreeMap<String, Tag> implements Iterable<IModel>, IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagCollection.class);
    public static final double DEFAULT_HIGH = -100.0;
    public static final double DEFAULT_LOW = -100.0;

    private double multiplier = 1;

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
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
        for (Entry<String, Tag> entry : this.entrySet()){
            set.add(entry.getValue());
        }
        return set.iterator();
    }
    
    public String toString(){
        StringBuilder buffer = new StringBuilder();
        for (Tag t : this.values()){
            buffer.append(t.getName());
            buffer.append(", ");
        }
        return buffer.toString();
    }
    
    /**
     * Iterate over tags and normalize to a score between 0 and 100
     * @todo should this manipulate the current collection or return a new one???
     */
    public TagCollection normalizeScores(){
        double highest = DEFAULT_HIGH;
        double lowest = DEFAULT_LOW;
        // detect highest and lowest score (needed to calculate the factor for normalization)
        for (Tag tag : this.values()){
            // set to values of first tag
            if (highest == DEFAULT_HIGH){
                highest = tag.getScore();
            }
            if (lowest == DEFAULT_LOW){
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
        for (Tag tag : this.values()){
            tag.setScore((tag.getScore()) / factor);
        }
        return this; // for chaining
    }
    
    public TagCollection top(int limit){
        TagCollection collection = new TagCollection();
        int count = 0;
        for (Iterator<IModel> i = this.iterator(); i.hasNext() && count < limit; count++){
            collection.add((Tag) i.next().getObject());
        }
        return collection;
    }
}
