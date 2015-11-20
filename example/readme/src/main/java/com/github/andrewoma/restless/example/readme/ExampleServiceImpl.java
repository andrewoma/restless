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

package com.github.andrewoma.restless.example.readme;

import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.core.exception.NotFoundException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ExampleServiceImpl implements ExampleService {
    private Map<Long, Foo> foos = new ConcurrentHashMap<Long, Foo>();
    private AtomicLong fooId = new AtomicLong(0);

    @Override
    public String echo(String name, String message) {
        return "Hello " + name + "! You said '" + message + "'";
    }

    @Override
    public long createFoo(Foo foo) {
        long id = fooId.incrementAndGet();
        foos.put(id, foo);
        return id;
    }

    @Override
    public Foo getFoo(Long id) {
        Foo foo = foos.get(id);
        if (foo == null) {
            throw new NotFoundException(Contexts.get().getRequestId(), "Foo with id '" + id + "' does not exist", null);
        }
        return foo;
    }
}
