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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.aiops.connectors.template.integrations.GithubIntegration;
import com.ibm.aiops.connectors.template.integrations.Integration;
import com.ibm.aiops.connectors.template.integrations.IntegrationManager;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionConnectorSettings;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionRequest;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionResult;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;
import com.ibm.cp4waiops.connectors.sdk.notifications.NotificationConnectorBase;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public class TicketConnector extends NotificationConnectorBase {
    static final Logger logger = Logger.getLogger(TicketConnector.class.getName());

    // default to 1 hour
    private static final int HISTORICAL_DATACOLLECTION_ALERT_EXPIRY_SECONDS = Integer
            .parseInt(System.getenv().getOrDefault("HISTORICAL_DATACOLLECTION_ALERT_EXPIRY_SECONDS", "3600"));

    protected AtomicReference<Configuration> _configuration;
    protected AtomicReference<IntegrationManager> _integrationManager;
    protected ConcurrentLinkedQueue<ConnectorAction> actionQueue = new ConcurrentLinkedQueue<ConnectorAction>();
    protected AtomicReference<HttpClientUtil> httpClient = new AtomicReference<HttpClientUtil>();
    protected AtomicBoolean _configured = new AtomicBoolean(false);

    protected ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    protected int sleepInterval = 1;
    protected long timeLastCleared = 0L;

    private Counter _issuePollingActionCounter;
    private Counter _issuePollingActionErrorCounter;
    private Counter _issuePollingActionSuccessCounter;
    private Counter _issueCreationActionCounter;
    private Counter _issueCreationActionErrorCounter;
    private Counter _issueCreationActionSuccessCounter;
    private Counter _issueUpdatingActionCounter;
    private Counter _issueUpdatingActionErrorCounter;
    private Counter _issueUpdatingActionSuccessCounter;

    private ConnectorAction issuePollingAction;
    private IssuePollingAction issuePollingInstance;

    protected AtomicLong _lastStatus;
    protected String _systemName;

    public TicketConnector() {
        _configuration = new AtomicReference<>();
        _integrationManager = new AtomicReference<>();
        _lastStatus = new AtomicLong(0);
        _systemName = "";
    }

    @Override
    public void registerMetrics(MeterRegistry metricRegistry) {
        super.registerMetrics(metricRegistry);

        _issuePollingActionCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_POLL_COUNTER)
                .description("Counter that tracks the number of Github poll actions.").register(metricRegistry);
        _issuePollingActionSuccessCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_POLL_SUCCESS_COUNTER)
                .description("Counter that tracks the number of Github poll actions that resulted in success.")
                .register(metricRegistry);
        _issuePollingActionErrorCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_POLL_ERROR_COUNTER)
                .description("Counter that tracks the number of Github poll actions that resulted in error.")
                .register(metricRegistry);

        _issueCreationActionCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_CREATE_COUNTER)
                .description("Counter that tracks the number of Github incident create actions.")
                .register(metricRegistry);
        _issueCreationActionSuccessCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_CREATE_SUCCESS_COUNTER)
                .description(
                        "Counter that tracks the number of Github incident create actions that resulted in success.")
                .register(metricRegistry);
        _issueCreationActionErrorCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_CREATE_ERROR_COUNTER)
                .description("Counter that tracks the number of Github incident create actions that resulted in error.")
                .register(metricRegistry);

        _issueUpdatingActionCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_UPDATE_COUNTER)
                .description("Counter that tracks the number of Github incident update actions.")
                .register(metricRegistry);
        _issueUpdatingActionSuccessCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_UPDATE_SUCCESS_COUNTER)
                .description(
                        "Counter that tracks the number of Github incident update actions that resulted in success.")
                .register(metricRegistry);
        _issueUpdatingActionErrorCounter = Counter.builder(ConnectorConstants.ACTION_ISSUE_UPDATE_ERROR_COUNTER)
                .description("Counter that tracks the number of Github incident update actions that resulted in error.")
                .register(metricRegistry);
    }

    @Override
    public ActionConnectorSettings onConfigure(ConnectorConfigurationHelper config)
            throws ConnectorException, ConnectorActionException {
        Configuration newConfiguration = config.getDataObject(Configuration.class);

        this._systemName = config.getSystemName();

        if (newConfiguration == null) {
            throw new ConnectorException("no configuration provided");
        }

        buildHttpClient(_configuration.get(), newConfiguration);
        try {
            // Now we have the newly created "hello" property in the model, you can perform actions on this
            // such as determining if hasConnectionCreateCfgChanged should result in a change
            // TODO: remove this temporary workaround
            if (!_configured.get() || hasConnectionCreateCfgChanged(_configuration.get(), newConfiguration)) {
                logger.log(Level.INFO, "Configuring ConnectionId: " + config.getConnectionID());
                this._configuration.set(newConfiguration);

                logger.log(Level.INFO, "Configuring Integration", this._configuration.get());

                this._integrationManager.set(new IntegrationManager());

                GithubIntegration githubIntegration = new GithubIntegration(httpClient.get(), this);

                this._integrationManager.get().registerIntegration(githubIntegration);

                logger.log(Level.INFO, "Integration Configured");

                _configured.set(true);
                collectData(newConfiguration);
                emitStatus(ConnectorStatus.Phase.Running, Duration.ofMinutes(5));
            }
        } catch (Exception exception) {
            logger.log(Level.INFO, "Exception occurred while configuring", exception.getMessage());
            emitStatus(ConnectorStatus.Phase.Errored, Duration.ofMinutes(5));
        }

        return ActionConnectorSettings.builder().sourceUri(ConnectorConstants.SELF_SOURCE).build();
        // return settings;
    }

    protected boolean hasConnectionCreateCfgChanged(Configuration oldConfig, Configuration newConfig) {
        // For example, if we care about the "hello" property and want to make a change, we compare
        // the new and old property to see if anything has changed
        if (oldConfig != null && booleanEqual(oldConfig.isData_flow(), newConfig.isData_flow())
                && stringsEqual(oldConfig.getCollectionMode(), newConfig.getCollectionMode())
                && stringsEqual(oldConfig.getOwner(), newConfig.getOwner())
                && stringsEqual(oldConfig.getRepo(), newConfig.getRepo())
                && stringsEqual(oldConfig.getToken(), newConfig.getToken())
                && stringsEqual(oldConfig.getUrl(), newConfig.getUrl())
                && stringsEqual(oldConfig.getUsername(), newConfig.getUsername())
                && stringsEqual(oldConfig.getHello(), newConfig.getHello())
                && stringsEqual(oldConfig.getMappingsGithub(), newConfig.getMappingsGithub())
                && (oldConfig.getIssueSamplingRate() == newConfig.getIssueSamplingRate())
                && (oldConfig.getStart() == newConfig.getStart())) {
            logger.log(Level.INFO, "hasConnectionCreateCfgChanged(): configuration has not changed");
            return false;
        }
        logger.log(Level.INFO, "hasConnectionCreateCfgChanged(): configuration has changed");
        return true;
    }

    void collectData(Configuration connectionCreateCfg) {

        logger.log(Level.INFO, "collectData(): Stopping existing polling");

        stopPolling();

        // collect data if data flow is enable
        if (booleanEqual(connectionCreateCfg.isData_flow(), true)) {
            logger.log(Level.INFO, "Data flow is on");
            issuePollingAction = new ConnectorAction(ConnectorConstants.ISSUE_POLL, connectionCreateCfg, this,
                    _issuePollingActionCounter, _issuePollingActionSuccessCounter, _issuePollingActionErrorCounter,
                    null);
            addActionToQueue(issuePollingAction);
        }
    }

    protected boolean stringsEqual(String a, String b) {
        if (a == null || b == null)
            return a == b;
        return a.equals(b);
    }

    protected boolean booleanEqual(Boolean a, Boolean b) {
        if (a == null || b == null)
            return a == b;
        return a.equals(b);
    }

    protected void buildHttpClient(Configuration oldConfig, Configuration newConfig) throws ConnectorException {
        // Skip if no change was made
        if (httpClient.get() != null && oldConfig != null && stringsEqual(oldConfig.getUrl(), newConfig.getUrl())
                && stringsEqual(oldConfig.getOwner(), newConfig.getOwner())
                && stringsEqual(oldConfig.getRepo(), newConfig.getRepo())
                && stringsEqual(oldConfig.getToken(), newConfig.getToken())) {
            return;
        }

        logger.log(Level.INFO, "Building http client");

        // Build client
        try {
            HttpClientUtil client = new HttpClientUtil(newConfig.getUrl(), newConfig.getOwner(), newConfig.getRepo(),
                    newConfig.getToken());
            httpClient.set(client);
            logger.log(Level.INFO, "Http client created");
        } catch (Exception error) {
            throw new ConnectorException("Failed to client http client", error);
        }

    }

    protected Integration getCurrentIntegration() {
        // Query integrations and utilize first (and only) one available. If needed in
        // future we can
        // find the proper integration if we revert to one-to-many connector to
        // integrations model

        Integration integration = this._integrationManager.get().getIntegration();
        if (integration == null) {
            throw new ConnectorActionException("Specified integration is not properly configured", 500);
        }
        // ensure it's verified
        if (!integration.isVerified()) {
            // try one more time, if still unverified, stop
            integration.verifyIntegration();
            if (!integration.isVerified()) {
                throw new ConnectorActionException("Specified integration failed to be verified", 500);
            }
        }
        return integration;
    }

    private void stopPolling() {
        // Stop old polling
        if (issuePollingInstance != null) {
            logger.log(Level.INFO, "stopping problem polling");
            issuePollingInstance.stop();
        }
    }

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
        logger.log(Level.INFO, "Terminating");

        stopPolling();
        clearAlerts(ConnectorConstants.ALERT_TYPES_LIST);
    }

    public void clearAlerts(List<String> alertTypes) {
        CloudEvent ce;
        for (String alertType : alertTypes) {
            try {
                ce = createAlertEvent(alertType, EventLifeCycleEvent.EVENT_TYPE_RESOLUTION, "", 0);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
            } catch (JsonProcessingException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }

    // A helper function that checks github instantiation before adding action to
    // queue
    private void addActionToQueue(ConnectorAction action) {
        actionQueue.add(action);
        logger.log(Level.INFO, "Action was successfully added");
    }

    protected ConcurrentLinkedQueue<ConnectorAction> getActionQueue() {
        return actionQueue;
    }

    protected void processNextAction() {
        ConnectorAction currentAction = actionQueue.poll();
        if (currentAction != null) {
            logger.log(Level.INFO, currentAction.toString());
            try {
                Runnable action = ConnectorActionFactory.getRunnableAction(currentAction);
                if (action instanceof IssuePollingAction) {
                    issuePollingInstance = (IssuePollingAction) action;
                }
                executor.execute(action);
            } catch (RejectedExecutionException ex) {
                logger.log(Level.INFO, "Rejected Execution Exception occurred", ex.getMessage());
            } catch (NullPointerException ex) {
                logger.log(Level.INFO, "Null Pointer Exception occurred", ex.getMessage());
            }
        }
    }

    @Override
    public void run() {

        // Put monitoring logic in here. For now, this loop keeps the status of the
        // connector as ready.
        // If status is not sent every 5 minutes, the status of the connector will go
        // into an Unknown state
        boolean interrupted = false;
        long statusLastUpdated = 0;
        final long NANOSECONDS_PER_SECOND = 1000000000;
        final long STATUS_UPDATE_PERIOD_S = 300;
        final long LOOP_PERIOD_MS = 1000;

        while (!interrupted) {
            try {
                // Process next action

                // ensure it's verified
                getCurrentIntegration();

                processNextAction();

                // Periodic provide status update
                if ((System.nanoTime() - statusLastUpdated) / NANOSECONDS_PER_SECOND > STATUS_UPDATE_PERIOD_S) {
                    statusLastUpdated = System.nanoTime();
                    logger.log(Level.INFO, "Update Status to running");
                    emitStatus(ConnectorStatus.Phase.Running, Duration.ofMinutes(5));
                }
                // Wait
                Thread.sleep(LOOP_PERIOD_MS);
            } catch (InterruptedException ignored) {
                // Termination of the process has been requested
                interrupted = true;
                logger.log(Level.INFO, "Interrupted Exception occurred");
                emitStatus(ConnectorStatus.Phase.Errored, Duration.ofMinutes(5));
                Thread.currentThread().interrupt();
            } catch (ConnectorActionException exception) {
                logger.log(Level.INFO, "Exception occurred while executing run thread", exception.getMessage());
                emitStatus(ConnectorStatus.Phase.Errored, Duration.ofMinutes(5));
                interrupted = true;
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                logger.log(Level.INFO, "Exception occurred while executing run thread");
                emitStatus(ConnectorStatus.Phase.Errored, Duration.ofMinutes(5));
                interrupted = true;
                Thread.currentThread().interrupt();
            }
        }
    }

    public CloudEvent createAlertEvent(String alertType, String eventType, String summary, int expiryInSeconds)
            throws JsonProcessingException {
        EventLifeCycleEvent elcEvent = newInstanceAlertEvent(alertType, eventType, summary, expiryInSeconds);
        return CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(ConnectorConstants.SELF_SOURCE)
                .withType(alertType).withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
    }

    protected EventLifeCycleEvent newInstanceAlertEvent(String alertType, String eventType, String summary,
            int expiryInSeconds) {
        EventLifeCycleEvent event = new EventLifeCycleEvent();
        EventLifeCycleEvent.Type type = new EventLifeCycleEvent.Type();
        Map<String, String> details = new HashMap<>();

        Map<String, Object> sender = new HashMap<>();
        sender.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "GitHub Integration");
        sender.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName);
        sender.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "GitHub Integration");
        resource.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName);
        resource.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setResource(resource);

        event.setId(UUID.randomUUID().toString());
        event.setOccurrenceTime(Date.from(Instant.now()));
        event.setSeverity(3); // 3 represents warning
        event.setExpirySeconds(expiryInSeconds);

        type.setEventType(eventType);
        type.setClassification("Monitoring GitHub Connector Calls");
        if (!eventType.equals(EventLifeCycleEvent.EVENT_TYPE_RESOLUTION)) {
            event.setSummary(summary);

            if (alertType == ConnectorConstants.INSTANCE_FORBIDDEN_CE_TYPE) {
                type.setCondition("GitHub API Forbidden");
                details.put("guidance", "Increase the GitHub API calls Rate Limit");
            } else if (alertType == ConnectorConstants.INSTANCE_UNAUTHENTICATED_CE_TYPE) {
                type.setCondition("GitHub API Call Unauthenticated");
                details.put("guidance",
                        "Ensure GitHub credentials are valid and increase the GitHub API calls Rate Limit if necessary");
            } else if (alertType == ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE) {
                type.setCondition("Historical data collection is done.");
                details.put("guidance", "Set up Similar Tickets Training");
                event.setSeverity(2); // 2 represents information.
            }
            event.setType(type);
            event.setDetails(details);
        }

        return event;
    }

    public CloudEvent createEvent(long responseTime, String ce_type, String jsonMessage, URI source) {
        // The cloud event being returned needs to be in a structured format
        return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withSource(source)
                .withTime(OffsetDateTime.now()).withType(ce_type).withExtension("responsetime", responseTime)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withExtension("tooltype", ConnectorConstants.TOOL_TYPE_TICKET)
                .withExtension("structuredcontentmode", "true").withData("application/json", jsonMessage.getBytes())
                .build();
    }

    public void triggerAlerts(int responseCode, String summary, long seconds) {
        CloudEvent ce;
        try {
            if (responseCode == 403) {
                ce = createAlertEvent(ConnectorConstants.INSTANCE_FORBIDDEN_CE_TYPE,
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, summary, (int) seconds);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, summary);
            } else if (responseCode == 401) {
                ce = createAlertEvent(ConnectorConstants.INSTANCE_UNAUTHENTICATED_CE_TYPE,
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, summary, (int) seconds);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, "Alert created: " + summary);
            }
            logger.log(Level.INFO, "Sleeping for  " + seconds + " seconds");
            TimeUnit.SECONDS.sleep(seconds);
        } catch (JsonProcessingException | InterruptedException e1) {
            logger.log(Level.SEVERE, e1.getMessage(), e1);
        }
    }

    public void triggerAlerts(String type) {
        CloudEvent ce;
        try {
            if (type.equals(ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE)) {
                ce = createAlertEvent(ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE,
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, "Historical data collection is done",
                        HISTORICAL_DATACOLLECTION_ALERT_EXPIRY_SECONDS);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, "Historical data collection is done.");
            }
        } catch (JsonProcessingException e1) {
            logger.log(Level.SEVERE, e1.getMessage(), e1);
        }
    }

    public long getTimeLastCleared() {
        return timeLastCleared;
    }

    public void setTimeLastCleared(long timeLastCleared) {
        this.timeLastCleared = timeLastCleared;
    }

    public String getPartition() {
        // Generate the partition
        String connectionID = getConnectorID();
        if (connectionID != null && !connectionID.isEmpty()) {
            return "{\"ce-partitionkey\":\"" + connectionID + "\"}";
        }

        // If a partition cannot be created, return null
        // Null is a valid partition and will not throw errors, but
        // can run into unintended consequences from consumerss
        return null;
    }

    @Override
    public CompletableFuture<ActionResult> notifyCreate(ActionRequest request) {
        // This is the trigger for incident creation. The incident create request comes via
        // a Kafka message. We take that message and perform the appropriate action
        logger.log(Level.INFO, "Notify Creation Completable Future", request);

        ConnectorAction connectorAction = new ConnectorAction(ConnectorConstants.ISSUE_CREATE,
                this._configuration.get(), this, _issueCreationActionCounter, _issueCreationActionSuccessCounter,
                _issueCreationActionErrorCounter, request);
        // The action is added to the queue, which eventually calls the IncidentActions class
        addActionToQueue(connectorAction);
        CompletableFuture<ActionResult> result = null;
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJson).build());
        return result;
    }

    @Override
    public CompletableFuture<ActionResult> notifyUpdate(ActionRequest request) {
        // Example of Ignoring updates until we have a better handling mechanism
        logger.log(Level.INFO, "Notify Update Completable Future", request);

        ConnectorAction connectorAction = new ConnectorAction(ConnectorConstants.ISSUE_UPDATE,
                this._configuration.get(), this, _issueUpdatingActionCounter, _issueUpdatingActionSuccessCounter,
                _issueUpdatingActionErrorCounter, request);
        addActionToQueue(connectorAction);
        CompletableFuture<ActionResult> result = null;
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJson).build());
        return result;
    }

    @Override
    public CompletableFuture<ActionResult> notifyClose(ActionRequest request) {
        // Map incoming data bytes to a JSON structure
        logger.log(Level.INFO, "Notify Close Completable Future", request);

        ConnectorAction connectorAction = new ConnectorAction(ConnectorConstants.ISSUE_CLOSE, this._configuration.get(),
                this, _issueUpdatingActionCounter, _issueUpdatingActionSuccessCounter, _issueUpdatingActionErrorCounter,
                request);
        addActionToQueue(connectorAction);
        CompletableFuture<ActionResult> result = null;
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJson).build());
        return result;

    }
}
