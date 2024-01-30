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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.bridge.ConnectorStatus;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.ConnectorBase;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.EventLifeCycleEvent;
import com.ibm.cp4waiops.connectors.sdk.KafkaTopicHelper;
import com.ibm.cp4waiops.connectors.sdk.SDKSettings;
import com.ibm.cp4waiops.connectors.sdk.TicketAction;
import com.ibm.cp4waiops.connectors.sdk.Util;
import com.ibm.cp4waiops.connectors.sdk.models.IncidentCreation;
import com.ibm.cp4waiops.connectors.sdk.models.Ticket;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.micrometer.core.instrument.MeterRegistry;

import org.json.JSONArray;
import org.json.JSONObject;

public class TicketConnector extends ConnectorBase {
    static final Logger logger = Logger.getLogger(TicketConnector.class.getName());
    public static String ACTION_TYPE_CHANGE_RISK_COMMENT = "com.ibm.sdlc.snow.comment.create";

    protected AtomicReference<Configuration> _configuration;

    protected AtomicLong _lastStatus;

    public TicketConnector() {
        _configuration = new AtomicReference<>();
        _lastStatus = new AtomicLong(0);
    }

    @Override
    public void registerMetrics(MeterRegistry metricRegistry) {
        super.registerMetrics(metricRegistry);
    }

    /**
     * onConfigure is called when the gRPC server sends information about the ConnectorConfiguration to the connector.
     * This will include changes to the ConnectorConfiguration (e.g. if the user updates a field via the CPAIOPs UI)
     */
    @Override
    public SDKSettings onConfigure(CloudEvent event) throws ConnectorException {
        ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(event);
        Configuration configuration = helper.getDataObject(Configuration.class);
        if (configuration == null) {
            throw new ConnectorException("No configuration provided");
        }

        // Display the ConnectorConfiguration for debugging. Do not print out any
        // sensitive information like passwords
        StringBuffer buffer = new StringBuffer();
        buffer.append("Type: " + event.getType());
        buffer.append("\nData flow: " + configuration.getDataFlow());
        buffer.append("\nHistoric start: " + configuration.getStart());
        buffer.append("\nHistoric end: " + configuration.getEnd());
        buffer.append("\nTypes: " + configuration.getTypes());
        buffer.append("\nUsername: " + configuration.getUsername());
        buffer.append("\nURL: " + configuration.getUrl());
        buffer.append("\nMapping: " + configuration.getMapping());

        logger.log(Level.INFO, buffer.toString());

        _configuration.set(configuration);

        // Set initial topics and local state if needed
        SDKSettings settings = new SDKSettings();

        HashSet<String> options = new HashSet<String>();
        options.add(KafkaTopicHelper.OPTION_CHANGES);
        options.add(KafkaTopicHelper.OPTION_INCIDENTS);
        KafkaTopicHelper kafkaHelper = new KafkaTopicHelper(options);

        settings.consumeTopicNames = kafkaHelper
                .addConsumeTopics(new String[] { TicketAction.TOPIC_INPUT_REQUESTS });
        String[] defaultProduce = { TicketAction.TOPIC_LIFECYCLE_INPUT_EVENTS,
                TicketAction.TOPIC_OUTPUT_RESPONSES };
        settings.produceTopicNames = kafkaHelper.addProduceTopics(defaultProduce);

        try {
            int consumeTopicNameLength = settings.consumeTopicNames.length;
            int produceTopicNameLength = settings.produceTopicNames.length;

            StringBuffer bufferTopic = new StringBuffer();
            for (int i = 0; i < consumeTopicNameLength; i++) {
                bufferTopic.append(settings.consumeTopicNames[i] + " ");
            }
            logger.log(Level.INFO, "Consume topics: " + bufferTopic.toString());

            bufferTopic = new StringBuffer();
            for (int i = 0; i < produceTopicNameLength; i++) {
                bufferTopic.append(settings.produceTopicNames[i] + " ");
            }
            logger.log(Level.INFO, "Produce topics: " + bufferTopic.toString());
        } catch (Exception e) {
            logger.log(Level.INFO, "Cannot display consume and produce topic names");
        }

        return settings;
    }

    @Override
    public SDKSettings onReconfigure(CloudEvent event) throws ConnectorException {
        // Update topics and local state if needed
        SDKSettings settings = onConfigure(event);
        return settings;
    }

    @Override
    public void onTerminate(CloudEvent event) {
        // Cleanup external resources if needed
    }

