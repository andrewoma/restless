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

package com.github.andrewoma.restless.core.exception;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ValidationException extends ClientException {
    public static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private List<FieldError> fieldErrors;

    @JsonCreator
    public ValidationException(@JsonProperty("id") String id,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("detail") String detail,
            @JsonProperty("fieldErrors") List<FieldError> fieldErrors) {
        super(id, code, message, detail);
        this.fieldErrors = fieldErrors;
    }

    public ValidationException(String id, String code, String message, String detail) {
        this(id, code, message, detail, null);
    }

    public ValidationException(String id, String message, String detail) {
        this(id, DEFAULT_ERROR_CODE, message, detail, null);
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    @Override
    public int getStatus() {
        return 400;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldError {
        private String field;
        private String code;
        private String message;

        @JsonCreator
        public FieldError(@JsonProperty("field") String field, @JsonProperty("code") String code, @JsonProperty("message") String message) {
            this.code = code;
            this.field = field;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "FieldError{" +
                    "code='" + code + '\'' +
                    ", field='" + field + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
