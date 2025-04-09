package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.ConnectorConfigurationHelper;
import com.ibm.cp4waiops.connectors.sdk.Constant;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class TestConnectorTemplate extends TicketConnector {

    @Test
    @DisplayName("Loading configuration with all fields")
    void testLoadConfigurationAll() throws IOException {
        String result = TestUtils.getJSONFromTestResources("ConnectorConfiguration01.json");
        System.out.println(result);

        CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString())
                .withSource(ConnectorConstants.SELF_SOURCE).withType("com.ibm.sdlc.ticket.connection.request")
                .withExtension(TicketConnector.TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(TicketConnector.CONNECTION_ID_CE_EXTENSION_NAME, "connectorid")
                .withExtension(TicketConnector.COMPONENT_NAME_CE_EXTENSION_NAME, "connector")
                .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();

        ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(ce);
        Configuration configuration = helper.getDataObject(Configuration.class);

        Assertions.assertNotNull(configuration);
        Assertions.assertEquals(Long.parseLong("1701907199999"), configuration.getEnd());
        Assertions.assertEquals(Long.parseLong("1701388800000"), configuration.getStart());
        Assertions.assertEquals(true, configuration.isData_flow());
        Assertions.assertEquals("mappings", configuration.getMappings());
        Assertions.assertEquals("pass", configuration.getPassword());
        Assertions.assertEquals("admin", configuration.getUsername());
        Assertions.assertEquals("https://example.com", configuration.getUrl());
        Assertions.assertEquals("historical", configuration.getCollectionMode());
    }

    @Test
    @DisplayName("Loading configuration with some fields")
    void testLoadConfigurationSome() throws IOException {
        String result = TestUtils.getJSONFromTestResources("ConnectorConfiguration02.json");
        System.out.println(result);

        CloudEvent ce = CloudEventBuilder.v1().withId(UUID.randomUUID().toString())
                .withSource(ConnectorConstants.SELF_SOURCE).withType("com.ibm.sdlc.ticket.connection.request")
                .withExtension(TicketConnector.TENANTID_TYPE_CE_EXTENSION_NAME, Constant.STANDARD_TENANT_ID)
                .withExtension(TicketConnector.CONNECTION_ID_CE_EXTENSION_NAME, "connectorid")
                .withExtension(TicketConnector.COMPONENT_NAME_CE_EXTENSION_NAME, "connector")
                .withData(Constant.JSON_CONTENT_TYPE, result.getBytes(StandardCharsets.UTF_8)).build();

        ConnectorConfigurationHelper helper = new ConnectorConfigurationHelper(ce);
        Configuration configuration = helper.getDataObject(Configuration.class);

        Assertions.assertNotNull(configuration);
        Assertions.assertEquals(Long.parseLong("1701907199999"), configuration.getEnd());
        Assertions.assertEquals(Long.parseLong("1701388800000"), configuration.getStart());
        Assertions.assertEquals(true, configuration.isData_flow());
        Assertions.assertEquals("pass", configuration.getPassword());
        Assertions.assertEquals("admin", configuration.getUsername());
        Assertions.assertEquals("https://example.com", configuration.getUrl());
        Assertions.assertEquals("mappings2", configuration.getMappings());
        Assertions.assertEquals("live", configuration.getCollectionMode());
    }
}