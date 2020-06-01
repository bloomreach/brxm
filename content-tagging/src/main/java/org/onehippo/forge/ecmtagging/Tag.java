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

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This represents a tag (including its score).
 * 
 * @author Jeroen Tietema
 *
 */
public class Tag implements Comparable<Tag>, IModel {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(Tag.class);

    private String name;
    private double score = 1.0;

    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public Tag(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public void addScore(double score) {
        this.score += score;
    }

    /**
     * Compares two tags by name and score
     * 
     * <ul>
     * <li>Two tags are equal when they have the same name (score doesn't matter)</li>
     * <li>Tags are first compared by name, then by score</li>
     * </ul>
     * 
     */
    public int compareTo(Tag otherTag) {
        if (otherTag.getName().equals(name)) {
            return 0;
        } else if (otherTag.getScore() > score) {
            return 1;
        } else if (otherTag.getScore() < score) {
            return -1;
        } else {
            // tags are equal in score, compare strings (strings will never be equal)
            int c = otherTag.getName().compareTo(name);
            // normalize result to 1, 0 or -1
            if (c < 0) {
                return 1;
            } else if (c > 0) {
                return -1;
            } else {
                return 0;
            }

        }

    }

    /**
     * IModel (hack), needed to be able to pass the TagCollection to the frontend
     */
    public Object getObject() {
        return this;
    }

    /**
     * IModel (hack)
     */
    public void setObject(Object object) {
        Tag tag = (Tag) object;
        name = tag.getName();
        score = tag.getScore();
    }

    public void detach() {
    }

}
