/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2023
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template.model;

import lombok.Data;
import lombok.ToString;

/**
 * The model that represents the ConnectorConfiguration. If you have more properties to add to your connector's
 * configuration, add it here and ensure it is defined in your BundleManifest's schema
 */

// Todo: If connector schema is modified with new form varibales add them here. Remove the ones that are no longer
// required.
@Data
@ToString(exclude = "password")
public class Configuration {
    protected boolean data_flow = true;
    protected String[] datasource_type = { "tickets" };
    // The historical start time since the epoch to begin collecting
    protected long start = 0;
    // The historical end time since the epoch to end collecting
    protected long end = 0;
    protected String username;
    protected String password;
    protected String owner;
    protected String repo;
    protected String url;
    protected String collectionMode;
    protected int issueSamplingRate;
    protected String mappings;
    protected String description;
}
