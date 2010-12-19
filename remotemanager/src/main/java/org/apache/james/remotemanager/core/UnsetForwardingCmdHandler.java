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

package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;

import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;

/**
 * Handler called upon receipt of an UNSETFORWARDING command.
 *
 */
public class UnsetForwardingCmdHandler implements CommandHandler{
    private final static String COMMAND_NAME = "UNSETFORWARDING";
    private CommandHelp help = new CommandHelp("unsetforwarding [username]","removes a forward");

    private UsersRepository users;

    /**
     * Sets the users repository.
     * 
     * @param users
     *            the users to set
     */
    @Resource(name = "usersrepository")
    public final void setUsers(UsersRepository users) {
        this.users = users;
    }
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandler#onCommand(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Request)
     */
    public Response onCommand(RemoteManagerSession session, Request request) {
        RemoteManagerResponse response;
        String parameters = request.getArgument();
        if ((parameters == null) || (parameters.equals(""))) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }
        String username = parameters;
        JamesUser user = (JamesUser) users.getUserByName(username);
        if (user == null) {
            response = new RemoteManagerResponse("No such user " + username);
        } else if (user.getForwarding()){
            user.setForwarding(false);
            users.updateUser(user);
            StringBuilder responseBuffer =
                new StringBuilder(64)
                        .append("Forward for ")
                        .append(username)
                        .append(" unset");
            String responseString = responseBuffer.toString();

            session.getLogger().info(responseString);
            response = new RemoteManagerResponse(responseString);
        } else {
            response = new RemoteManagerResponse("Forwarding not active for" + username);
        }
        return response;
    }


    /**
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}