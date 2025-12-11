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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.ibm.aiops.connectors.template.integrations.GithubIntegration;
import com.ibm.cp4waiops.connectors.sdk.Util;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.cloudevents.CloudEvent;

public class TestUtils {
    public static JSONObject getDataFromCloudEvent(CloudEvent ce) throws JSONException {
        String ceJSONString = Util.convertCloudEventToJSON(ce);
        JSONObject ceJSON = new JSONObject(ceJSONString);
        JSONObject ceDataJSON = ceJSON.getJSONObject("data");

        return ceDataJSON;
    }

    public static String getJSONFromTestResources(String filePath) throws IOException {
        String file = "src/test/resources/" + filePath;
        return getJsonString(file);
    }

    public static String getJSONFromTestData(String filePath) throws IOException {
        String file = "src/test/data/" + filePath;
        return getJsonString(file);
    }

    private static String getJsonString(String file) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(file)));
        return json;
    }

    // The run method in actions runs async, so certain
    // checks will fail without a small delay (for example metrics)
    public static void waitForSimpleRun() throws InterruptedException {
        Thread.sleep(3000);
    }

    @Test
    @DisplayName("Test time difference should match epoch time")
    void testTimeDifference() throws Exception {

        LocalDateTime localDateTime = LocalDateTime.of(2024, 4, 3, 22, 39, 40);
        long currentTimeStamp = localDateTime.toInstant(ZoneOffset.UTC).getEpochSecond();
        String epochString = "1712187580"; // 11:39:40 PM
        long epoch = Long.parseLong(epochString);
        Assertions.assertEquals("2024-04-03T23:39:40Z", Instant.ofEpochSecond(epoch).toString());
        long expected = 3601;
        long diffTime = Utils.getTimeDifference(epochString, currentTimeStamp);
        Assertions.assertEquals(expected, diffTime);
        long addTime = currentTimeStamp + diffTime;
        Instant.ofEpochSecond(addTime);
        Assertions.assertEquals(epoch + 1, addTime);

        Assertions.assertEquals("2024-04-03T23:39:41Z", Instant.ofEpochSecond(addTime).toString());
    }

    @Test
    @DisplayName("Test time difference should match epoch time with nearest time")
    void testTimeDifferencewithCurrenttime() throws Exception {

        long currentTimeStamp = Instant.now().getEpochSecond();
        String epochString = "1712187580"; // 11:39:40 PM
        long epoch = Long.parseLong(epochString);
        Instant epochInstant = Instant.ofEpochSecond(epoch);

        Assertions.assertEquals("2024-04-03T23:39:40Z", epochInstant.toString());

        long diffTime = Utils.getTimeDifference(epochString, currentTimeStamp);
        long addTime = currentTimeStamp + diffTime;
        Instant.ofEpochSecond(addTime);
        Assertions.assertEquals(epoch + 1, addTime);

        Assertions.assertEquals("2024-04-03T23:39:41Z", Instant.ofEpochSecond(addTime).toString());
    }

    @Test
    @DisplayName("Readable date from epoch time")
    void testReadableDateFormat() throws Exception {
        String epochString = "1712187580"; // 11:39:40 PM
        String readableDate = Utils.getReadableDateFromEpoch(epochString);

        Assertions.assertEquals("Wednesday, April 03, 2024 11:39:41 PM UTC", readableDate);
    }

    @Test
    @DisplayName("Should return domain name from URL")
    void testDomainNameFromURL() throws Exception {
        String url = "https://example.com/test/params?id=1";
        String domainName = Utils.getDomainName(url);

        Assertions.assertEquals("example.com", domainName);
    }
}
