package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.ConnectorException;
import com.ibm.cp4waiops.connectors.sdk.Constant;
import com.ibm.cp4waiops.connectors.sdk.SDKSettings;
import com.ibm.cp4waiops.connectors.sdk.StatusWriter;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import org.mockito.Mockito;

public class TestConnectorTemplate {

    // @Test
    // @DisplayName("Loading configuration with all fields")
    // void testLoadConfigurationAll() throws IOException {
    // String result = TestUtils.getJSONFromTestResources("ConnectorConfiguration01.json");
    // System.out.println(result);

    // CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString())
    // .withSource(ConnectorConstants.SELF_SOURCE).withType("com.ibm.sdlc.ticket.connection.request")
    // .withExtension(TicketConnector.TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
    // .withExtension(TicketConnector.CONNECTION_ID_CE_EXTENSION_NAME, "connectorid")
    // .withExtension(TicketConnector.COMPONENT_NAME_CE_EXTENSION_NAME, "connector")
    // .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();

    // ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(ce);
    // Configuration configuration = helper.getDataObject(Configuration.class);

    // Assertions.assertNotNull(configuration);
    // Assertions.assertEquals(Long.parseLong("1701907199999"), configuration.getEnd());
    // Assertions.assertEquals(Long.parseLong("1701388800000"), configuration.getStart());
    // Assertions.assertEquals(true, configuration);
    // Assertions.assertEquals("mappings", configuration.getMappingsGithub());
    // Assertions.assertEquals("owner", configuration.getOwner());
    // Assertions.assertEquals("repo", configuration.getRepo());
    // Assertions.assertEquals("pass", configuration.getToken());
    // Assertions.assertEquals("https://example.com", configuration.getUrl());
    // Assertions.assertEquals(true, configuration.isData_flow());
    // Assertions.assertEquals("historical", configuration.getCollectionMode());
    // }

    // @Test
    // @DisplayName("Loading configuration with some fields")
    // void testLoadConfigurationSome() throws IOException {
    // String result = TestUtils.getJSONFromTestResources("ConnectorConfiguration02.json");
    // System.out.println(result);

    // CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString())
    // .withSource(ConnectorConstants.SELF_SOURCE).withType("com.ibm.sdlc.ticket.connection.request")
    // .withExtension(TicketConnector.TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
    // .withExtension(TicketConnector.CONNECTION_ID_CE_EXTENSION_NAME, "connectorid")
    // .withExtension(TicketConnector.COMPONENT_NAME_CE_EXTENSION_NAME, "connector")
    // .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();

    // ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(ce);
    // Configuration configuration = helper.getDataObject(Configuration.class);

    // Assertions.assertNotNull(configuration);
    // Assertions.assertEquals(Long.parseLong("1701907199999"), configuration.getEnd());
    // Assertions.assertEquals(Long.parseLong("1701388800000"), configuration.getStart());
    // Assertions.assertEquals(true, configuration);
    // Assertions.assertEquals("mappings2", configuration.getMappingsGithub());
    // Assertions.assertEquals("owner2", configuration.getOwner());
    // Assertions.assertEquals("repo2", configuration.getRepo());
    // Assertions.assertEquals("pass2", configuration.getToken());
    // Assertions.assertEquals("https://example.com2", configuration.getUrl());
    // Assertions.assertEquals(false, configuration.isData_flow());
    // Assertions.assertEquals("historical2", configuration.getCollectionMode());
    // Assertions.assertEquals("inference", configuration.getCollectionMode());
    // }

    // @Test
    // @DisplayName("Test connector properties")
    // void testConnectorProperties() throws IOException, ConnectorException {
    // TicketConnector ticketConnector = new TicketConnector();

    // String result = TestUtils.getJSONFromTestResources("ConnectorConfiguration02.json");
    // System.out.println(result);

    // CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString())
    // .withSource(ConnectorConstants.SELF_SOURCE).withType("com.ibm.sdlc.ticket.connection.request")
    // .withExtension(TicketConnector.TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
    // .withExtension(TicketConnector.CONNECTION_ID_CE_EXTENSION_NAME, "connectorid")
    // .withExtension(TicketConnector.COMPONENT_NAME_CE_EXTENSION_NAME, "connector")
    // .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();

    // SDKSettings settings = ticketConnector.onConfigure(ce);

    // // Verify the Kafka consume topic names
    // Assertions.assertEquals(3, settings.consumeTopicNames.length);
    // Assertions.assertEquals("cp4waiops-cartridge.lifecycle.output.connector-requests",
    // settings.consumeTopicNames[0]);
    // Assertions.assertEquals("cp4waiops-cartridge.connector-snow-actions", settings.consumeTopicNames[1]);
    // Assertions.assertEquals("cp4waiops-cartridge.snow-handlers", settings.consumeTopicNames[2]);

    // // Verify the Kafka produce topic names
    // Assertions.assertEquals(6, settings.produceTopicNames.length);
    // Assertions.assertEquals("cp4waiops-cartridge.lifecycle.input.events", settings.produceTopicNames[0]);
    // Assertions.assertEquals("cp4waiops-cartridge.lifecycle.input.connector-responses",
    // settings.produceTopicNames[1]);
    // Assertions.assertEquals("cp4waiops-cartridge.connector-snow-actions", settings.produceTopicNames[2]);
    // Assertions.assertEquals("cp4waiops-cartridge.changerequest", settings.produceTopicNames[3]);
    // Assertions.assertEquals("cp4waiops-cartridge.incident", settings.produceTopicNames[4]);
    // Assertions.assertEquals("cp4waiops-cartridge.itsmincidentresponse", settings.produceTopicNames[5]);

    // StatusWriter mockStatusWriter;
    // mockStatusWriter = Mockito.mock(StatusWriter.class);

    // BlockingQueue<CloudEvent> eventOutput = new LinkedBlockingDeque<>(1024);
    // ticketConnector.onInit("test_conn_id", "test_connector", eventOutput, mockStatusWriter, null);

    // Assertions.assertEquals("test_conn_id", ticketConnector.getConnectorID());
    // Assertions.assertEquals("test_connector", ticketConnector.getComponentName());
    // // Assertions.assertEquals("{\"ce-partitionkey\":\"test_conn_id\"}", ticketConnector.getPartition());
    // }
}