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

    static final String TOPIC_INPUT_LIFECYCLE_EVENTS = "cp4waiops-cartridge.lifecycle.input.events";

    // Self identifier
    public static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/github-sample");
    public static final String TICKET_TYPE = "github";
    static final String TOOL_TYPE_TICKET = "com.ibm.type.ticket." + TICKET_TYPE;

    static final String ISSUE_POLL = "com.ibm.type.ticket.github.issue.poll";
    static final String ISSUE_CREATE = "com.ibm.type.ticket.github.issue.create";
    static final String ISSUE_UPDATE = "com.ibm.type.ticket.github.issue.update";

    static final String ACTION_ISSUE_POLL_COUNTER = "ticket.github.issue.poll.action";
    static final String ACTION_ISSUE_POLL_ERROR_COUNTER = "ticket.github.issue.poll.action.error";
    static final String ACTION_ISSUE_CREATE_COUNTER = "ticket.github.issue.create.action.action";
    static final String ACTION_ISSUE_CREATE_ERROR_COUNTER = "ticket.github.issue.create.action.error";
    static final String ACTION_ISSUE_UPDATE_COUNTER = "ticket.github.issue.update";
    static final String ACTION_ISSUE_UPDATE_ERROR_COUNTER = "ticket.github.issue.update.error";

    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String PLAIN_CONTENT_TYPE = "text/plain";
    public static final String STANDARD_TENANT_ID = "cfd95b7e-3bc7-4006-a4a8-a73a79c71255";

    static final String INSTANCE_FORBIDDEN_CE_TYPE = "com.ibm.type.ticket.github.forbidden.error";
    static final String INSTANCE_UNAUTHENTICATED_CE_TYPE = "com.ibm.type.ticket.github.unauthenticated.error";

    static final List<String> ALERT_TYPES_LIST = Arrays.asList(INSTANCE_FORBIDDEN_CE_TYPE,
            INSTANCE_UNAUTHENTICATED_CE_TYPE);
}