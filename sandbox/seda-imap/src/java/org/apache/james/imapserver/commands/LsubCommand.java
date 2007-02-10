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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.ListResult;

/**
 * @version $Revision: 109034 $
 */
class LsubCommand extends ListCommand
{
    public static final String NAME = "LSUB";



    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }
    
    
    
    protected ListCommandMessage createMessage(String referenceName, String mailboxPattern, String tag) {
        return new LsubListCommandMessage(this, referenceName, mailboxPattern, tag);
    }

    private static class LsubListCommandMessage extends ListCommandMessage 
    {
        public LsubListCommandMessage(ImapCommand command, String referenceName, String mailboxPattern, String tag) {
            super(command, referenceName, mailboxPattern, tag);
        }

        protected ListResult[] doList( ImapSession session, String base, String pattern )  throws MailboxException
        {
            return doList(session,base,pattern,true);
        }
    }
}
