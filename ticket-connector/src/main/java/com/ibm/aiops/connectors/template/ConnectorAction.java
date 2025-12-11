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
import com.ibm.cp4waiops.connectors.sdk.actions.ActionRequest;

import io.micrometer.core.instrument.Counter;

public class ConnectorAction {
    String actionType;
    Configuration configuration;
    TicketConnector connector;
    Counter actionCounter;
    Counter actionSuccessCounter;
    Counter actionErrorCounter;
    // actionConfig is not always initialied
    ObjectNode request = null;
    String jsonnata = null;
    ActionRequest actionRequest;

    public ConnectorAction(String actionType, Configuration configuration, TicketConnector connector,
            Counter actionCounter, Counter actionSuccessCounter, Counter actionErrorCounter, ActionRequest request) {
        this.actionType = actionType;
        this.configuration = configuration;
        this.connector = connector;
        this.actionCounter = actionCounter;
        this.actionSuccessCounter = actionSuccessCounter;
        this.actionErrorCounter = actionErrorCounter;
        this.actionRequest = request;
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

    public Counter getActionSuccessCounter() {
        return actionSuccessCounter;
    }

    public ObjectNode getRequest() {
        return request;
    }

    public String getJsonata() {
        return jsonnata;
    }

    public ActionRequest getActionRequest() {
        return actionRequest;
    }
}