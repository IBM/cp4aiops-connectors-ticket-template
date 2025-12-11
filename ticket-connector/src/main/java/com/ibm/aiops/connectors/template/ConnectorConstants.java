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
 *      (C) Copyright IBM Corp. 2024
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

    public static final String ISSUE_GITHUB = "/issues";
    public static final String COMMENTS_GITHUB = "comments";

    static final String TOPIC_INPUT_LIFECYCLE_EVENTS = "cp4waiops-cartridge.lifecycle.input.events";

    // Self identifier
    public static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/github");
    public static final String TICKET_TYPE = "github";
    static final String TOOL_TYPE_TICKET = "com.ibm.type.ticket." + TICKET_TYPE;

    static final String ISSUE_POLL = "com.ibm.type.ticket.github.issue.poll";
    static final String ISSUE_CREATE = "com.ibm.type.ticket.github.issue.create";
    static final String ISSUE_UPDATE = "com.ibm.type.ticket.github.issue.update";
    static final String ISSUE_CLOSE = "com.ibm.type.ticket.jira.issue.close";

    // prometheus counter names
    static final String METRIC_PREFIX = "ibm.aiops.";
    static final String ACTION_ISSUE_POLL_COUNTER = METRIC_PREFIX + "ticket.github.issue.poll.action";
    static final String ACTION_ISSUE_POLL_ERROR_COUNTER = ACTION_ISSUE_POLL_COUNTER + ".error";
    static final String ACTION_ISSUE_POLL_SUCCESS_COUNTER = ACTION_ISSUE_POLL_COUNTER + ".success";
    static final String ACTION_ISSUE_CREATE_COUNTER = METRIC_PREFIX + "ticket.github.issue.create.action";
    static final String ACTION_ISSUE_CREATE_ERROR_COUNTER = ACTION_ISSUE_CREATE_COUNTER + ".error";
    static final String ACTION_ISSUE_CREATE_SUCCESS_COUNTER = ACTION_ISSUE_CREATE_COUNTER + ".success";
    static final String ACTION_ISSUE_UPDATE_COUNTER = METRIC_PREFIX + "ticket.github.issue.update.action";
    static final String ACTION_ISSUE_UPDATE_ERROR_COUNTER = ACTION_ISSUE_UPDATE_COUNTER + ".error";
    static final String ACTION_ISSUE_UPDATE_SUCCESS_COUNTER = ACTION_ISSUE_UPDATE_COUNTER + ".success";

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String PLAIN_CONTENT_TYPE = "text/plain";
    public static final String STANDARD_TENANT_ID = "cfd95b7e-3bc7-4006-a4a8-a73a79c71255";

    static final String INSTANCE_FORBIDDEN_CE_TYPE = "com.ibm.type.ticket.github.forbidden.error";
    static final String INSTANCE_UNAUTHENTICATED_CE_TYPE = "com.ibm.type.ticket.github.unauthenticated.error";
    static final String INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE = "com.ibm.type.ticket.github.historical.datacollection.information";

    static final List<String> ALERT_TYPES_LIST = Arrays.asList(INSTANCE_FORBIDDEN_CE_TYPE,
            INSTANCE_UNAUTHENTICATED_CE_TYPE, INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE);
}
