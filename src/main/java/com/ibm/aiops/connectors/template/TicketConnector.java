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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.aiops.connectors.template.integrations.GithubIntegration;
import com.ibm.aiops.connectors.template.integrations.Integration;
import com.ibm.aiops.connectors.template.integrations.IntegrationManager;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.aiops.connectors.template.model.IssueModel;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionConnectorSettings;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionDataDeserializationException;
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
    public static String ACTION_GITHUB_RESPONSE = "cp4waiops-cartridge.itsmincidentresponse";// "cp4waiops-cartridge.githubcreateresponse";

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
    private Counter _issueCreationActionCounter;
    private Counter _issueCreationActionErrorCounter;
    private Counter _issueUpdatingActionCounter;
    private Counter _issueUpdatingActionErrorCounter;

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

        _issuePollingActionCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_POLL_COUNTER);
        _issuePollingActionErrorCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_CREATE_ERROR_COUNTER);
        _issueCreationActionCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_CREATE_COUNTER);
        _issueCreationActionErrorCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_CREATE_ERROR_COUNTER);
        _issueUpdatingActionCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_UPDATE_COUNTER);
        _issueUpdatingActionErrorCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_UPDATE_ERROR_COUNTER);
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

        // TODO: remove this temporary workaround
        if (!_configured.get() || hasConnectionCreateCfgChanged(_configuration.get(), newConfiguration)) {
            logger.log(Level.INFO, "Configuring ConnectionId: " + config.getConnectionID());
            this._configuration.set(newConfiguration);

            logger.log(Level.INFO, "Configuring Integration", this._configuration.get());

            this._integrationManager.set(new IntegrationManager());

            GithubIntegration githubIntegration = new GithubIntegration(httpClient.get(), this);

            this._integrationManager.get().registerIntegration(githubIntegration);

            logger.log(Level.INFO, "Integration Configured");

            collectData(newConfiguration);
            _configured.set(true);
        }
        return ActionConnectorSettings.builder().sourceUri(ConnectorConstants.SELF_SOURCE).build();
        // return settings;
    }

    protected boolean hasConnectionCreateCfgChanged(Configuration oldConfig, Configuration newConfig) {
        if (oldConfig != null && booleanEqual(oldConfig.isData_flow(), newConfig.isData_flow())
                && stringsEqual(oldConfig.getCollectionMode(), newConfig.getCollectionMode())
                && stringsEqual(oldConfig.getOwner(), newConfig.getOwner())
                && stringsEqual(oldConfig.getRepo(), newConfig.getRepo())
                && stringsEqual(oldConfig.getToken(), newConfig.getToken())
                && stringsEqual(oldConfig.getUrl(), newConfig.getUrl())
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

        // Stop old polling
        if (issuePollingInstance != null) {
            logger.log(Level.INFO, "collectData(): stopping problem polling");
            issuePollingInstance.stop();
        }
        // collect data if data flow is enable
        if (booleanEqual(connectionCreateCfg.isData_flow(), true)) {
            logger.log(Level.INFO, "Data flow is on");
            issuePollingAction = new ConnectorAction(ConnectorConstants.ISSUE_POLL, connectionCreateCfg, this,
                    _issuePollingActionCounter, _issuePollingActionErrorCounter);
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

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
        logger.log(Level.INFO, "Terminating");

        // Stop old polling
        if (issuePollingInstance != null) {
            logger.log(Level.INFO, "stopping problem polling");
            issuePollingInstance.stop();
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
                Thread.currentThread().interrupt();
            } catch (Exception exception) {
                logger.log(Level.INFO, "Exception occurred while executing run thread");
            }
        }
    }

    public CloudEvent createAlertEvent(String alertType, String eventType, String summary)
            throws JsonProcessingException {
        EventLifeCycleEvent elcEvent = newInstanceAlertEvent(alertType, summary);
        return CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(ConnectorConstants.SELF_SOURCE)
                .withType(alertType).withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
    }

    protected EventLifeCycleEvent newInstanceAlertEvent(String alertType, String summary) {
        EventLifeCycleEvent event = new EventLifeCycleEvent();
        EventLifeCycleEvent.Type type = new EventLifeCycleEvent.Type();
        Map<String, String> details = new HashMap<>();

        Map<String, Object> sender = new HashMap<>();
        sender.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Github Integration");
        sender.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName);
        sender.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Github Integration");
        resource.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName);
        resource.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setResource(resource);

        event.setId(UUID.randomUUID().toString());
        event.setOccurrenceTime(Date.from(Instant.now()));
        event.setSeverity(3);
        event.setExpirySeconds(0);

        type.setEventType(EventLifeCycleEvent.EVENT_TYPE_PROBLEM);
        type.setClassification("Monitoring Github Connector Calls");

        if (alertType == ConnectorConstants.INSTANCE_FORBIDDEN_CE_TYPE) {
            event.setSummary(summary);
            type.setCondition("Github API Forbidden");
            details.put("guidance", "Increate the Github API calls Rate Limit");
        } else if (alertType == ConnectorConstants.INSTANCE_UNAUTHENTICATED_CE_TYPE) {
            event.setSummary(summary);
            type.setCondition("Github API Call Unauthenticated");
            details.put("guidance",
                    "Ensure Github credentials are valid and increate the Github API calls Rate Limit if necessary");
        }
        event.setType(type);
        event.setDetails(details);

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
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, summary);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, summary);
            } else if (responseCode == 401) {
                ce = createAlertEvent(ConnectorConstants.INSTANCE_UNAUTHENTICATED_CE_TYPE,
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, summary);
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, "Alert created: " + summary);
            }
            logger.log(Level.INFO, "Sleeping for  " + seconds + " milliseconds");
            TimeUnit.MILLISECONDS.sleep(seconds);
        } catch (JsonProcessingException | InterruptedException e1) {
            logger.log(Level.SEVERE, e1.getMessage(), e1);
        }
    }

    public long getTimeLastCleared() {
        return timeLastCleared;
    }

    public void setTimeLastCleared(long timeLastCleared) {
        this.timeLastCleared = timeLastCleared;
    }

    protected String getPartition() {
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

        logger.log(Level.INFO, "Notify Creation Completable Future");
        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            return CompletableFuture.failedFuture(e);
        }

        // Get integration and send create notification
        CompletableFuture<ActionResult> result = null;
        try {
            Integration integration = getCurrentIntegration();
            ObjectNode responseJSON = integration.createIssue(requestContent,
                    this._configuration.get().getMappingsGithub());
            logger.log(Level.INFO, "Notify Creation Completable Future integration Response", responseJSON);
            if (responseJSON.get("status").asText().equals("success")) {
                // trigger kafka topic to push it to insights.
                logger.log(Level.INFO, "Triggering Kafka topic");
                ObjectMapper objectMapper = new ObjectMapper();
                String responseBody = responseJSON.get("data").asText();
                JsonNode data = objectMapper.readTree(responseBody);
                logger.log(Level.INFO, "Triggering Kafka topic data", data);
                String permalink = data.get("html_url").asText();
                logger.log(Level.INFO, "Triggering Kafka topic", data.get("html_url").asText());
                String response = IssueModel.getResponse(data.get("id").asText(), true,
                        "Created incident with id =  " + data.get("number").asText(), data.get("number").asText(),
                        getConnectorID(), IssueModel.getStoryId(request.getData()), "Successful", permalink);
                CloudEvent ce = createEvent(0, "com.ibm.sdlc.github.issue.create.response", response,
                        new URI(permalink));
                // bot orchestrator requires: "com.ibm.sdlc.snow.incident.create.response"
                emitCloudEvent(ACTION_GITHUB_RESPONSE, getPartition(), ce);
                emitCloudEvent("cp4waiops-cartridge.ticketresponse", getPartition(), ce);
                _issueCreationActionCounter.increment();
            } else {
                _issueCreationActionErrorCounter.increment();
            }
            result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJSON).build());
        } catch (ConnectorActionException ex) {
            _issueCreationActionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return CompletableFuture.failedFuture(ex);
        } catch (Exception e) {
            _issueCreationActionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return result;
    }

    @Override
    public CompletableFuture<ActionResult> notifyUpdate(ActionRequest request) {
        // Example of Ignoring updates until we have a better handling mechanism
        logger.log(Level.INFO, "Notify Update Completable Future", request);

        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            return CompletableFuture.failedFuture(e);
        }

        // Get integration and send create notification
        CompletableFuture<ActionResult> result = null;
        try {
            Integration integration = getCurrentIntegration();
            String issueNum = IssueModel.getIssueId(request.getData());
            if (issueNum != null) {
                ObjectNode responseJSON = integration.updateIssue(requestContent,
                        this._configuration.get().getMappingsGithub(), issueNum, null);
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJSON).build());
                    _issueUpdatingActionCounter.increment();
                } else {
                    _issueUpdatingActionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to GitHub", issueNum);
            }
        } catch (ConnectorActionException ex) {
            _issueUpdatingActionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return CompletableFuture.failedFuture(ex);
        } catch (Exception e) {
            _issueUpdatingActionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return result;
    }

    @Override
    public CompletableFuture<ActionResult> notifyClose(ActionRequest request) {
        // Map incoming data bytes to a JSON structure
        logger.log(Level.INFO, "Notify Close Completable Future", request);
        // Map incoming data bytes to a JSON structure
        ObjectNode requestContent;
        try {
            requestContent = request.dataAs(ObjectNode.class);
        } catch (ActionDataDeserializationException e) {
            return CompletableFuture.failedFuture(e);
        }

        // Get integration and send create notification
        CompletableFuture<ActionResult> result = null;
        try {
            Integration integration = getCurrentIntegration();
            String issueNum = IssueModel.getIssueId(request.getData());
            if (issueNum != null) {
                ObjectNode responseJSON = integration.updateIssue(requestContent,
                        this._configuration.get().getMappingsGithub(), issueNum, "close");
                logger.log(Level.INFO, "Notify Updating Completable Future integration Response", responseJSON);
                if (responseJSON.get("status").asText().equals("success")) {
                    _issueUpdatingActionCounter.increment();
                    result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJSON).build());
                } else {
                    _issueUpdatingActionErrorCounter.increment();
                }
            } else {
                logger.log(Level.INFO, "Didn't find issueNum to update to github", issueNum);
            }
        } catch (ConnectorActionException ex) {
            _issueUpdatingActionErrorCounter.increment();
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return CompletableFuture.failedFuture(ex);
        } catch (Exception e) {
            _issueCreationActionErrorCounter.increment();
            logger.log(Level.SEVERE, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
        return result;
    }
}
