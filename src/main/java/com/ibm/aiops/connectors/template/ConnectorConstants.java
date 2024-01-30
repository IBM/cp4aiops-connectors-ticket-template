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

public class ConnectorConstants {
    // Ticket source type
    static final String HISTORICAL = "historical";
    static final String LIVE = "live";

    // Self identifier
    static final URI SELF_SOURCE = URI.create("template.connectors.aiops.ibm.com/ticket-system");
}