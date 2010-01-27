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



package org.apache.james.api.dnsservice.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.james.api.dnsservice.DNSService;

class InetNetwork
{

    /*
     * Implements network masking, and is compatible with RFC 1518 and
     * RFC 1519, which describe CIDR: Classless Inter-Domain Routing.
     */

    private InetAddress network;
    private InetAddress netmask;
    private DNSService dnsServer;
    
    /**
     * Constructor
     * 
     * @param dnsServer the DNSService to use
     */
    InetNetwork(DNSService dnsServer) {
        this.dnsServer = dnsServer;
    }

    /**
     * Constuctor
     * 
     * @param ip the InetAddress to init the class
     * @param netmask the InetAddress represent the netmask to init the class
     */
    public InetNetwork(InetAddress ip, InetAddress netmask)
    {
        network = maskIP(ip, netmask);
        this.netmask = netmask;
    }

    /**
     * Return true if the network contains the given name
     * 
     * @param name hostname or ipAddress
     * @return true if the network contains the given name
     * @throws java.net.UnknownHostException if the given name can not resolved
     */
    public boolean contains(final String name) throws java.net.UnknownHostException
    {
        return network.equals(maskIP(dnsServer.getByName(name), netmask));
    }

    /**
     * @see #contains(String)
     */
    public boolean contains(final InetAddress ip)
    {
        return network.equals(maskIP(ip, netmask));
    }

    /**
     * Return String represention of this class 
     * 
     * @return string String representation of this class
     */
    public String toString()
    {
        return network.getHostAddress() + "/" + netmask.getHostAddress();
    }

    /**
     * Return hashCode representation of this class
     * 
     * @return hashCode the hashCode representation of this class
     */
    public int hashCode()
    {
        return maskIP(network, netmask).hashCode();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj)
    {
        return (obj != null) && (obj instanceof InetNetwork) &&
                ((((InetNetwork)obj).network.equals(network)) && (((InetNetwork)obj).netmask.equals(netmask)));
    }

    /**
     * Get InetNetwork of the given String
     * 
     * @param netspec the String which is will converted to InetNetwork
     * @return network the InetNetwork
     * @throws java.net.UnknownHostException
     */
    public InetNetwork getFromString(String netspec) throws java.net.UnknownHostException
    {
        if (netspec.endsWith("*")) netspec = normalizeFromAsterisk(netspec);
        else
        {
            int iSlash = netspec.indexOf('/');
            if (iSlash == -1) netspec += "/255.255.255.255";
            else if (netspec.indexOf('.', iSlash) == -1) netspec = normalizeFromCIDR(netspec);
        }

        return new InetNetwork(dnsServer.getByName(netspec.substring(0, netspec.indexOf('/'))),
                               dnsServer.getByName(netspec.substring(netspec.indexOf('/') + 1)));
    }

    /**
     * Return InetAddress generated of the passed argements. Return Null if any errors accour
     * 
     * @param ip the byte[] represent the ip
     * @param mask the byte[] represent the netmask
     * @return inetAddress the InetAddress generated of the passed arguments. 
     */
    public static InetAddress maskIP(final byte[] ip, final byte[] mask)
    {
        try {
            return getByAddress(new byte[] {
            (byte) (mask[0] & ip[0]),
            (byte) (mask[1] & ip[1]),
            (byte) (mask[2] & ip[2]),
            (byte) (mask[3] & ip[3])
        });
    } catch (UnknownHostException e) {
            return null;
    }
    }

    /**
     * @see #maskIP(byte[], byte[])
     */
    public static InetAddress maskIP(final InetAddress ip, final InetAddress mask)
    {
        return maskIP(ip.getAddress(), mask.getAddress());
    }

    /**
     * This converts from an uncommon "wildcard" CIDR format
     * to "address + mask" format:
     * 
     *   *               =>  000.000.000.0/000.000.000.0
     *   xxx.*           =>  xxx.000.000.0/255.000.000.0
     *   xxx.xxx.*       =>  xxx.xxx.000.0/255.255.000.0
     *   xxx.xxx.xxx.*   =>  xxx.xxx.xxx.0/255.255.255.0
     * 
     * @param netspec 
     * @return addrMask the address/mask of the given argument
     */
    static private String normalizeFromAsterisk(final String netspec)
    {
        String[] masks = {  "0.0.0.0/0.0.0.0", "0.0.0/255.0.0.0", "0.0/255.255.0.0", "0/255.255.255.0" };
        char[] srcb = netspec.toCharArray();                
        int octets = 0;
        for (int i = 1; i < netspec.length(); i++) {
            if (srcb[i] == '.') octets++;
        }
        return (octets == 0) ? masks[0] : netspec.substring(0, netspec.length() -1 ).concat(masks[octets]);
    }

    /**
     * RFC 1518, 1519 - Classless Inter-Domain Routing (CIDR)
     * This converts from "prefix + prefix-length" format to
     * "address + mask" format, e.g. from xxx.xxx.xxx.xxx/yy
     * to xxx.xxx.xxx.xxx/yyy.yyy.yyy.yyy.
     * 
     * @param netspec the xxx.xxx.xxx.xxx/yyy format
     * @return addrMask the xxx.xxx.xxx.xxx/yyy.yyy.yyy.yyy format 
     */
    static private String normalizeFromCIDR(final String netspec)
    {
        final int bits = 32 - Integer.parseInt(netspec.substring(netspec.indexOf('/')+1));
        final int mask = (bits == 32) ? 0 : 0xFFFFFFFF - ((1 << bits)-1);

        return netspec.substring(0, netspec.indexOf('/') + 1) +
                Integer.toString(mask >> 24 & 0xFF, 10) + "." +
                Integer.toString(mask >> 16 & 0xFF, 10) + "." +
                Integer.toString(mask >>  8 & 0xFF, 10) + "." +
                Integer.toString(mask >>  0 & 0xFF, 10);
    }

    private static java.lang.reflect.Method getByAddress = null;

    static {
        try {
            Class<?> inetAddressClass = Class.forName("java.net.InetAddress");
            Class<?>[] parameterTypes = { byte[].class };
            getByAddress = inetAddressClass.getMethod("getByAddress", parameterTypes);
        } catch (Exception e) {
            getByAddress = null;
        }
    }

    /**
     * Return InetAddress which represent the given byte[]
     * 
     * @param ip the byte[] represent the ip
     * @return ip the InetAddress generated of the given byte[]
     * @throws java.net.UnknownHostException
     */
    private static InetAddress getByAddress(byte[] ip) throws java.net.UnknownHostException
    {
        InetAddress addr = null;
        if (getByAddress != null) try {
            addr = (InetAddress) getByAddress.invoke(null, new Object[] { ip });
        } catch (IllegalAccessException e) {
        } catch (java.lang.reflect.InvocationTargetException e) {
        }

        if (addr == null) {
            addr = InetAddress.getByName
                   (
                    Integer.toString(ip[0] & 0xFF, 10) + "." +
                    Integer.toString(ip[1] & 0xFF, 10) + "." +
                    Integer.toString(ip[2] & 0xFF, 10) + "." +
                    Integer.toString(ip[3] & 0xFF, 10)
                   );
        }
        return addr;
    }
}