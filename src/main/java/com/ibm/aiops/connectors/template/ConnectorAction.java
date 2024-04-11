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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.template.model.Configuration;

import io.micrometer.core.instrument.Counter;

public class ConnectorAction {
    String actionType;
    Configuration configuration;
    TicketConnector connector;
    Counter actionCounter;
    Counter actionErrorCounter;
    // actionConfig is not always initialied
    ObjectNode request = null;
    String jsonnata = null;

    public ConnectorAction(String actionType, Configuration configuration, TicketConnector connector,
            Counter actionCounter, Counter actionErrorCounter) {
        this.actionType = actionType;
        this.configuration = configuration;
        this.connector = connector;
        this.actionCounter = actionCounter;
        this.actionErrorCounter = actionErrorCounter;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Action: " + actionType + " with configuration: " + configuration.toString());

        return sb.toString();
    }

    public String getActionType() {
        return actionType;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public TicketConnector getConnector() {
        return connector;
    }

    public Counter getActionCounter() {
        return actionCounter;
    }

    public Counter getActionErrorCounter() {
        return actionErrorCounter;
    }

    public ObjectNode getRequest() {
        return request;
    }

    public String getJsonata() {
        return jsonnata;
    }
}