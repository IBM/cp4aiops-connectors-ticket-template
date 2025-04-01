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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.TicketAction;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionConnectorSettings;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionDataDeserializationException;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionRequest;
import com.ibm.cp4waiops.connectors.sdk.actions.ActionResult;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;
import com.ibm.cp4waiops.connectors.sdk.notifications.NotificationConnectorBase;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

public class TicketConnector extends NotificationConnectorBase {
    static final Logger logger = Logger.getLogger(TicketConnector.class.getName());
    public static String ACTION_TYPE_CHANGE_RISK_COMMENT = "com.ibm.sdlc.snow.comment.create";
    private static final URI SELF_SOURCE = URI.create("connectors.aiops.ibm.com/ticketingsystem"); // Replace ticketing
                                                                                                   // with the change
                                                                                                   // management system

    // Counters in case you need any metrics to be captured about the failures or
    // successful transactions.
    private Counter _issuePollingActionCounter;
    private Counter _issuePollingActionErrorCounter;
    private Counter _issueActionCounter;
    private Counter _issueActionErrorCounter;

    protected ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);

    // Queue to process events without blocking
    protected ConcurrentLinkedQueue<ConnectorAction> actionQueue = new ConcurrentLinkedQueue<ConnectorAction>(); 

    private ConnectorAction issuePollingAction;
    private IssuePollingAction issuePollingInstance;

    protected AtomicReference<Configuration> _configuration;

    protected AtomicLong _lastStatus;

    protected String _systemName;

    public TicketConnector() {
        _configuration = new AtomicReference<>();
        _lastStatus = new AtomicLong(0);
    }

    // Todo: Only required if you want to make capture the metrics.
    // Example on how we use counters for polling and action succcess and errors.
    @Override
    public void registerMetrics(MeterRegistry metricRegistry) {
        super.registerMetrics(metricRegistry);

        _issuePollingActionCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_POLL_COUNTER);
        _issuePollingActionErrorCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_POLL_ERROR_COUNTER);
        _issueActionCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_ACTIONS_COUNTER);
        _issueActionErrorCounter = metricRegistry.counter(ConnectorConstants.ACTION_ISSUE_ACTIONS_ERROR_COUNTER);
    }

    // This is triggered when an integration is created or updated.
    // This is to verify the connection and to make a connection to the outbound system. d
    // Todo: Have the connection or set up to your system here
    @Override
    public ActionConnectorSettings onConfigure(ConnectorConfigurationHelper config)
            throws ConnectorException, ConnectorActionException {
        try {
            Configuration newConfiguration = config.getDataObject(Configuration.class);

            this._systemName = config.getSystemName();

            if (newConfiguration == null) {
                throw new ConnectorException("no configuration provided");
            }

            logger.log(Level.INFO, "Configuring ConnectionId: " + config.getConnectionID());
            // Verify the connection here if needed
            this._configuration.set(newConfiguration);

            collectData(newConfiguration); // Polling data.

            emitStatus(ConnectorStatus.Phase.Running, Duration.ofMinutes(5)); // On successful verification, show status
                                                                              // as Running in the Integration UI.

            logger.log(Level.INFO, "Integration Configured", this._configuration.get());
        } catch (Exception ex) {
            emitStatus(ConnectorStatus.Phase.Errored, Duration.ofMinutes(5)); // If there are any errors, show status as
                                                                              // Error in the Integration UI.
        }
        return ActionConnectorSettings.builder().sourceUri(SELF_SOURCE).build();
    }

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
        logger.log(Level.INFO, "Terminating");

        stopPolling();
        clearAlerts(ConnectorConstants.ALERT_TYPES_LIST);
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

    void collectData(Configuration connectionCreateCfg) {

        logger.log(Level.INFO, "collectData(): Stopping existing polling");

        stopPolling();

        // collect data if data flow is enable
        if (booleanEqual(connectionCreateCfg.isData_flow(), true)) {
            logger.log(Level.INFO, "Data flow is on");
            issuePollingAction = new ConnectorAction(ConnectorConstants.ISSUE_POLL, connectionCreateCfg, this,
                    _issuePollingActionCounter, _issuePollingActionErrorCounter, null);
            addActionToQueue(issuePollingAction);
        }
    }

    protected boolean booleanEqual(Boolean a, Boolean b) {
        if (a == null || b == null)
            return a == b;
        return a.equals(b);
    }

    private void stopPolling() {
        // Stop old polling
        if (issuePollingInstance != null) {
            logger.log(Level.INFO, "stopping problem polling");
            issuePollingInstance.stop();
        }
    }

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

                // ensure the connection to ticketing system is verified

                processNextAction(); // processing the actions in queue.

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

    // Example: generate an alert
    // 1. Create a policy in AIOps that matches the conditions in this alert
    // 2. When called multiple times, the alert count will increase
    // try {
    // logger.log(Level.INFO,
    // "Generating an alert (same alerts re-run multiple times will increase the
    // alert counter and not generate another alert entry into the Alerts table in
    // CP4AIOps)");
    // CloudEvent ce;
    // ce = createAlertEvent(_configuration.get(), "ticket.alert.type",
    // EventLifeCycleEvent.EVENT_TYPE_PROBLEM);
    // emitCloudEvent(TicketAction.TOPIC_LIFECYCLE_INPUT_EVENTS, getPartition(),
    // ce);
    // } catch (JsonProcessingException error) {
    // logger.log(Level.SEVERE, "failed to construct cpu threshold breached cloud
    // event", error);
    // }
    public CloudEvent createAlertEvent(Configuration config, String alertType, String eventType)
            throws JsonProcessingException {
        EventLifeCycleEvent elcEvent = newInstanceAlertEvent();
        return CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(ConnectorConstants.SELF_SOURCE)
                .withType(alertType).withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
    }

    // Use this if you want to clear alerts after certain time.
    public CloudEvent createAlertEvent(String alertType, String eventType, String summary, int expiryInSeconds)
            throws JsonProcessingException {
        EventLifeCycleEvent elcEvent = newInstanceAlertEvent(alertType, eventType, summary, expiryInSeconds);
        return CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(ConnectorConstants.SELF_SOURCE)
                .withType(alertType).withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
    }

    public void triggerAlerts(String type) {
        CloudEvent ce;
        try {
            // Example of alert creation when polling is done. This creates an alert, you can check in the alerts page.
            if (type.equals(ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE)) {
                ce = createAlertEvent(ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE,
                        EventLifeCycleEvent.EVENT_TYPE_PROBLEM, "Historical data collection is done", 3600); // default
                                                                                                             // to 1
                                                                                                             // hour
                emitCloudEvent(ConnectorConstants.TOPIC_INPUT_LIFECYCLE_EVENTS, getPartition(), ce);
                logger.log(Level.INFO, "Historical data collection is done.");
            }
        } catch (JsonProcessingException e1) {
            logger.log(Level.SEVERE, e1.getMessage(), e1);
        }
    }

    /**
     * An example of a generated alert. If your event is run multiple times, the event count will increase. If you want
     * a new event created, modify the name, source, or type field. That will deal with the de-duplication that can be
     * set in CP4AIOps. Alternatively, in the AIOPs UI, change the status of the Incident to resolved, then wait a few
     * minutes and the Incident and resulting Alert will be in a resolved state and then closed. You can run this
     * integraiton again to have the event occur again.
     *
     * @return EventLifeCycleEvent which is used to represent the alert API
     */
    protected EventLifeCycleEvent newInstanceAlertEvent() {
        EventLifeCycleEvent event = new EventLifeCycleEvent();
        EventLifeCycleEvent.Type type = new EventLifeCycleEvent.Type();
        Map<String, String> details = new HashMap<>();

        Map<String, Object> sender = new HashMap<>();
        sender.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Ticket Resource");
        sender.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, getComponentName());
        sender.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Ticket Resource");
        resource.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, getComponentName());
        resource.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setResource(resource);

        event.setId(UUID.randomUUID().toString());
        event.setOccurrenceTime(Date.from(Instant.now()));
        event.setSeverity(3);
        event.setExpirySeconds(0);

        type.setEventType(EventLifeCycleEvent.EVENT_TYPE_PROBLEM);
        type.setClassification("Email account setup");

        event.setSummary("Create email account for new employee.");
        type.setCondition("New user has arrived into the organization");
        details.put("guidance", "Have the email admin create a new email account");
        event.setType(type);
        event.setDetails(details);

        return event;
    }

    // Use this if you want to clear alerts after certain time.
    protected EventLifeCycleEvent newInstanceAlertEvent(String alertType, String eventType, String summary,
            int expiryInSeconds) {
        EventLifeCycleEvent event = new EventLifeCycleEvent();
        EventLifeCycleEvent.Type type = new EventLifeCycleEvent.Type();
        Map<String, String> details = new HashMap<>();

        Map<String, Object> sender = new HashMap<>();
        sender.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Ticketing System Integration");
        sender.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName); // this._systemName is the integration
                                                                               // name
        sender.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID()); // getConnectorID() give the
                                                                                    // connector id.
        event.setSender(sender);

        Map<String, Object> resource = new HashMap<>();
        resource.put(EventLifeCycleEvent.RESOURCE_TYPE_FIELD, "Ticketing System Integration");
        resource.put(EventLifeCycleEvent.RESOURCE_NAME_FIELD, this._systemName);
        resource.put(EventLifeCycleEvent.RESOURCE_SOURCE_ID_FIELD, getConnectorID());
        event.setResource(resource);

        event.setId(UUID.randomUUID().toString());
        event.setOccurrenceTime(Date.from(Instant.now()));
        event.setSeverity(3); // 3 represents warning
        event.setExpirySeconds(expiryInSeconds); // alerts expires

        type.setEventType(eventType);
        type.setClassification("Monitoring Ticketing System Connector Calls");
        if (!eventType.equals(EventLifeCycleEvent.EVENT_TYPE_RESOLUTION)) {
            event.setSummary(summary);
            // Example of how we can add conditions, guidance and severity when historical data collection is done.
            if (alertType == ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE) {
                type.setCondition("Historical data collection is done.");
                details.put("guidance", "Set up Similar Tickets Training");
                event.setSeverity(2); // 2 represents information.
            }
            event.setType(type);
            event.setDetails(details);
        }

        return event;
    }

    // Example of how event can be created to be produced.
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

    @Override
    public CompletableFuture<ActionResult> notifyCreate(ActionRequest request) {

        logger.log(Level.INFO, "Notify Creation Completable Future", request);

        ConnectorAction connectorAction = new ConnectorAction(ConnectorConstants.ISSUE_CREATE,
                this._configuration.get(), this, _issueActionCounter, _issueActionErrorCounter, request);
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
                this._configuration.get(), this, _issueActionCounter, _issueActionErrorCounter, request);
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
                this, _issueActionCounter, _issueActionErrorCounter, request);
        addActionToQueue(connectorAction);
        CompletableFuture<ActionResult> result = null;
        ObjectNode responseJson = JsonNodeFactory.instance.objectNode();
        responseJson.set("status", JsonNodeFactory.instance.textNode("success"));
        result = CompletableFuture.completedFuture(ActionResult.builder().body(responseJson).build());
        return result;

    }
}