package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.ibm.cp4waiops.connectors.sdk.Util;

import org.json.JSONException;
import org.json.JSONObject;

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
}
