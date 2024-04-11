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

import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The IntegrationManager will manage the configured integrations which are available to create issues
 */
public class IntegrationManager {
    private static final Logger logger = Logger.getLogger(IntegrationManager.class.getName());

    private List<Integration> configuredIntegrations;

    public IntegrationManager() {
        this.configuredIntegrations = new ArrayList<Integration>();
    }

    /**
     * Add an integration to be managed
     *
     * @param integration
     *            the integration to be managed
     *
     * @throws ConnectorActionException
     *             if the integration failed to connect to github and get issues
     */
    public void registerIntegration(Integration integration) throws ConnectorActionException {
        integration.verifyIntegration();
        configuredIntegrations.add(integration);
        logger.log(Level.INFO, "Registered Integration: " + integration.getName());
    }

    /**
     * Get a list of all active integrations (ones that are successfully configured)
     *
     * @return a list of all active integrations (ones that are successfully configured)
     */
    public CompletableFuture<List<Integration>> getActiveIntegrations() {
        CompletableFuture<List<Integration>> activeIntegrations = new CompletableFuture<>();

        List<Integration> succesfullyVerified = new ArrayList<>();
        for (Integration integration : configuredIntegrations) {
            try {
                integration.verifyIntegration();
                succesfullyVerified.add(integration);
            } catch (ConnectorActionException e) {
                e.printStackTrace();
            }
        }
        activeIntegrations.complete(succesfullyVerified);

        return activeIntegrations;
    }

    /**
     * Find the integration with the given name
     *
     * @param name
     *            the name of the integration to find
     *
     * @return the integration with the given name, or null if it doesn't exist (TODO: Update to throw error?)
     */
    public Integration findIntegration(String name) {
        for (Integration integration : configuredIntegrations) {
            if (integration.getName().equalsIgnoreCase(name)) {
                return integration;
            }
        }
        return null;
    }

    /**
     * For new use case, one-to-one architecture, gain access to the single integration managed.
     *
     * @return the only integration being managed by the integration manager, otherwise null.
     */
    public Integration getIntegration() {
        if (configuredIntegrations.size() == 0) {
            logger.log(Level.SEVERE, "No integrations found!");
            return null;
        }
        if (configuredIntegrations.size() != 1) {
            logger.log(Level.SEVERE, "Found more than one integration registered! Returning the latest");
            return configuredIntegrations.get(configuredIntegrations.size() - 1);
        }
        return configuredIntegrations.get(0);
    }
}
