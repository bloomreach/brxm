package org.hippoecm.frontend.plugins.tagging;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagCollection extends TreeMap<String, Tag> implements Iterable<IModel>{
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
        TreeSet<IModel> set = new TreeSet<IModel>();
        for (Iterator<Map.Entry<String, Tag>> i = this.entrySet().iterator(); i.hasNext(); ){
            set.add(i.next().getValue());
        }
        return set.iterator();
    }
    
    
}
