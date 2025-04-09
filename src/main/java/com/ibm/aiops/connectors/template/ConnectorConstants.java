/*
 *
 * IBM Confidential
 * OCO Source Materials
 *
 * 5737-M96
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

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class ConnectorConstants {
    // Ticket source type
    static final String HISTORICAL = "historical";
    static final String LIVE = "live";

    // Todo: this could be anything like github jira etc.
    public static final String TICKET_TYPE = "ticket-template";
    
    static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/" + TICKET_TYPE);
    
    // Prefix shouldn't be changed. This helps in mapping the ticketing system to AIOps.
    static final String TOOL_TYPE_TICKET = "com.ibm.type.ticket." + TICKET_TYPE; 

    // Actions
    static final String ISSUE_POLL = "com.ibm.type.ticket." + TICKET_TYPE + ".issue.poll";
    static final String ISSUE_CREATE = "com.ibm.type.ticket." + TICKET_TYPE + ".issue.create";
    static final String ISSUE_UPDATE = "com.ibm.type.ticket." + TICKET_TYPE + ".issue.update";
    static final String ISSUE_CLOSE = "com.ibm.type.ticket." + TICKET_TYPE + ".issue.close";

    // Metrics
    static final String ACTION_ISSUE_POLL_COUNTER = "ticket." + TICKET_TYPE + ".issue.poll.action";
    static final String ACTION_ISSUE_POLL_ERROR_COUNTER = "ticket." + TICKET_TYPE + ".issue.poll.action.error";
    static final String ACTION_ISSUE_ACTIONS_COUNTER = "ticket." + TICKET_TYPE + ".issue.action.action";
    static final String ACTION_ISSUE_ACTIONS_ERROR_COUNTER = "ticket." + TICKET_TYPE + ".issue.action.error";

    // Topic to produce alert create event
    static final String TOPIC_INPUT_LIFECYCLE_EVENTS = "cp4waiops-cartridge.lifecycle.input.events";
    // Example of Cloud Event type for Alert creation.
    static final String INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE = "com.ibm.type.ticket." + TICKET_TYPE + ".historical.datacollection.information";

    static final List<String> ALERT_TYPES_LIST = Arrays.asList(INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE);
}