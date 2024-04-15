/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template.model;

import com.ibm.aiops.connectors.template.Utils;

import lombok.Data;
import lombok.ToString;

/**
 * The model that represents the ConnectorConfiguration. If you have more properties to add to your connector's
 * configuration, add it here and ensure it is defined in your BundleManifest's schema
 */
@Data
@ToString(exclude = "token")
public class Configuration {
    protected boolean data_flow = true;
    // The historical start time since the epoch to begin collecting
    protected long start = 0;
    protected String token;
    protected String owner;
    protected String repo;
    protected String url;
    protected String collectionMode;
    protected int issueSamplingRate;
    protected String mappingsGithub;
    protected String description;
    protected String username;

    public String getToken() {
        return Utils.encode(token);
    }

    
}
