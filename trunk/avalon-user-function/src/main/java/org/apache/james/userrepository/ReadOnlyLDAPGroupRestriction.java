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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.commons.configuration.HierarchicalConfiguration;

/**
 * <p>
 * Encapsulates the information required to restrict users to LDAP groups or roles.
 * Instances of this type are populated from the contents of the <code>&lt;users-store&gt;</code>
 * configuration child-element <code>&lt;restriction&gt;<code>.  
 * </p>
 *   
 * @see ReadOnlyUsersLDAPRepository
 * @see ReadOnlyLDAPUser
 * 
 * @author Obi Ezechukwu
 */

public class ReadOnlyLDAPGroupRestriction
{   
	/**
	 * <p>
	 * The name of the LDAP attribute name which holds the unique names 
	 * (distinguished-names/DNs) of the members of the group/role.
	 * 
	 * </p>  
	 */
    private String memberAttribute;
    
    /**
     * <p>
     * The distinguished-names of the LDAP groups/roles to which James users must 
     * belong. A user who is not a member of at least one of the groups or roles 
     * specified here will not be allowed to authenticate against James. If the 
     * list is empty, group/role restriction  will be disabled.
     * </p>
     */
    private List<String> groupDNs; 
    
    
    /**
     * <p>
     * Initialises an instance from the contents of 
     * a <code>&lt;restriction&gt;<code> configuration XML 
     * element. 
     * </p> 
     * @param configuration	The avalon configuration instance that 
     * encapsulates the contents of the <code>&lt;restriction&gt;<code>
     * XML element.
     * 
     * @throws ConfigurationException	If an error occurs extracting
     * values from the configuration element.
     */
    @SuppressWarnings("unchecked")
    public ReadOnlyLDAPGroupRestriction(HierarchicalConfiguration configuration) {
        groupDNs = new ArrayList<String>();

		if (configuration != null) {
			memberAttribute = configuration.getString("[@memberAttribute]");

			if (configuration.getKeys("group").hasNext()) {
			List<String> groupNames = configuration
					.getList("group");

				for (int i = 0; i < groupNames.size(); i++) {
					groupDNs.add(groupNames.get(i));
				}
			}
		}
	}

    /**
     * <p>
     * Indicates if group/role-based restriction is enabled for the
     * the user-store, based on the information encapsulated in the instance.
     * </p>
     * @return <code>True</code> If there list of group/role distinguished 
     * names is not empty, and <code>false</code> otherwise.
     */
	protected boolean isActivated() {
		return !groupDNs.isEmpty();
	}

	/**
	 * <p>
	 * Converts an instance of this type to a string.
	 * </p>
	 * @return A string representation of the instance. 
	 */
	public String toString() {
		return "Activated=" + isActivated() + "; Groups=" + groupDNs;
	}

	/**
	 * <p>
	 * Returns the distinguished-names (DNs) of all the members of the 
	 * groups specified in the restriction list. The information is 
	 * organised as a list of <code>&quot;&lt;groupDN&gt;=&lt;
	 * [userDN1,userDN2,...,userDNn]&gt;&quot;</code>. Put differently,
	 * each <code>groupDN</code> is associated to a list of <code>userDNs</code>.   
	 * </p>
	 * 
	 * @param connection	The connection to the LDAP directory server.
	 * @return	Returns a map of groupDNs to userDN lists.
	 * @throws NamingException	Propagated from underlying 
	 * LDAP communication layer. 
	 */
	protected Map<String,Collection<String>> getGroupMembershipLists(SimpleLDAPConnection connection)
			throws NamingException {
		Map<String,Collection<String>> result = new HashMap<String,Collection<String>>();

		Iterator<String> groupDNsIterator = groupDNs.iterator();

		Attributes groupAttributes;
		while (groupDNsIterator.hasNext()) {
			String groupDN = (String) groupDNsIterator.next();
			groupAttributes = connection.getLdapContext()
					.getAttributes(groupDN);
			result.put(groupDN, extractMembers(groupAttributes));
		}

		return result;
	}

	/**
	 * <p>
	 * Extracts the DNs for members of the group with the given 
	 * LDAP context attributes. This is achieved by extracting all the values
	 * of the LDAP attribute, with name equivalent to the field value
	 * {@link #memberAttribute}, from the attributes collection.
	 * </p>
	 * 
	 * @param groupAttributes	The attributes taken from the group's LDAP context.
	 * @return	A collection of distinguished-names for the users belonging to 
	 * the group with the specified attributes.
	 * @throws NamingException	Propagated from underlying LDAP communication layer.
	 */
	private Collection<String> extractMembers(Attributes groupAttributes)
			throws NamingException {
		Collection<String> result = new ArrayList<String>();
		Attribute members = groupAttributes.get(memberAttribute);
		NamingEnumeration<?> memberDNs = members.getAll();

		while (memberDNs.hasMore())
			result.add(memberDNs.next().toString());

		return result;
	}
}
