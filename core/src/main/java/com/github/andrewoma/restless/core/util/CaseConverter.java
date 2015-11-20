/*
 * Copyright (c) 2015 Andrew O'Malley
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.andrewoma.restless.core.util;

public class CaseConverter {

    public static String camelCaseToLowerDash(String string) {
        if (string == null) return null;

        StringBuilder sb = new StringBuilder(string.length() + 10);
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i != 0) {
                    sb.append('-');
                }
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

    public static String lowerDashToLowerCamel(String string) {
        if (string == null) return null;

        StringBuilder sb = new StringBuilder(string.length());
        boolean upperNext = false;
        for (int i = 0; i < string.length(); i++) {
            char ch = string.charAt(i);
            if (ch == '-') {
                upperNext = true;
            } else {
                if (upperNext) {
                    upperNext = false;
                    sb.append(Character.toUpperCase(ch));
                } else {
                    sb.append(ch);
                }
            }
        }

        return sb.toString();
    }
}
