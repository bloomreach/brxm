package org.hippoecm.frontend.plugins.tagging;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tag implements Comparable<Tag>, IModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(Tag.class);

    private String name;
    private int score = 1;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Tag(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void addScore(int score) {
        this.score += score;
    }

    public int compareTo(Tag otherTag) {
        if (otherTag.getName().equals(name)) {
            return 0;
        } else if (otherTag.getScore() > score) {
            return -1;
        } else if (otherTag.getScore() < score) {
            return 1;
        } else {
            // tags are equal in score, compare strings
            int c = otherTag.getName().compareTo(name);
            // normalize result to 1, 0 or -1
            if (c > 0){
                return 1;
            }else if (c < 0){
                return -1;
            } else{
                return 0;
            }
            
        }

    }

    public Object getObject() {
        return this;
    }

    public void setObject(Object object) {
        Tag tag = (Tag) object;
        name = tag.getName();
        score = tag.getScore();
        
    }

    public void detach() {}

}
