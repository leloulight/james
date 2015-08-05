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
package org.apache.james.userrepository;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Repository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.services.User;
import org.apache.james.userrepository.UsersFileRepository.UsersFileRepositoryException;

/**
 * Unit Tests for <code>org.apache.james.userrepository.UsersFileRepository</code>
 *
 * @see org.apache.james.userrepository.UsersFileRepository
 *
 */
public class UsersFileRepositoryTest extends TestCase {
    
    private class MockUser implements User {
        
        /**
         * @param userName
         */
        public MockUser(String userName) {
            super();
            this.userName = userName;
        }

        private String userName;

        public String getUserName() {
            return userName;
        }

        public boolean verifyPassword(String pass) {
            return false;
        }

        public boolean setPassword(String newPass) {
            return false;
        }
        
    }
    
    private class MockServiceManager implements ServiceManager {

        public boolean hasService(String arg0) {
            return true;
        }

        public Object lookup(String arg0) throws ServiceException {
            return new Store() {

                public boolean isSelectable(Object arg0) {
                    return false;
                }

                public void release(Object arg0) {
                }

                public Object select(Object arg0) throws ServiceException {
                    return new MockObjectRepository();
                }};
        }

        public void release(Object arg0) {          
        }
        
    }
    
    private class MockObjectRepository implements ObjectRepository {

        public Repository getChildRepository(String arg0) {
            return null;
        }

        public boolean containsKey(String arg0) {
            return false;
        }

        public Object get(String arg0) {
            return null;
        }

        public Object get(String arg0, ClassLoader arg1) {
            return null;
        }

        public Iterator list() {
            return null;
        }

        public void put(String arg0, Object arg1) {     
        }

        public void remove(String arg0) {       
        }}
    
    
    private static final String destination = File.separator + "what" + File.separator + "ever";
    
    private UsersFileRepository repo;

    public UsersFileRepositoryTest(String name) {
        super(name);
    }

    /**
     * Setup a <code>UsersFileRepository</code> in a mocked Avalon container. 
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        repo = new UsersFileRepository();
        repo.enableLogging(new ConsoleLogger());
        DefaultConfiguration config = new DefaultConfiguration("");
        DefaultConfiguration destConfig = new DefaultConfiguration("destination");
        destConfig.setAttribute("URL", destination);
        config.addChild(destConfig);
        repo.configure(config);
        repo.service(new MockServiceManager());
        repo.initialize();
    }

    public final void testValidateUserPass() throws UsersFileRepositoryException {
        User user = new MockUser("legal");
        repo.validateUser(user);
        
        user = new MockUser("legal" + File.separator + "legal" + File.separator + "legal");
        repo.validateUser(user);
        
        user = new MockUser("legal" + '.' + "legal" + '.' + "legal");
        repo.validateUser(user);
    }
    
    public final void testValidateUserFail() {
        boolean failed = false;
        try {
            User user = new MockUser(".legal");
            repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            User user = new MockUser("legal.");;
            repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            User user = new MockUser("..");
            repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            User user = new MockUser("legal/../legal");
            repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {   
        User user = new MockUser(".");
        repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
        
        failed = false;
        try {
            User user = new MockUser("legal" + File.separator);
            repo.validateUser(user);
        } catch (UsersFileRepositoryException e) {
            failed = true;
        }
        assertTrue(failed);
            
    }
    
    public final void testAddUserPass() {
        User user = new MockUser("legal");
        repo.addUser(user);
    }
    
    public final void testAddUserFail() {
        boolean failed = false;
        try {
            User user = new MockUser(".." + File.separator + "illegal");
            repo.addUser(user);
        } catch (RuntimeException e) {
            failed = true;
        }
        assertTrue(failed);
    }

}
