package com.ibm.aiops.connectors.importer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.importer.models.TicketExtended;
import com.ibm.cp4waiops.connectors.sdk.models.Ticket;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 * Imports incident data into AIOps
 *
 */
public class JSONImporterChangeRequest 
{
    // The similar incident elastic index name
    public static final String INCIDENT_INDEX = "snowchangerequest";

    public static void main(String[] args) {
        // Read the file from resources folder
        String jsonString = readResourceFile("sampleChangeMultipleCloseNotes.json");

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        // HashMap
        HashMap<String,TicketExtended> changeRequestMaps = new HashMap<String,TicketExtended>();

        ElasticHelper elasticHelper = null;
        try {
            elasticHelper = new ElasticHelper();

            if (jsonString != null) {
                // Parse the JSON string
                JSONArray jsonArr = new JSONArray(jsonString);

                int len = jsonArr.length();
                int validEntries = 0;
                // Iterate and combine all close notes together. Change requests can have more than
                // one identifier with different close notes
                for (int i=0; i<len; i++){
                    JSONObject jsonObject = jsonArr.getJSONObject(i);

                    String id = "";
                    // If any of these fields are null, skip the entry. Alternatively, if that field doesn't
                    // exist, replace with a static dummy string
                    try {
                        JSONObject normalizedJSON = new JSONObject();

                        String current_sys_id = jsonObject.getString("sys_id");
                        String current_close_notes = jsonObject.getString("close_notes");

                        normalizedJSON.put("sys_id", current_sys_id);
                        normalizedJSON.put("number", jsonObject.getString("number"));
                        normalizedJSON.put("assigned_to", jsonObject.getString("assigned_to"));
                        normalizedJSON.put("sys_created_by", jsonObject.getString("sys_created_by"));
                        normalizedJSON.put("sys_domain", jsonObject.getString("sys_domain"));
                        normalizedJSON.put("business_service", jsonObject.getString("business_service"));
                        normalizedJSON.put("type", jsonObject.getString("type"));
                        normalizedJSON.put("state", jsonObject.getString("state"));
                        normalizedJSON.put("short_description", jsonObject.getString("short_description"));
                        normalizedJSON.put("impact", jsonObject.getString("impact"));
                        normalizedJSON.put("reason", jsonObject.getString("reason"));
                        normalizedJSON.put("justification", jsonObject.getString("justification"));
                        normalizedJSON.put("description", jsonObject.getString("description"));
                        normalizedJSON.put("backout_plan", jsonObject.getString("backout_plan"));
                        normalizedJSON.put("close_code", jsonObject.getString("close_code"));
                        normalizedJSON.put("close_notes", current_close_notes);
                        normalizedJSON.put("closed_at", jsonObject.getString("closed_at"));

                        ObjectMapper om = new ObjectMapper();
                        TicketExtended myTicket = om.readValue(normalizedJSON.toString(), TicketExtended.class);

                        if (changeRequestMaps.containsKey(current_sys_id)){
                            // If another change request was found with the same id, aggregate the close notes
                            String closeNotes = changeRequestMaps.get(current_sys_id).getClose_notes();
                            closeNotes += "\n" + current_close_notes;
                            
                            myTicket.setClose_notes(closeNotes);
                            changeRequestMaps.remove(current_sys_id);
                        }

                        changeRequestMaps.put(current_sys_id, myTicket);
                    } catch (Exception e){
                        System.out.println("Ignored id " + id + " due to " + e.getMessage());
                    }
                }

                // Iterate through just the hashmap to insert into elastic
                // Close notes have been aggregated together
                for (HashMap.Entry<String, TicketExtended> set :
                    changeRequestMaps.entrySet()) {
                    elasticHelper.insertIntoElastic(set.getValue().getHashMap(), INCIDENT_INDEX);
                    validEntries++;                                    
                }

                System.out.println("Valid change requests to insert: "  + validEntries);
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
