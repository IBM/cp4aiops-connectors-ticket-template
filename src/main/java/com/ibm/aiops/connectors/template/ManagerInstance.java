/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2023
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template;

import jakarta.enterprise.context.ApplicationScoped;

import com.ibm.cp4waiops.connectors.sdk.ConnectorManager;
import com.ibm.cp4waiops.connectors.sdk.StandardConnectorManager;

@ApplicationScoped
public class ManagerInstance {
    private ConnectorManager manager = new StandardConnectorManager(new TicketConnectorFactory());

    public ConnectorManager getConnectorManager() {
        return manager;
    }
}
