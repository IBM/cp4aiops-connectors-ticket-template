package com.ibm.aiops.connectors.template;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aiops.connectors.template.model.Configuration;
import com.ibm.cp4waiops.connectors.sdk.TicketAction;
import com.ibm.cp4waiops.connectors.sdk.actions.ConnectorActionException;
import com.ibm.cp4waiops.connectors.sdk.models.Ticket;

import io.micrometer.core.instrument.Counter;

public class IssuePollingAction implements Runnable {

    private Counter actionCounter;
    private Counter actionErrorCounter;
    private Configuration config;
    private String connMode;

    private ScheduledExecutorService executorService = null;

    private TicketConnector connector;

    static final Logger logger = Logger.getLogger(IssuePollingAction.class.getName());
    // Format the modified date and time acording to the github
    static final String dateForamtPAttern = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private AtomicBoolean stopDataCollection = new AtomicBoolean(false);
    private SimpleDateFormat sdf = new SimpleDateFormat(dateForamtPAttern); // Format by github
    Date nowDate;
    TicketAction ticketAction;

    private HttpClientUtil httpClientUtil;

    public IssuePollingAction(ConnectorAction action) {

        actionCounter = action.getActionCounter();
        actionErrorCounter = action.getActionErrorCounter();

        config = action.getConfiguration();
        connMode = config.getCollectionMode();

        connector = action.getConnector();
        this.httpClientUtil = new HttpClientUtil(action.configuration.getUrl(), action.configuration.getUsername(),
                action.configuration.getPassword());

    }

