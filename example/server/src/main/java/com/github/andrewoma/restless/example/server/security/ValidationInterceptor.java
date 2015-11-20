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

package com.github.andrewoma.restless.example.server.security;

import com.github.andrewoma.restless.core.Context;
import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.core.exception.ValidationException;
import com.github.andrewoma.restless.core.proxy.MethodInterceptor;
import com.github.andrewoma.restless.core.proxy.MethodInvocation;
import com.github.andrewoma.restless.core.proxy.TargetMethodHandler;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ValidationInterceptor implements MethodInterceptor<Context> {
    private final Validator validator;

    public ValidationInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    public Object invoke(MethodInvocation<Context> invocation) throws Throwable {
        TargetMethodHandler<Context> handler = (TargetMethodHandler<Context>) invocation.getHandler();

        ExecutableValidator executableValidator = validator.forExecutables();

        Set<ConstraintViolation<Object>> violations = executableValidator.validateParameters(
                handler.getTarget(), invocation.getMethod(), invocation.getParameters());

        if (!violations.isEmpty()) {
            throw buildValidationException(invocation.getParameterNames(), violations);
        }

        return invocation.proceed();

        // TODO ... validate the return value?
//        executableValidator.validateReturnValue(handler.getTarget(), invocation.getMethod(), result);
    }


    private ValidationException buildValidationException(Object[] parameterNames, Set<? extends ConstraintViolation<?>> violations) {

        StringBuilder message = new StringBuilder();
        List<ValidationException.FieldError> fieldErrors = new ArrayList<ValidationException.FieldError>();

        for (Iterator<? extends ConstraintViolation<?>> i = violations.iterator(); i.hasNext(); ) {
            ConstraintViolation<?> constraintViolation = i.next();

            String field = calculateFieldName(parameterNames, constraintViolation);
            message.append(field);
            message.append(" ");

            message.append(constraintViolation.getMessage());

            fieldErrors.add(new ValidationException.FieldError(field, null, constraintViolation.getMessage()));

            if (i.hasNext()) {
                message.append("; ");
            }
        }

        return new ValidationException(Contexts.get().getRequestId(), ValidationException.DEFAULT_ERROR_CODE, message.toString(), null, fieldErrors);
    }

    private String calculateFieldName(Object[] parameterNames, ConstraintViolation<?> constraintViolation) {
        StringBuilder name = new StringBuilder();
        Path propertyPath = constraintViolation.getPropertyPath();
        Iterator<Path.Node> iterator = propertyPath.iterator();
        iterator.next();
        Path.Node arg = iterator.next();
        name.append(parameterNames[((Path.ParameterNode) arg).getParameterIndex()]);
        while (iterator.hasNext()) {
            name.append(".");
            name.append(iterator.next().getName());
        }
        return name.toString();
    }
}
