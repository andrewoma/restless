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

import com.github.andrewoma.restless.core.Contexts;
import com.github.andrewoma.restless.example.api.AuthenticationService;
import com.github.andrewoma.restless.example.api.Headers;
import com.github.andrewoma.restless.example.api.model.UserCredentials;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;

public class AuthenticationServiceImpl implements AuthenticationService {
    @Override
    public String login(UserCredentials credentials) {
        Subject subject = ThreadContext.getSubject();
        subject.login(new UsernamePasswordToken(credentials.getUsername(), credentials.getPassword()));
        org.apache.shiro.session.Session session = subject.getSession(true);
        return (String) session.getId();
    }

    @Override
    public void logout() {
        String sessionId = Contexts.get().getRequestHeaders().get(Headers.SESSION_ID);
        Subject subject = new Subject.Builder().sessionId(sessionId).buildSubject();
        subject.logout();
    }
}
