// Found code at http://www.restlesscoder.net/software/namegen-java

// NameGen Java style, coded in 2006-2008 by Curtis Rueden
// This code is dedicated to the public domain.
// All copyrights (and warranties) are disclaimed.

package org.hippoecm.frontend.plugins.development.content.names;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.wicket.IClusterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Names implements IClusterable {
    private static final long serialVersionUID = 1L;


    static final Logger log = LoggerFactory.getLogger(Names.class);

    /** Marker for beginning and end of words. */
    protected static final char MARKER = '/';

    /** Number of times to try generating a name before giving up. */
    protected static final int MAX_ITERATIONS = 2000;

    protected static final int DEFAULT_WIDTH = 3;
    protected static final int DEFAULT_MAXIMUM_LENGTH = 25;
    protected static final boolean DEFAULT_ALLOW_SOURCE_NAMES = false;

    // -- Static fields --

    /** Debugging flag. */
    protected static boolean debug = false;

    // -- Fields --

    /**
     * The list of names used for input. We keep this data structure so that
     * we have the option to check if a generated name matches the input set.
     */
    protected HashSet<String> names;

    /** The list of names to disallow as output. */
    protected HashSet<String> no;

    /** Counters for valid N-tuples. */
    protected int[] counts;

    /** Sums of valid N-tuple counts, for computing probability. */
    protected int[] sums;

    /** Tuple width for generation algorithm. */
    protected int width;

    /** Maximum length of generated names. */
    protected int maxLen;

    /** Whether to allow generated names to match a source name. */
    protected boolean allowSourceNames;

    // -- Constructor --

    /**
     * Constructs a names object capable of generating random names.
     *
     * @param source Construct names based on the names from the given source.
     */
    public Names(HashSet<String> names) {
        this(names, new HashSet<String>(), DEFAULT_WIDTH);
    }

    /**
     * Constructs a names object capable of generating random names.
     *
     * @param source Construct names based on the names from the given source.
     * @param exclude Names to disallow from the output (may be null).
     * @param width Controls how similar to the existing names the generated
     *   names are&mdash;4 is similar, 2 is deviant, and 3 is in between.
     */
    public Names(HashSet<String> names, HashSet<String> no, int width) {
        if (names.size() == 0) {
            throw new IllegalArgumentException("No valid names.");
        }
        this.names = names;
        this.no = no;

        setWidth(width);
        setMaximumLength(DEFAULT_MAXIMUM_LENGTH);
        setAllowSourceNames(DEFAULT_ALLOW_SOURCE_NAMES);
    }

    // -- Names methods --

    /** Generates the given number of random names. */
    public String[] generate(int num) {
        String[] names = new String[num];
        for (int n = 0; n < num; n++) {
            String name = generate();
            if (name == null) {
                break;
            }
            names[n] = name;
        }
        return names;
    }

    /** Generates a random name. */
    public String generate() {
        log.debug("generate: begin name generation");
        StringBuffer sb = new StringBuffer();
        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            sb.setLength(0);
            for (int i = 0; i < width - 1; i++) {
                sb.append(MARKER);
            }
            boolean success = false;
            while (sb.length() < maxLen + width) {
                // tuple for N-1 letters
                String tuple = sb.substring(sb.length() - width + 1);
                log.debug("generate: [" + tuple + "] " + sb);
                int sum = sums[getIndex(tuple)];
                int ci = (int) (sum * Math.random());
                if (log.isDebugEnabled()) {
                    log.debug("generate:   sum=" + sum);
                    log.debug("generate:   ci=" + ci);
                    int total = 0;
                    for (int i = 0; i <= 26; i++) {
                        char cc = i == 26 ? MARKER : (char) ('a' + i);
                        int q = counts[getIndex(tuple + cc)];
                        total += q;
                        log.debug("generate:     count['" + cc + "']=" + q + " (" + total + ")");
                    }
                }
                char c = 'a' - 1;
                while (c <= 'z') {
                    c++;
                    ci -= counts[getIndex(tuple + c)];
                    if (ci < 0) {
                        break;
                    }
                }
                if (c > 'z') {
                    // successfully reached a terminating tuple
                    log.debug("generate: [" + tuple.substring(1) + MARKER + "] " + sb);
                    success = true;
                    break;
                }
                sb.append(c);
            }
            if (success) {
                String name = sb.substring(width - 1);
                if (!allowSourceNames && names.contains(name)) {
                    log.debug("generate: discarded source name: " + name);
                    continue;
                }
                if (no.contains(name)) {
                    log.debug("generate: discarded forbidden name: " + name);
                    continue;
                }
                log.debug("generate: name = " + name);
                return name;
            }
            // name too long; try again...
        }
        // not enough data; give up
        return null;
    }

    /** Gets the number of names used for input. */
    public int getSourceNameCount() {
        return names.size();
    }

    /** Gets the number of names on the exclusion list. */
    public int getExcludeNameCount() {
        return no.size();
    }

    /** Sets tuple width for generation algorithm. */
    public void setWidth(int width) {
        this.width = width;
        if (width < 2 || width > 5) {
            throw new IllegalArgumentException("Invalid width: " + width + " (expected 2 <= width <= 5)");
        }

        // rebuild counts and sums structures
        int size = 1;
        for (int i = 0; i < width; i++) {
            size *= 27;
        }
        counts = new int[size];
        sums = new int[size / 27];

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < width - 1; i++) {
            prefix.append(MARKER);
        }

        Iterator<String> iter = names.iterator();
        while (iter.hasNext()) {
            String name = iter.next();

            // prepend N-1 markers to the front, and append one marker to the end
            name = prefix.toString() + name + MARKER;

            // compile tuples from name
            int len = name.length();
            for (int i = width; i <= len; i++) {
                String s = name.substring(i - width, i);
                // increment count for this N-tuple
                counts[getIndex(s)]++;
                // increment sum for N-tuples starting with these N-1 letters
                sums[getIndex(s.substring(0, s.length() - 1))]++;
            }
        }
    }

    /** Gets tuple width for generation algorithm. */
    public int getWidth() {
        return width;
    }

    /** Sets the maximum length of each generated name. */
    public void setMaximumLength(int maxLen) {
        this.maxLen = maxLen;
    }

    /** Gets the maximum length of each generated name. */
    public int getMaximumLength() {
        return maxLen;
    }

    /** Sets whether to allow generated names to match a source name. */
    public void setAllowSourceNames(boolean allow) {
        allowSourceNames = allow;
    }

    /** Gets whether to allow generated names to match a source name. */
    public boolean isAllowSourceNames() {
        return allowSourceNames;
    }

    // -- Helper methods --

    /** Converts an N-tuple to an index. */
    protected int getIndex(String s) {
        int ndx = 0;
        for (int j = 0; j < s.length(); j++) {
            ndx *= 27;
            char c = s.charAt(j);
            int q = c == MARKER ? 26 : c - 'a';
            ndx += q;
        }
        return ndx;
    }

}
