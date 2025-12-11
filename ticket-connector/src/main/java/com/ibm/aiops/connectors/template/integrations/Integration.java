/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/
package com.ibm.aiops.connectors.template.integrations;

import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;
import lombok.Getter;

/**
 * The base class for all Integrations (Slack, Email, etc.) which defines the base functionality all Integrations are
 * expected to define.
 */
public abstract class Integration {
    @Getter
    private String name;
    @Getter
    protected boolean verified;

    public Integration(String name) {
        // regex to replace spaces with underscore
        this.name = name.toLowerCase().replaceAll("\\s+", "_");
    }

    /**
     * Queries the issues GitHub API
     *
     * @param url
     *            the URL including query paramaters
     *
     * @return the string representation of the response body
     *
     * @throws ConnectorActionException
     */
    public abstract HttpResponse<String> getIssues(String url) throws ConnectorActionException;

    /**
     * Queries the comments for an issue
     *
     * @param queryParam
     *
     * @return
     *
     * @throws ConnectorActionException
     */
    public abstract String getComments(String commentsURL, String queryParam) throws ConnectorActionException;

    /**
     * Verify the integration is properly configured and ready to send notifications.
     *
     * @throws ConnectorActionException
     *             if the integration is improperly setup.
     */
    public abstract ObjectNode createIssue(ObjectNode requestData, String jsonata) throws ConnectorActionException;

    /**
     * Verify the integration is properly configured and ready to send notifications.
     *
     * @throws ConnectorActionException
     *             if the integration is improperly setup.
     */
    public abstract ObjectNode updateIssue(ObjectNode requestData, String jsonata, String issueNum, String state)
            throws ConnectorActionException;

    /**
     * Verify the integration is properly configured and ready to send notifications.
     *
     * @throws ConnectorActionException
     *             if the integration is improperly setup.
     */
    public abstract void verifyIntegration() throws ConnectorActionException;

    @Override
    public String toString() {
        return "Integration: " + getName();
    }
}
