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

import org.apache.avalon.cornerstone.services.store.ObjectRepository;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * Implementation of a Repository to store users on the File System.
 *
 * Requires a configuration element in the .conf.xml file of the form:
 * &lt;repository destinationURL="file://path-to-root-dir-for-repository"
 * type="USERS" model="SYNCHRONOUS"/&gt; Requires a logger called
 * UsersRepository.
 *
 *
 * @version CVS $Revision$
 *
 */
public class UsersFileRepository extends AbstractLogEnabled implements
		UsersRepository, Configurable, Serviceable, Initializable {

	public class UsersFileRepositoryException extends Exception {

		/**
         * 
         */
		private static final long serialVersionUID = -3156859346280071870L;

		public UsersFileRepositoryException() {
			super();
		}

		/**
		 * @param message
		 * @param cause
		 */
		public UsersFileRepositoryException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * @param message
		 */
		public UsersFileRepositoryException(String message) {
			super(message);
		}

		/**
		 * @param cause
		 */
		public UsersFileRepositoryException(Throwable cause) {
			super(cause);
		}

	}

	/**
	 * Whether 'deep debugging' is turned on.
	 */
	protected static boolean DEEP_DEBUG = false;

	private Store store;
	private ObjectRepository or;

	/**
	 * The destination URL used to define the repository.
	 */
	private String destination;
	private File destinationCanonicalFile;

	/**
	 * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
	 */
	public void service(final ServiceManager componentManager)
			throws ServiceException {

		try {
			store = (Store) componentManager.lookup(Store.ROLE);
		} catch (Exception e) {
			final String message = "Failed to retrieve Store component:"
					+ e.getMessage();
			getLogger().error(message, e);
			throw new ServiceException("", message, e);
		}
	}

	/**
	 * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
	 */
	public void configure(final Configuration configuration)
			throws ConfigurationException {

		destination = configuration.getChild("destination").getAttribute("URL");

		if (!destination.endsWith(File.separator)) {
			destination += File.separator;
		}
		try {
			destinationCanonicalFile = new File(destination).getCanonicalFile();
		} catch (IOException e) {
			throw new ConfigurationException("destination>>URL", e);
		}
		getLogger().debug("Canonical destination: " + destinationCanonicalFile);
	}

	/**
	 * @see org.apache.avalon.framework.activity.Initializable#initialize()
	 */
	public void initialize() throws Exception {

		try {
			// prepare Configurations for object and stream repositories
			final DefaultConfiguration objectConfiguration = new DefaultConfiguration(
					"repository", "generated:UsersFileRepository.compose()");

			objectConfiguration.setAttribute("destinationURL", destination);
			objectConfiguration.setAttribute("type", "OBJECT");
			objectConfiguration.setAttribute("model", "SYNCHRONOUS");

			or = (ObjectRepository) store.select(objectConfiguration);
			if (getLogger().isDebugEnabled()) {
				StringBuffer logBuffer = new StringBuffer(192)
						.append(this.getClass().getName())
						.append(" created in ").append(destination);
				getLogger().debug(logBuffer.toString());
			}
		} catch (Exception e) {
			if (getLogger().isErrorEnabled()) {
				getLogger().error(
						"Failed to initialize repository:" + e.getMessage(), e);
			}
			throw e;
		}
	}

	/**
	 * List users in repository.
	 *
	 * @return Iterator over a collection of Strings, each being one user in the
	 *         repository.
	 */
	public Iterator list() {
		return or.list();
	}

	/**
	 * Update the repository with the specified user object. A user object with
	 * this username must already exist.
	 *
	 * @param user
	 *            the user to be added.
	 *
	 * @return true if successful.
	 */
	public synchronized boolean addUser(User user) {
		String username = user.getUserName();
		if (contains(username)) {
			return false;
		}
		try {
			validateUser(user);
			or.put(username, user);
		} catch (Exception e) {
			throw new RuntimeException("Exception caught while storing user: "
					+ e);
		}
		return true;
	}

	public void addUser(String name, Object attributes) {
		if (attributes instanceof String) {
			User newbie = new DefaultUser(name, "SHA");
			newbie.setPassword((String) attributes);
			addUser(newbie);
		} else {
			throw new RuntimeException("Improper use of deprecated method"
					+ " - use addUser(User user)");
		}
	}

	public boolean addUser(String username, String password) {
		User newbie = new DefaultJamesUser(username, "SHA");
		newbie.setPassword(password);
		return addUser(newbie);
	}

	public synchronized User getUserByName(String name) {
		if (contains(name)) {
			try {
				return (User) or.get(name);
			} catch (Exception e) {
				throw new RuntimeException("Exception while retrieving user: "
						+ e.getMessage());
			}
		} else {
			return null;
		}
	}

	public User getUserByNameCaseInsensitive(String name) {
		String realName = getRealName(name);
		if (realName == null) {
			return null;
		}
		return getUserByName(realName);
	}

	public String getRealName(String name) {
		Iterator it = list();
		while (it.hasNext()) {
			String temp = (String) it.next();
			if (name.equalsIgnoreCase(temp)) {
				return temp;
			}
		}
		return null;
	}

	public boolean updateUser(User user) {
		String username = user.getUserName();
		if (!contains(username)) {
			return false;
		}
		try {
			or.put(username, user);
		} catch (Exception e) {
			throw new RuntimeException("Exception caught while storing user: "
					+ e);
		}
		return true;
	}

	public synchronized void removeUser(String name) {
		or.remove(name);
	}

	public boolean contains(String name) {
		return or.containsKey(name);
	}

	public boolean containsCaseInsensitive(String name) {
		Iterator it = list();
		while (it.hasNext()) {
			if (name.equalsIgnoreCase((String) it.next())) {
				return true;
			}
		}
		return false;
	}

	public boolean test(String name, String password) {
		User user;
		try {
			if (contains(name)) {
				user = (User) or.get(name);
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new RuntimeException("Exception retrieving User" + e);
		}
		return user.verifyPassword(password);
	}

	public int countUsers() {
		int count = 0;
		for (Iterator it = list(); it.hasNext(); it.next()) {
			count++;
		}
		return count;
	}

	/**
	 * Validate the passed <code>User</code>.
	 * 
	 * <p>
	 * Enforces partial RFC 3696 compliance and a file system 'jail' such that
	 * only user names that will result in a file that is a child of the
	 * configured directory for the repository pass validation.
	 * 
	 * @see org.apache.james.userrepository.UsersFileRepositoryTest
	 * 
	 * @param user
	 * @throws UsersFileRepositoryException
	 */
	protected void validateUser(final User user)
			throws UsersFileRepositoryException {

		// "." is never allowed as a starting character. It is neither RFC 3696
		// compliant or safe
		if (user.getUserName().startsWith(".")) {
			UsersFileRepositoryException ex = new UsersFileRepositoryException(
					"User name \"" + user.getUserName()
							+ "\" starts with \".\"");
			getLogger().error("User name validation failure", ex);
			throw ex;
		}

		// "." is never allowed as an ending character. It is neither RFC 3696
		// compliant or safe
		if (user.getUserName().endsWith(".")) {
			UsersFileRepositoryException ex = new UsersFileRepositoryException(
					"User name \"" + user.getUserName() + "\" ends with \".\"");
			getLogger().error("User name validation failure", ex);
			throw ex;
		}

		// A sequence of two or more "." is never allowed. It is neither RFC
		// 3696 compliant or safe
		if (user.getUserName().contains("..")) {
			UsersFileRepositoryException ex = new UsersFileRepositoryException(
					"User name \"" + user.getUserName() + "\" contains \"..\"");
			getLogger().error("User name validation failure", ex);
			throw ex;
		}

		// Absolute path conversion discards the trailing file separator
		// so "X" and "X/" resolve to the same path potentially resulting in
		// conflicts
		if (user.getUserName().endsWith(File.separator)) {
			UsersFileRepositoryException ex = new UsersFileRepositoryException(
					"User name \"" + user.getUserName()
							+ "\" ends with a file name separator");
			getLogger().error("User name validation failure", ex);
			throw ex;
		}

		// Canonical paths derived from the user name must be children
		// of the configured destination
		try {
			File targetCanonicalFile = new File(destinationCanonicalFile,
					user.getUserName()).getCanonicalFile();
			boolean isChild = false;
			File targetParentCanonicalFile = targetCanonicalFile
					.getParentFile().getCanonicalFile();
			while (!isChild && null != targetParentCanonicalFile) {
				isChild = destinationCanonicalFile
						.equals(targetParentCanonicalFile);
				targetParentCanonicalFile = targetParentCanonicalFile
						.getParentFile().getCanonicalFile();
			}
			if (!isChild) {
				UsersFileRepositoryException ex = new UsersFileRepositoryException(
						"The canonical path \""
								+ targetCanonicalFile
								+ "\" for user name \""
								+ user.getUserName()
								+ "\" is invalid. The resultant path is not a child of \""
								+ destinationCanonicalFile + "\"");
				getLogger().error("User name validation failure", ex);
				throw ex;
			} else if (getLogger().isDebugEnabled()) {
				getLogger()
						.debug("The canonical path \""
								+ targetCanonicalFile
								+ "\" for user name \""
								+ user.getUserName()
								+ "\" is valid. The resultant path is a child of \""
								+ destinationCanonicalFile + "\"");
			}
		} catch (IOException e) {
			throw new UsersFileRepositoryException(e);
		}
	}

}
