package com.ibm.aiops.connectors.template;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.template.model.IssueModel;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IssueModelTest {

    private static final String ISSUE_ID = "12345";

    @Test
    void getIssueId() throws IOException {
        String jsonString = "{ \"incident\": { \"insights\": [ { \"type\": \"aiops.ibm.com/insight-type/itsm/metadata\", \"details\": { \"name\": \"github\", \"permalink\": \"https://test.com/testOwner/testRepo/"
                + ISSUE_ID + "\", \"ticket_num\": \"" + ISSUE_ID + "\" } } ] } }";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);

        String issueId = IssueModel.getIssueId(jsonNode.toString().getBytes(), "testOwner", "testRepo");
        assertEquals(ISSUE_ID, issueId);
    }
}