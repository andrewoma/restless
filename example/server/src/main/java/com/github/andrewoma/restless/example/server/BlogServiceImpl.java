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

package com.github.andrewoma.restless.example.server;

import com.github.andrewoma.restless.example.api.BlogService;
import com.github.andrewoma.restless.example.api.model.BlogDetails;
import com.github.andrewoma.restless.example.api.model.BlogResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class BlogServiceImpl implements BlogService {
    private Map<Integer, BlogResponse> blogs = new ConcurrentHashMap<Integer, BlogResponse>();
    private AtomicInteger blogId = new AtomicInteger(0);
    private SecurityService securityService;

    public BlogServiceImpl(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public BlogResponse createBlog(String name, String description, BlogDetails details) {
        BlogResponse blog = new BlogResponse(blogId.incrementAndGet(), name, description, securityService.currentUser());
        blogs.put(blog.getId(), blog);
        return blog;
    }

    @Override
    public BlogResponse getBlog(int blogId) {
        return blogs.get(blogId);
    }
}
