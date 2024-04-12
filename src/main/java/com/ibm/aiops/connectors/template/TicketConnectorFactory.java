/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template;

import com.ibm.cp4waiops.connectors.sdk.Connector;
import com.ibm.cp4waiops.connectors.sdk.ConnectorFactory;

public class TicketConnectorFactory implements ConnectorFactory {

    public TicketConnectorFactory() {
    }

    @Override
    public String GetConnectorName() {
        return "github-grpc-connector-template";
    }

    @Override
    public String GetComponentName() {
        return "connector";
    }

    @Override
    public Connector Create() {
        return new TicketConnector();
    }

}
