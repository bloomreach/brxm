package org.hippoecm.frontend.util;

import org.apache.wicket.IClusterable;

public class MaxLengthStringFormatter implements IClusterable {
    private static final long serialVersionUID = 1L;

    int maxLength;
    String splitter;
    int splitterLength;
    int indentLength;

    int splitterLengthLeft;

    public MaxLengthStringFormatter() {
        this(-1, "..", 3);
    }

    public MaxLengthStringFormatter(int maxLength, String split, int indentLength) {
        this.maxLength = maxLength;
        this.indentLength = indentLength;
        this.splitter = split;
        splitterLength = split.length();
        splitterLengthLeft = splitterLength / 2 + splitterLength % 2;
    }

    public boolean isTooLong(String input) {
        return isTooLong(input, 0);
    }

    public boolean isTooLong(String input, int indent) {
        return input.length() > getMax(indent);
    }

    public String parse(String input) {
        return parse(input, 0);
    }

    public String parse(String input, int indent) {
        if (maxLength == -1 || !isTooLong(input, indent))
            return input;

        int max = getMax(indent);
        if (max < 1) {
            return input;
        }
        int start = (max / 2) - splitterLengthLeft;
        int end = input.length() - (max - (start + splitterLength));
        return input.substring(0, start) + splitter + input.substring(end);
    }

    private int getMax(int indent) {
        return maxLength - (indent * indentLength);
    }
}
