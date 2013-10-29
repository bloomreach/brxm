/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import java.util.regex.Pattern;

/**
 * GlobCompiler
 * <P>
 * Translate glob expression into regular expression to compile pattern.
 * Inspired by org/apache/oro/text/GlobCompiler.java.
 * </P>
 * <P>
 * This compiler supports the following glob syntax:
 * <TABLE border="1">
 * <TR>
 * <TH>Task</TH>
 * <TH>Example</TH>
 * </TR>
 * <TR>
 * <TD>Match one or zero unknown characters</TD>
 * <TD>
 * ?at matches at, Cat, cat, Bat or bat
 * </TD>
 * </TR>
 * <TR>
 * <TD>Match any number of unknown characters</TD>
 * <TD>
 * ?at matches Cat, cat, Bat or bat, but not at
 * </TD>
 * </TR>
 * <TR>
 * <TD>Match any number of unknown characters</TD>
 * <TD>
 * Law* matches Law, Laws, or Lawyer
 * </TD>
 * </TR>
 * <TR>
 * <TD>Match a character as part of a group of characters</TD>
 * <TD>
 * [CB]at matches Cat or Bat but not cat or bat
 * </TD>
 * </TR>
 * <TR>
 * </TABLE>
 * <EM>Note: This compiler escapes all the meta characters such as '*', '.', etc. from the expression input automatically.</EM>
 * </P>
 * <P>
 * Refer to <a href="http://en.wikipedia.org/wiki/Glob_%28programming%29">http://en.wikipedia.org/wiki/Glob_%28programming%29</a>
 * for general information on glob expression.
 * </P>
 */
public class GlobCompiler {

    private boolean questionMatchesZero;

    public GlobCompiler() {
    }

    /**
     * Returns the flag whether this compiler creates a pattern matching zero unknown character by '?' expression.
     * If true, it matches with zero unknown character by '?'. Otherwise, it matches only with one unknown character by '?'.
     * @return
     */
    public boolean isQuestionMatchesZero() {
        return questionMatchesZero;
    }

    /**
     * Sets the flag whether this compiler creates a pattern matching zero character by '?' expression.
     * If set to true, it matches with zero unknown character by '?'. Otherwise, it matches only with one unknown character by '?'.
     * @param questionMatchesZero
     */
    public void setQuestionMatchesZero(boolean questionMatchesZero) {
        this.questionMatchesZero = questionMatchesZero;
    }

    /**
     * Compiles the glob expression and returns a {@link Pattern} object.
     * @param globExpr
     * @return
     */
    public Pattern compile(String globExpr) {
        return compile(globExpr, 0);
    }

    /**
     * Compiles the glob expression and returns a {@link Pattern} object with the Java reglar expresson compile option.
     * @param globExpr
     * @param options The Java regular expression compile option such as {@link Pattern#CASE_INSENSITIVE}.
     * @return
     */
    public Pattern compile(String globExpr, int options) {
        char [] pattern = globExpr.toCharArray();
        boolean inCharSet;
        int ch;
        StringBuilder buffer;

        buffer = new StringBuilder(2 * pattern.length);
        inCharSet = false;

        for (ch = 0; ch < pattern.length; ch++) {
            switch (pattern[ch]) {
            case '*':
                if (inCharSet)
                    buffer.append('*');
                else {
                    buffer.append(".*");
                }
                break;
            case '?':
                if (inCharSet)
                    buffer.append('?');
                else {
                    if (isQuestionMatchesZero()) {
                        buffer.append(".?");
                    } else {
                        buffer.append('.');
                    }
                }
                break;
            case '[':
                inCharSet = true;
                buffer.append(pattern[ch]);

                if (ch + 1 < pattern.length) {
                    switch (pattern[ch + 1]) {
                    case '!':
                    case '^':
                        buffer.append('^');
                        ++ch;
                        continue;
                    case ']':
                        buffer.append(']');
                        ++ch;
                        continue;
                    }
                }
                break;
            case ']':
                inCharSet = false;
                buffer.append(pattern[ch]);
                break;
            case '\\':
                buffer.append('\\');
                if (ch == pattern.length - 1) {
                    buffer.append('\\');
                } else if (__isGlobMetaCharacter(pattern[ch + 1]))
                    buffer.append(pattern[++ch]);
                else
                    buffer.append('\\');
                break;
            default:
                if (!inCharSet && __isPerl5MetaCharacter(pattern[ch]))
                    buffer.append('\\');
                buffer.append(pattern[ch]);
                break;
            }
        }

        if (options > 0) {
            return Pattern.compile(buffer.toString(), options);
        } else {
            return Pattern.compile(buffer.toString());
        }
    }

    private static boolean __isPerl5MetaCharacter(char ch) {
        return ("'*?+[]()|^$.{}\\".indexOf(ch) >= 0);
    }

    private static boolean __isGlobMetaCharacter(char ch) {
        return ("*?[]".indexOf(ch) >= 0);
    }
}
