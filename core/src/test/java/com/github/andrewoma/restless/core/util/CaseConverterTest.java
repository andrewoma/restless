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

import org.junit.Test;

import static com.github.andrewoma.restless.core.util.CaseConverter.camelCaseToLowerDash;
import static com.github.andrewoma.restless.core.util.CaseConverter.lowerDashToLowerCamel;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CaseConverterTest {

    @Test
    public void shouldConvertCamelToLowerDash() {
        assertThat(camelCaseToLowerDash(null)).isNull();
        assertThat(camelCaseToLowerDash("")).isEqualTo("");
        assertThat(camelCaseToLowerDash("A")).isEqualTo("a");
        assertThat(camelCaseToLowerDash("AA")).isEqualTo("a-a");
        assertThat(camelCaseToLowerDash("CamelCaseRocks")).isEqualTo("camel-case-rocks");
        assertThat(camelCaseToLowerDash("camelCaseRocks")).isEqualTo("camel-case-rocks");
    }

    @Test
    public void shouldConvertLowerDashCamelToLowerCamel() {
        assertThat(lowerDashToLowerCamel(null)).isNull();
        assertThat(lowerDashToLowerCamel("")).isEqualTo("");
        assertThat(lowerDashToLowerCamel("a")).isEqualTo("a");
        assertThat(lowerDashToLowerCamel("a-a")).isEqualTo("aA");
        assertThat(lowerDashToLowerCamel("camel-case-rocks")).isEqualTo("camelCaseRocks");
    }
}