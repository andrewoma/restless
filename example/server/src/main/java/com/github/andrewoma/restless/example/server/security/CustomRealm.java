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

import com.github.andrewoma.restless.example.server.model.Permission;
import com.github.andrewoma.restless.example.server.model.Role;
import com.github.andrewoma.restless.example.server.model.User;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// http://shiro-user.582556.n2.nabble.com/How-to-add-a-role-to-the-subject-td5562700.html
public class CustomRealm extends AuthorizingRealm {
    private Map<String, User> usersByName = new HashMap<String, User>();

    public CustomRealm() {
        Role admin = new Role("admin", Collections.singleton(new Permission("*")));
        Role pleb = new Role("pleb", Collections.singleton(new Permission("*:view")));

        addUser(new User("andrewo", "andrew@example.com", "password", Collections.singleton(admin)));
        addUser(new User("danielm", "daniel@example.com", "password", Collections.singleton(pleb)));
    }

    private void addUser(User user) {
        usersByName.put(user.getUsername(), user);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        User user = usersByName.get(upToken.getUsername());
        if (user == null) {
            throw new AuthenticationException("Username '" + upToken.getUsername() + "' not found");
        }

        return new SimpleAuthenticationInfo(user, user.getPassword(), getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Set<String> roles = new HashSet<String>();
        Set<org.apache.shiro.authz.Permission> permissions = new HashSet<org.apache.shiro.authz.Permission>();
        Collection<User> principalsList = principals.byType(User.class);

        if (principalsList.isEmpty()) {
            throw new AuthorizationException("Empty principals list");
        }

        for (User userPrincipal : principalsList) {
            for (Role role : userPrincipal.getRoles()) {
                roles.add(role.getName());
                for (Permission permission : role.getPermissions()) {
                    permissions.add(new WildcardPermission(permission.getName()));
                }
            }
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roles);
        info.setRoles(roles);
        info.setObjectPermissions(permissions);

        return info;
    }
}
