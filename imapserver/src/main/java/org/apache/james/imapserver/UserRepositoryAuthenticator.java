/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.imapserver;

import javax.annotation.Resource;

import org.apache.james.api.user.UsersRepository;
import org.apache.james.mailbox.store.Authenticator;

/**
 * Authenticator which use an UsersRepository to check if the user and password match
 *
 */
public class UserRepositoryAuthenticator implements Authenticator{

    private UsersRepository repos;

    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository repos) {
        this.repos = repos;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.mailbox.store.Authenticator#isAuthentic(java.lang.String, java.lang.CharSequence)
     */
    public boolean isAuthentic(String userid, CharSequence passwd) {
        return repos.test(userid, passwd.toString());
    }
    
}