    @Override
    public void run() {
        HashMap<String, String> mapping = new HashMap<String, String>();

        // For historical training of data, this ticket action is used
        TicketAction ticketAction = new TicketAction(this, mapping, "ticket template", "ticket template URL",
                ConnectorConstants.HISTORICAL);

        Ticket ticket = null;
        ObjectMapper objectMapper = new ObjectMapper();

        // Generate sample incidents for Similiar Incident AI model training
        // This data is programatically generated, to ensure a sufficient data set
        for (int i = 0; i < 200; i++) {
            JSONObject json = new JSONObject();
            json.put(Ticket.key_sys_id, "sysid" + i);
            json.put(Ticket.key_number, "number" + i);

            String assignName = "";
            if (i >= 0 && i < 100) {
                assignName = "Email Admin";
            } else if (i >= 100 && i < 101) {
                assignName = "System Admin";
            } else if (i >= 101 && i < 200) {
                assignName = "Manager";
            }

            json.put(Ticket.key_assigned_to, assignName);
            json.put(Ticket.key_sys_created_by, "Email Service Bot");
            json.put(Ticket.key_sys_domain, "global");
            json.put(Ticket.key_business_service, "email");
            json.put(Ticket.key_state, "Closed");
            json.put(Ticket.key_short_description, "Create email account for new employee.");
            json.put(Ticket.key_impact, "2 - Medium");
            json.put(Ticket.key_description, "Create email account for new employee in the Developer group.");
            json.put(Ticket.key_close_code, "Solved (Permanently)");
            json.put(Ticket.key_close_notes, "Email account created in the Developer group.");
            json.put(Ticket.key_closed_at, "2023-10-22 02:46:44");
            json.put(Ticket.key_opened_at, "2023-10-22 01:46:44");
            json.put(Ticket.key_type, "type" + i);
            json.put(Ticket.key_reason, "reason" + i);
            json.put(Ticket.key_justification, "justification" + i);
            json.put(Ticket.key_backout_plan, "backup plan" + i);

            // When similar incident is called, the URL that points to the incident is
            // this source. In your ITSM, make sure this generated link points to the
            // proper incident
            String source = "https://example.org/incident/" + i;
            json.put("source", source);

            try {
                ticket = objectMapper.readValue(json.toString(), Ticket.class);

                ticketAction.emitIncident(ticket, source);
                ticketAction.insertIncidentIntoElastic(ticket);
            } catch (JsonMappingException e) {
                logger.log(Level.WARNING, "Failed to map JSON: ", e);
            } catch (JsonProcessingException e) {
                logger.log(Level.WARNING, "Failed to process JSON: ", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to insert into elastic: ", e);
            }
        }

        for (int i = 200; i < 300; i++) {
            JSONObject json = new JSONObject();
            json.put(Ticket.key_sys_id, "sysid" + i);
            json.put(Ticket.key_number, "number" + i);

            String assignName = "";
            if (i >= 200 && i < 300) {
                assignName = "Server Admin";
            } else if (i >= 300 && i < 401) {
                assignName = "System Admin";
            } else if (i >= 401 && i < 500) {
                assignName = "IT Specialist";
            }

            json.put(Ticket.key_assigned_to, assignName);
            json.put(Ticket.key_sys_created_by, "IT Service Bot");
            json.put(Ticket.key_sys_domain, "global");
            json.put(Ticket.key_business_service, "IT Services");
            json.put(Ticket.key_state, "Closed");
            json.put(Ticket.key_short_description, "Install security patch.");
            json.put(Ticket.key_impact, "1 - High");
            json.put(Ticket.key_description, "Install the latest security patch and restart the server.");
            json.put(Ticket.key_close_code, "Solved (Permanently)");
            json.put(Ticket.key_close_notes, "Server was successfully patched, restarted, and verified as running.");
            json.put(Ticket.key_closed_at, "2023-10-22 02:46:44");
            json.put(Ticket.key_opened_at, "2023-10-22 01:46:44");
            json.put(Ticket.key_type, "type" + i);
            json.put(Ticket.key_reason, "reason" + i);
            json.put(Ticket.key_justification, "justification" + i);
            json.put(Ticket.key_backout_plan, "backup plan" + i);

            // When similar incident is called, the URL that points to the incident is
            // this source. In your ITSM, make sure this generated link points to the
            // proper incident
            String source = "https://example.org/incident/" + i;
            json.put("source", source);

            try {
                ticket = objectMapper.readValue(json.toString(), Ticket.class);
                ticketAction.emitIncident(ticket, source);
                ticketAction.insertIncidentIntoElastic(ticket);
            } catch (JsonMappingException e) {
                logger.log(Level.WARNING, "Failed to map JSON: ", e);
            } catch (JsonProcessingException e) {
                logger.log(Level.WARNING, "Failed to process JSON: ", e);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to insert into elastic: ", e);
            }
        }

        // Generate Change Request data, used to train the Change Risk AI model.
        // The Change Risk AI model is much more strict for the data, so the data cannot be programmatically generated
        // like for Similar Incident. As a result, more realistic data in example.json is used, to ensure there's enough
        // variance in the data to properly train the AI model
        InputStream exampleInputStream = this.getClass().getClassLoader().getResourceAsStream("META-INF/example.json");
        try {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(exampleInputStream, "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            JSONArray jsonArray = new JSONArray(responseStrBuilder.toString());

            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    ticket = objectMapper.readValue(jsonArray.get(i).toString(), Ticket.class);
                    ticketAction.emitChangeRequest(ticket, ConnectorConstants.SELF_SOURCE.toString());
                    ticketAction.insertChangeRequestIntoElastic(ticket);
                } catch (JsonMappingException e) {
                    logger.log(Level.WARNING, "Failed to map JSON: ", e);
                } catch (JsonProcessingException e) {
                    logger.log(Level.WARNING, "Failed to process JSON: ", e);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to insert into elastic: ", e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load change request sample data: ", e);
        }

        // Example: Generate change request for change risk.
        // To prevent duplicate, need some randomness
        // You can check the change risk pod for debugging problems too: aimanager-aio-change-risk
        TicketAction ticketActionLive = new TicketAction(this, mapping, "ticket template", "ticket template URL",
                ConnectorConstants.LIVE);

        // Generate 1 or more example change requests, which will lead to the onAction being called with
        // a change risk score. The Change Risk AI Model must be trained and deployed first.
        for (int i = 0; i < 1; i++) {
            JSONObject json = new JSONObject();
            // The ID needs to be unique, so on re-runs you can see the change risk score again
            String uniqueID = UUID.randomUUID().toString();
            json.put(Ticket.key_sys_id, "sysid" + uniqueID);
            json.put(Ticket.key_number, "number" + uniqueID);
            json.put(Ticket.key_assigned_to, "assigned_user" + i);
            json.put(Ticket.key_sys_created_by, "created_user" + i);
            json.put(Ticket.key_sys_domain, "global");
            json.put(Ticket.key_business_service, "IT Services");
            json.put(Ticket.key_type, "Emergency"); // Normal is another circumstance
            json.put(Ticket.key_state, "Open");
            json.put(Ticket.key_short_description, "Server blew up due to fire.");
            json.put(Ticket.key_impact, "1 - High");
            json.put(Ticket.key_reason, "reason" + i);
            json.put(Ticket.key_justification, "justification" + i);
            json.put(Ticket.key_description, "Server blew up due to a fire caused by a failed water cooling system");
            json.put(Ticket.key_backout_plan, "backup plan" + i);
            json.put(Ticket.key_close_code, "Open");
            json.put(Ticket.key_close_notes, "");
            json.put(Ticket.key_closed_at, "2023-10-22 02:46:44");
            json.put(Ticket.key_opened_at, "2023-10-22 01:46:44");

            try {
                ticket = objectMapper.readValue(json.toString(), Ticket.class);
                // Generate problematic change risk
                ticketActionLive.emitChangeRequest(ticket, ConnectorConstants.SELF_SOURCE.toString());
            } catch (JsonMappingException e) {
                logger.log(Level.WARNING, "Failed to map JSON: ", e);
            } catch (JsonProcessingException e) {
                logger.log(Level.WARNING, "Failed to parse JSON: ", e);
            }
        }

        // Example: generate an alert
        // 1. Create a policy in AIOps that matches the conditions in this alert
        // 2. When called multiple times, the alert count will increase
        try {
            logger.log(Level.INFO,
                    "Generating an alert (same alerts re-run multiple times will increase the alert counter and not generate another alert entry into the Alerts table in CP4AIOps)");
            CloudEvent ce;
            ce = createAlertEvent(_configuration.get(), "ticket.alert.type", EventLifeCycleEvent.EVENT_TYPE_PROBLEM);
            emitCloudEvent(TicketAction.TOPIC_LIFECYCLE_INPUT_EVENTS, getPartition(), ce);
        } catch (JsonProcessingException error) {
            logger.log(Level.SEVERE, "failed to construct cpu threshold breached cloud event", error);
        }

        // Put monitoring logic in here. For now, this loop keeps the status of the connector as ready.
        // If status is not sent every 5 minutes, the status of the connector will go into an Unknown state
        boolean interrupted = false;
        long statusLastUpdated = 0;
        final long NANOSECONDS_PER_SECOND = 1000000000;
        final long STATUS_UPDATE_PERIOD_S = 300;
        final long LOOP_PERIOD_MS = 1000;

        while (!interrupted) {
            try {
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
                Thread.currentThread().interrupt();
            }
        }
    }

    public CloudEvent createAlertEvent(Configuration config, String alertType, String eventType)
            throws JsonProcessingException {
        EventLifeCycleEvent elcEvent = newInstanceAlertEvent();
        return CloudEventBuilder.v1().withId(elcEvent.getId()).withSource(ConnectorConstants.SELF_SOURCE)
                .withType(alertType).withExtension(TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withData(Constant.JSON_CONTENT_TYPE, elcEvent.toJSON().getBytes(StandardCharsets.UTF_8)).build();
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

    @Override
    public void onAction(String channelName, CloudEvent event) {
        /*
         * See sample CloudEvents that come in via Kafka in the "samples" folder in the root of this project
         *
         * Change Risk Event Topic: cp4waiops-cartridge.connector-snow-actions Sample: samples/sampleChangeRisk.json
         *
         * Incident Creation Event Topic: cp4waiops-cartridge.lifecycle.output.connector-requests Sample:
         * samples/sampleIncidentCreation.json
         * 
         */
        logger.info("onAction called with type=" + event.getType());
        try {
            if (IncidentCreation.ACTION_TYPE_INCIDENT_CREATION.equals(event.getType())) {
                // Get the incident creation action. Use your ITSM system to create an
                // incident
                logger.info("Got incident create action");

                // Convert incoming action to JSON
                JSONObject obj = new JSONObject(Util.convertCloudEventToJSON(event));

                String uuidSysID = UUID.randomUUID().toString();
                String uuidIncidentNumber = UUID.randomUUID().toString();

                String permalink = "http://example.org/incident/" + uuidSysID;
                String response = IncidentCreation.getResponse(uuidSysID, true,
                        "Created incident with sys id =  " + uuidSysID, "INC" + uuidIncidentNumber, getConnectorID(),
                        IncidentCreation.getStoryId(obj), "Successful", permalink);

                /*
                 * Example json body: { "connection_id":"1b1aeb97-775c-46a6-9767-ce76838433d4", "story_id":"storyID2",
                 * "snow_response":{ "result":{
                 * "message":"Successfully created incident with id 4bf8880e2f6d81108bfb56e62799b6c8", "incident":{
                 * "sysId":"4bf8880e2f6d81108bfb56e62799b6c8", "incidentNumber":"INC0021356" }, "status":"success" } },
                 * "permalink":
                 * "https://dev109758.service-now.com/now/workspace/agent/record/incident/4bf8880e2f6d81108bfb56e62799b6c8"
                 * }
                 */

                 logger.info("Response: " + response);
                CloudEvent ce = createEvent(0, "com.ibm.sdlc.snow.incident.create.response", response, new URI(permalink));
                // bot orchestrator requires: "com.ibm.sdlc.snow.incident.create.response"
                emitCloudEvent("cp4waiops-cartridge.itsmincidentresponse", getPartition(), ce);
            } else if (ACTION_TYPE_CHANGE_RISK_COMMENT.equals(event.getType())) {
                // Get change risk action. Use your ITSM system to create a comment in the change
                // request with the change risk score
                logger.info("Got change risk action");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public CloudEvent createEvent(long responseTime, String ce_type, String jsonMessage) {
        // The cloud event being returned needs to be in a structured format
        return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withSource(ConnectorConstants.SELF_SOURCE)
                .withTime(OffsetDateTime.now()).withType(ce_type).withExtension("responsetime", responseTime)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withExtension("tooltype", ConnectorConstants.SELF_SOURCE)
                .withExtension("structuredcontentmode", "true").withData("application/json", jsonMessage.getBytes())
                .build();
    }

    public CloudEvent createEvent(long responseTime, String ce_type, String jsonMessage, URI source) {
        // The cloud event being returned needs to be in a structured format
        return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withSource(source)
                .withTime(OffsetDateTime.now()).withType(ce_type).withExtension("responsetime", responseTime)
                .withExtension(CONNECTION_ID_CE_EXTENSION_NAME, getConnectorID())
                .withExtension(COMPONENT_NAME_CE_EXTENSION_NAME, getComponentName())
                .withExtension("tooltype", ConnectorConstants.SELF_SOURCE)
                .withExtension("structuredcontentmode", "true").withData("application/json", jsonMessage.getBytes())
                .build();
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
}