    @Override
    public void run() {
        logger.log(Level.INFO, "Run Incident Poll Action");

        HashMap<String, String> mapping = new HashMap<String, String>();

        ticketAction = new TicketAction(connector, mapping, ConnectorConstants.TICKET_TYPE, config.getUrl(), connMode);

        actionCounter.increment();
        try {
            if (connMode.equals(ConnectorConstants.HISTORICAL)) {
                logger.log(Level.INFO, "Start collecting historical data");
                fetchAndEmitIssue();
                connector.triggerAlerts(ConnectorConstants.INSTANCE_HISTORICAL_DATACOLLECTION_CE_TYPE);
            } else {
                logger.log(Level.INFO, "Start collecting live data");

                // Create a single-threaded executor
                executorService = Executors.newSingleThreadScheduledExecutor();
                // Get interval sampling rate. For incidents, it is in minutes. Default is 1
                // second if not set by user
                executorService.scheduleAtFixedRate(this::fetchAndEmitIssue, 0, config.getIssueSamplingRate(),
                        TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to collect the data " + e);
        }

    }

    public void stop() {
        logger.log(Level.INFO, "stop(): issue stop called");
        // This variable needs to be set as the shutdown of the executorService
        // will not force stop the polling from stopping
        stopDataCollection.set(true);
        ticketAction.closeSearchBulkProcessor();

        if (executorService != null) {
            logger.log(Level.INFO, "stop(): Stopping issue polling thread");
            executorService.shutdownNow();
            logger.log(Level.INFO, "issue polling stopped");
        }
    }

    private void fetchAndEmitIssue() {
        logger.log(Level.INFO, "Calling fetchAndEmitIssue");
        String queryString = null;
        if (connMode.equals(ConnectorConstants.HISTORICAL) && config.getStart() > 0) {
            logger.log(Level.INFO, "Start collecting historical data");
            Date startDate = new Date(config.getStart());
            String dateStr = sdf.format(startDate);
            // Getting closed ones for historical data.
            queryString = "?state=closed&since=" + dateStr;
            logger.log(Level.INFO, "Modified Date and Time: " + dateStr);
        } else {
            // Get the current date and time
            LocalDateTime currentDateTime = LocalDateTime.now();
            // Subtract minutes from sampling
            LocalDateTime modifiedDateTime = currentDateTime.minusMinutes(config.getIssueSamplingRate());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateForamtPAttern);
            String dateStr = modifiedDateTime.format(formatter);
            queryString = "?state=all&since=" + dateStr;
            logger.log(Level.INFO, "Original Date and Time: " + currentDateTime);
            logger.log(Level.INFO, "Modified Date and Time: " + dateStr);
        }

        queryAllPages(queryString);
    }

    /**
     * Queries all pages within that date range
     *
     * @param dateStr
     */
    protected void queryAllPages(String connModeBasedQueryString) {

        try {
            // Pagination reference:
            // https://docs.github.com/en/rest/using-the-rest-api/using-pagination-in-the-rest-api?apiVersion=2022-11-28

            // Get the first page of data
            // Remove trailing slash

            String apiURL = config.getUrl().replaceAll("/$", "") + connModeBasedQueryString + "&per_page=100&page=1";
            HttpResponse<String> response = getIssues(apiURL);
            String results = "";

            boolean pagesRemaining = true;

            if (response != null) {
                while (pagesRemaining) {
                    results = response.body().toString();

                    // Convert results to JSONArray
                    JSONArray resultsJSONArray = new JSONArray(results);

                    int len = resultsJSONArray.length();
                    if (len > 0) {

                        ArrayList<Ticket> ticketList = new ArrayList<Ticket>();

                        for (int i = 0; i < len; i++) {
                            // Only get issues. PRs count as issues, so must be excluded
                            JSONObject jsonObj = resultsJSONArray.getJSONObject(i);
                            processTickets(jsonObj, ticketList);
                            actionCounter.increment();
                        }

                        logger.log(Level.INFO, "resultsJSONArray: ", resultsJSONArray);

                        if (ticketList.size() > 0)
                            ticketAction.insertIncident(ticketList);

                        Optional<String> link = response.headers().firstValue("link");

                        if (link.isPresent()) {

                            final String regex = "(?<=<)([\\S]*)(?=>; rel=\\\"Next\\\")";
                            final String linkStr = link != null ? link.get() : null;
                            logger.log(Level.INFO, linkStr);

                            final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                            final Matcher matcher = pattern.matcher(linkStr);
                            String nextURL = "";
                            if (matcher.find()) {
                                nextURL = matcher.group(1);
                                logger.log(Level.WARNING, "Next Url found ", nextURL);
                                response = getIssues(nextURL);
                            } else {
                                pagesRemaining = false;
                            }
                        } else {
                            pagesRemaining = false;
                        }
                    } else {
                        pagesRemaining = false;
                    }
                }

            }
        } catch (NoSuchElementException e) {
            actionErrorCounter.increment();
            logger.log(Level.WARNING, "No such element exception ", e);
        } catch (NullPointerException e) {
            actionErrorCounter.increment();
            logger.log(Level.WARNING, "Null Pointer Exception while quering pages ", e);
        } catch (Exception e) {
            actionErrorCounter.increment();
            logger.log(Level.WARNING, "Failed to query all pages ", e);
        }

    }

    protected void processTickets(JSONObject obj, ArrayList<Ticket> ticketList) {
        // Exclude all PRs
        try {
            if (!obj.has("pull_request")) {
                Ticket ticket = new Ticket();
                JSONObject json = new JSONObject();
                String html_url = obj.getString("html_url");
                String closedByUser = "";
                json.put(Ticket.key_sys_id, html_url);
                json.put(Ticket.key_number, obj.getNumber("number").toString());

                json.put(Ticket.key_assigned_to, "");
                json.put(Ticket.key_sys_created_by, obj.getJSONObject("user").get("login"));
                json.put(Ticket.key_sys_domain, "");

                json.put(Ticket.key_business_service, "");
                if (obj.getString("state").equals("closed")) {
                    json.put(Ticket.key_state, "Closed");
                    json.put(Ticket.key_close_code, "Closed");
                } else {
                    json.put(Ticket.key_state, "Open");
                    json.put(Ticket.key_close_code, "Open");
                    json.put(Ticket.key_close_code, "Open");
                }

                json.put(Ticket.key_short_description, obj.getString("title"));
                json.put(Ticket.key_impact, "");
                json.put(Ticket.key_description, obj.get("body").toString());

                JSONObject openedBy = (JSONObject) obj.get("user");
                String openedByUser = openedBy.getString("login");
                json.put(Ticket.key_opened_by, openedByUser);

                json.put(Ticket.key_source_name, ConnectorConstants.TICKET_TYPE);
                if (obj.has("closed_by")) {
                    JSONObject closedBy = (JSONObject) obj.get("closed_by");
                    closedByUser = closedBy.getString("login");
                } else {
                    /*
                     * If the issue got closed by the creator, then it's closed_by is the user. Typically 'user' field
                     * in the response refers to the user who created the issue, performed an action on the issue such
                     * as commenting, closing or reopening.
                     */
                    closedByUser = openedByUser;
                }
                json.put(Ticket.key_sys_updated_by, closedByUser);
                json.put(Ticket.key_closed_by, closedByUser);
                json.put(Ticket.key_caller_id, "IBM Cloud Pak for AIOps "); // This is how it is in ServiceNow, we follow the same format
                json.put(Ticket.key_sys_class_name, "Incident");
                json.put(Ticket.key_instance, getDomainName(html_url));
                json.put(Ticket.key_sys_updated_on, obj.getString("updated_at"));
                json.put(Ticket.key_sys_created_on, obj.getString("created_at"));
                json.put(Ticket.key_connectionmode, connMode);
                json.put(Ticket.key_connection_id, connector.getConnectorID());

                int numComments = obj.getInt("comments");

                // Don't get comments if the issue isn't closed (since we don't need close notes
                // and
                // making API calls to GitHub needs to be reduced)
                if (numComments > 0 && obj.getString("state").equals("closed")) {
                    String commentsURL = obj.getString("comments_url").replaceAll("/$", "");

                    String commentsRes = getComments(commentsURL,
                            "?direction=asc&sort=created&" + getLastCommentPage(numComments, 1)); // An example of how
                                                                                                  // we can retrieve
                                                                                                  // closed comments

                    JSONArray commentResponse = new JSONArray(commentsRes);
                    // Only get last comment, so length is 1
                    if (commentResponse.length() == 1) {
                        JSONObject obj2 = commentResponse.getJSONObject(0);
                        json.put(Ticket.key_close_notes, obj2.getString("body"));
                    }
                    // If no close note is there, then this can cause training to fail
                }

                Object closedAt = obj.get("closed_at");
                if (closedAt != null && !obj.isNull("closed_at")) {
                    json.put(Ticket.key_closed_at, obj.getString("closed_at"));
                }

                json.put(Ticket.key_opened_at, obj.getString("created_at"));
                json.put(Ticket.key_type, "");
                json.put(Ticket.key_reason, "");

                json.put(Ticket.key_justification, "");
                json.put(Ticket.key_backout_plan, "");
                String source = obj.getString("html_url");
                json.put(Ticket.key_source, source);
                ObjectMapper objectMapper = new ObjectMapper();
                ticket = objectMapper.readValue(json.toString(), Ticket.class);

                if (json.has(Ticket.key_closed_at) && json.getString(Ticket.key_state).equals("Closed")) {
                    try {
                        // Add the tickets into the array and bulk insert after
                        ticketList.add(ticket);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to insert: ", e);
                    }
                }
            }
        } catch (JsonMappingException e) {
            logger.log(Level.WARNING, "Failed to map JSON: ", e);
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "Failed to process JSON: ", e);
        } catch (URISyntaxException e) {
            logger.log(Level.WARNING, "Failed to get domain from github url", e.getMessage());
        }
    }

