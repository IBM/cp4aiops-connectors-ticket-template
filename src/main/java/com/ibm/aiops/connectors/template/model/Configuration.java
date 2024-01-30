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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The model that represents the ConnectorConfiguration. If you have more properties to add to your connector's
 * configuration, add it here and ensure it is defined in your BundleManifest's schema
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Configuration {
    @JsonProperty("data_flow")
    protected boolean dataFlow = true;
    // The historical start time since the epoch to begin collecting
    protected long start = 0;
    // The historical end time since the epoch to end collecting
    protected long end = 0;
    protected Map<String, String> mapping;
    protected String password;
    protected String username;
    protected String url;
    // A comma seperated list of the ticket types to query from the connector
    protected String types;
    @JsonProperty("collection_mode")
    protected String collectionMode;

    public Configuration() {
    }

    public boolean getDataFlow() {
        return dataFlow;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getUrl() {
        return url;
    }

    public String getTypes() {
        return types;
    }

    public Map<String, String> getMapping() {
        return mapping;
    }

    public String getCollectionMode() {
        return collectionMode;
    }
}
