package com.ibm.aiops.connectors.importer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.cp4waiops.connectors.sdk.models.Ticket;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Imports incident data into AIOps
 *
 */
public class JSONImporter 
{
    // The similar incident elastic index name
    public static final String INCIDENT_INDEX = "snowincident";

    public static void main(String[] args) {
        // Read the file from resources folder
        String jsonString = readResourceFile("sample.json");

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        ElasticHelper elasticHelper = null;
        try {
            elasticHelper = new ElasticHelper();

            if (jsonString != null) {
                // Parse the JSON string
                JSONArray jsonArr = new JSONArray(jsonString);

                int len = jsonArr.length();
                int validEntries = 0;
                for (int i=0; i<len; i++){
                    JSONObject jsonObject = jsonArr.getJSONObject(i);

                    String id = "";
                    // If any of these fields are null, skip the entry. Alternatively, if that field doesn't
                    // exist, replace with a static dummy string
                    try {
                        JSONObject normalizedJSON = new JSONObject();

                        normalizedJSON.put("sys_id", jsonObject.getString("id"));
                        normalizedJSON.put("number", jsonObject.getString("incident_number"));
                        normalizedJSON.put("assigned_to", jsonObject.getString("assignee"));
                        normalizedJSON.put("sys_created_by", jsonObject.getString("owner"));
                        normalizedJSON.put("sys_domain", jsonObject.getString("department"));



                        // Similar Incident looks for Closed state to determine if closed. Remapping
                        // Resolved to Closed
                        String state = jsonObject.getString("status");
                        if (state.equalsIgnoreCase("resolved")){
                            state = "Closed";
                        }
                        normalizedJSON.put("state", state);
                        normalizedJSON.put("close_code", jsonObject.getString("status"));
                        // In this sample, the short description is the same as the description
                        normalizedJSON.put("short_description", jsonObject.getString("description"));
                        normalizedJSON.put("impact", jsonObject.getString("impact"));
                        // If you have a more detailed description, set it here
                        normalizedJSON.put("description", jsonObject.getString("description"));
                        normalizedJSON.put("close_notes", jsonObject.getString("resolution"));
                        
                        // The date format needs to be converted to the formatter. This assumes the data source
                        // returns a unix timestamp format
                        normalizedJSON.put("closed_at", formatter.format(jsonObject.getInt("closed_date")));
                        normalizedJSON.put("opened_at", formatter.format(jsonObject.getInt("reported_date")));
                        
                        normalizedJSON.put("severity", jsonObject.getString("urgency"));
                        
                        // The source needs to be replaced with a custom URL for your data source
                        normalizedJSON.put("source", "https://example.com/incident/" + jsonObject.getString("incident_number"));
                        normalizedJSON.put("business_service", jsonObject.getString("service_type"));

                        ObjectMapper om = new ObjectMapper();
                        Ticket myTicket = om.readValue(normalizedJSON.toString(), Ticket.class);

                        elasticHelper.insertIntoElastic(myTicket.getHashMap(), INCIDENT_INDEX);
                        validEntries++;

                    } catch (Exception e){
                        System.out.println("Ignored id " + id + " due to " + e.getMessage());
                    }
                }
                System.out.println("Valid incidents to insert: "  + validEntries);
            }
        }
        catch (Exception e)   {
            e.printStackTrace();
        }
        finally {
            try {
                if (elasticHelper != null){
                    if (elasticHelper.closeBulkProcessor()){
                        // The BulkProcessor appears to have threads that aren't closed,
                        // since the program does not terminate without it. If we verify
                        // the BulkProcessor has closed sucessfully, exit the program 
                        // sucessfully to avoid a hang
                        System.exit(0);
                    }
                }
            }
            catch (Exception e){
                // Do nothing
            }
        }
    }

    private static String readResourceFile(String fileName) {
        ClassLoader classLoader = JSONImporter.class.getClassLoader();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = classLoader.getResourceAsStream(fileName);
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        finally {
            try {
                if (inputStream != null){
                    inputStream.close();
                }
            } catch (Exception e){
                // Do nothing
            }
            try {
                if (reader != null){
                    reader.close();
                }
            } catch (Exception e){
                // Do nothing
            }
        }
    }
}
