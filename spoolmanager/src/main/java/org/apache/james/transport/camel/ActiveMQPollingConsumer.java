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

package org.apache.james.transport.camel;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.ScheduledPollConsumer;

/**
 * Consumer which polls an activemq endpoint with a selector which only selects messages with JAMES_NEXT_DELIVERY header
 * value is smaller then the current time in milliseconds.
 * 
 * 
 * 
 *
 */
public class ActiveMQPollingConsumer extends ScheduledPollConsumer{

    private ConsumerTemplate consumerTemplate;
    private String receiveEndpointUri;
    
    public ActiveMQPollingConsumer(DefaultEndpoint endpoint, Processor processor, ConsumerTemplate consumerTemplate) {
        super(endpoint, processor);
        this.consumerTemplate = consumerTemplate;
        receiveEndpointUri = getEndpoint().getEndpointUri().replace(getEndpoint().getEndpointKey(),"activemq");
 
    }
  
    @Override
    protected void poll() throws Exception {
      
        StringBuffer consumerUri = new StringBuffer();
        consumerUri.append(receiveEndpointUri);
        if (receiveEndpointUri.indexOf("?") > -1) {
            consumerUri.append("&");
        } else {
            consumerUri.append("?");
        }
        consumerUri.append("selector=");
        consumerUri.append(JamesCamelConstants.JAMES_NEXT_DELIVERY);
        consumerUri.append("<");
        consumerUri.append(System.currentTimeMillis());
        
        // process every exchange which is ready. If no exchange is left break the loop
        while(true) {
            Exchange ex = consumerTemplate.receiveNoWait(consumerUri.toString());
            if (ex != null) {
                getProcessor().process(ex);
            } else {
                break;
            }
            
        }
    }

}