    // This refers Github Comments API example.
    public static String getLastCommentPage(int comments, int perPage) {
        int pageNum = Math.floorDiv(comments, perPage);
        int remainder = Math.floorMod(comments, perPage);

        // An extra page is needed
        if (remainder > 0) {
            pageNum++;
        }
        return "per_page=" + perPage + "&page=" + pageNum;
    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public String getComments(String commentsURL, String queryParam) throws ConnectorActionException {
        int responseCode = 200;
        try {
            String url = commentsURL + queryParam;

            logger.log(Level.INFO, "Get comments query: " + url);

            CompletableFuture<HttpResponse<String>> res = this.httpClientUtil.get(url);
            HttpResponse<String> response = res.get();

            responseCode = response.statusCode();
            if (responseCode == 200) {
                String body = response.body();
                return body;
            } else {
                logger.log(Level.INFO, "Failed to retrieve comments. Status code: " + responseCode);
                logger.log(Level.INFO, "Response body: " + response.body());
                return null;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred when retrieving comments. Error: ", e);
            return null;
        }
    }

    public HttpResponse<String> getIssues(String url) throws ConnectorActionException {
        int responseCode = 200;
        HttpResponse<String> result = null;
        try {

            CompletableFuture<HttpResponse<String>> res = this.httpClientUtil.get(url);

            HttpResponse<String> response = res.get();

            responseCode = response.statusCode();
            if (responseCode == 200) {
                result = response;
            } else {
                logger.log(Level.INFO, "Failed to retrieve issues. Status code: " + responseCode);
                logger.log(Level.INFO, "Response body: " + response.body());
                result = response;
            }
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Interrupt caused by a valid stop, polling stopped");
        } catch (Exception e) {
            throw new ConnectorActionException("Failed to query issues with error: " + e.getMessage(), responseCode);
        }
        return result;
    }

}
