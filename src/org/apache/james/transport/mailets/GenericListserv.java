/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

/**
 * An abstract implementation of a listserv.  The underlying implementation must define
 * various settings, and can vary in their individual configuration.  Supports restricting
 * to members only, allowing attachments or not, sending replies back to the list, and an
 * optional subject prefix.
 */
public abstract class GenericListserv extends GenericMailet {

    /**
     * Returns a Collection of MailAddress objects of members to receive this email
     */
    public abstract Collection getMembers() throws ParseException;

    /**
     * Returns whether this list should restrict to senders only
     */
    public abstract boolean isMembersOnly();

    /**
     * Returns whether this listserv allow attachments
     */
    public abstract boolean isAttachmentsAllowed();

    /**
     * Returns whether listserv should add reply-to header
     */
    public abstract boolean isReplyToList();

    /**
     * The email address that this listserv processes on
     */
    public abstract MailAddress getListservAddress() throws ParseException;

    /**
     * An optional subject prefix which will be surrounded by [].
     */
    public abstract String getSubjectPrefix();

    /**
     * Processes the message.  Assumes it is the only recipient of this forked message.
     */
    public final void service(Mail mail) throws MailetException, MessagingException {
        try {
            Collection members = new Vector();
            members.addAll(getMembers());

            //Check for members only flag....
            if (isMembersOnly() && !members.contains(mail.getSender())) {
                //Need to bounce the message to say they can't send to this list
                getMailetContext().bounce(mail, "Only members of this listserv are allowed to send a message to this address.");
                mail.setState(Mail.GHOST);
                return;
            }

            //Check for no attachments
            if (!isAttachmentsAllowed() && mail.getMessage().getContent() instanceof MimeMultipart) {
                getMailetContext().bounce(mail, "You cannot send attachments to this listserv.");
                mail.setState(Mail.GHOST);
                return;
            }

            MimeMessage message = mail.getMessage();

            //Set the subject if set
            if (getSubjectPrefix() != null) {
                String prefix = "[" + getSubjectPrefix() + "]";
                String subj = message.getSubject();
                //If the "prefix" is in the subject line, remove it and everything before it
                int index = subj.indexOf(prefix);
                if (index > -1) {
                    if (index == 0) {
                        subj = prefix + ' ' + subj.substring(index + prefix.length() + 1);
                    } else {
                        subj = prefix + ' ' + subj.substring(0, index) + subj.substring(index + prefix.length() + 1);
                    }
                } else {
                    subj = prefix + ' ' + subj;
                }

                message.setSubject(subj);
            }
            if (isReplyToList()) {
                message.setHeader("Reply-To", getListservAddress().toString());
            }

            //Send the message to the list members
            getMailetContext().sendMail(getListservAddress(), members, message);

            //Kill the old message
            mail.setState(Mail.GHOST);
        } catch (IOException ioe) {
            throw new MailetException(ioe);
        }
    }
}
