/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.transport.matchers;

import org.apache.james.util.NetMatcher;
import javax.mail.MessagingException;
import java.util.StringTokenizer;
import java.util.Collection;

/**
  * AbstractNetworkMatcher makes writing IP Address matchers easier.
  *
  * AbstractNetworkMatcher provides a means for checking to see whether
  * a particular IP address or domain is within a set of subnets
  * These subnets may be expressed in one of several formats:
  * 
  *     Format                          Example
  *     explicit address                127.0.0.1
  *     address with a wildcard         127.0.0.*
  *     domain name                     myHost.com
  *     domain name + prefix-length     myHost.com/24
  *     domain name + mask              myHost.com/255.255.255.0
  *     IP address + prefix-length      127.0.0.0/8
  *     IP + mask                       127.0.0.0/255.0.0.0
  *
  * For more information, see also: RFC 1518 and RFC 1519.
  * 
  * @version $ID$
  */
public abstract class AbstractNetworkMatcher extends org.apache.mailet.GenericMatcher {

    /**
     * This is a Network Matcher that should be configured to contain
     * authorized networks
     */
    private NetMatcher authorizedNetworks = null;

    public void init() throws MessagingException {
        Collection nets = allowedNetworks();
        if (nets != null) {
            authorizedNetworks = new NetMatcher() {
                protected void log(String s) {
                    AbstractNetworkMatcher.this.log(s);
                }
            };
            authorizedNetworks.initInetNetworks(allowedNetworks());
            log("Authorized addresses: " + authorizedNetworks.toString());
        }
    }

    protected Collection allowedNetworks() {
        Collection networks = null;
        if (getCondition() != null) {
            StringTokenizer st = new StringTokenizer(getCondition(), ", ", false);
            networks = new java.util.ArrayList();
            while (st.hasMoreTokens()) networks.add(st.nextToken());
        }
        return networks;
    }

    protected boolean matchNetwork(java.net.InetAddress addr) {
        return authorizedNetworks == null ? false : authorizedNetworks.matchInetNetwork(addr);
    }

    protected boolean matchNetwork(String addr) {
        return authorizedNetworks == null ? false : authorizedNetworks.matchInetNetwork(addr);
    }
}
