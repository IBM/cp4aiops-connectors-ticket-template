package com.ibm.aiops.connectors.importer;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.BulkRequest;

import org.opensearch.client.RestClient;

import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.endpoints.BooleanResponse;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;

import com.ibm.aiops.connectors.importer.SearchStatus.Status;
import com.ibm.cp4waiops.connectors.sdk.PropertyReadException;
import com.ibm.cp4waiops.connectors.sdk.models.Ticket;

import java.security.cert.X509Certificate;

public class OpenSearchHelper {
    OpenSearchClient opensearchClient;

    public String DIRECT_TO_SEARCH_PASSWORD = System.getenv().getOrDefault("DIRECT_TO_SEARCH_PASSWORD", "");
    public String DIRECT_TO_SEARCH_HOSTNAME = System.getenv().getOrDefault("DIRECT_TO_SEARCH_HOSTNAME", "");
    public int DIRECT_TO_SEARCH_PORT = Integer.parseInt(System.getenv().getOrDefault("DIRECT_TO_SEARCH_PORT", "1"));
    public String DIRECT_TO_SEARCH_USERNAME = System.getenv().getOrDefault("DIRECT_TO_SEARCH_USERNAME", "");

    protected static final int MAX_BATCH_SIZE = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_MAX_BATCH_SIZE", "1000"));
    protected static final long FLUSH_INTERVAL_SECONDS = Long
            .parseLong(System.getenv().getOrDefault("SEARCH_BULK_INSERT_FLUSH_INTERVAL_SECONDS", "10"));
    protected static final int MAX_BULK_SIZE_MB = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_MAX_SIZE_MB", "3"));
    protected static final int CONCURRENT_REQUESTS = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_CONCURRENT_REQUESTS", "100"));
    protected static final int BACKOFF_TIME_DELAY_SEC = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_BACKOFF_TIME_DELAY_SEC", "1"));
    protected static final int BACKOFF_MAX_TRIES = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_BACKOFF_MAX_TRIES", "3"));
    protected static final int CLOSE_BULK = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_CLOSE_PROCESSOR", "1"));
    // If the cluster size is large, then High Availability is enabled, which means the replicas is 1 shards are 5.
    // The shards being 5 matches what the Flink job does for ServiceNow
    protected static int SEARCH_SHARDS = Integer.parseInt(System.getenv().getOrDefault("SEARCH_SHARDS", "5"));
    protected static int SEARCH_REPLICAS = "small".equalsIgnoreCase(System.getenv().getOrDefault("SIZE", "small")) ? 0
            : 1;

    protected RestClient client = null;

    // The connector is only connecting to an elastic instance within the cluster that we trust, which is
    // why the TrustManager accepts all
    protected static TrustManager[] trustAllCertificates = new TrustManager[] { new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    } };

    /*
     * Loads OpenSearch variables using the service binding loader. It will look at a mounted directory to read the
     * variables in the kubernetes scenario. For the local case, create these files and use the env var
     * "SERVICE_BINDING_ROOT" to point to your local directory
     */
    public OpenSearchHelper() throws PropertyReadException, NoSuchAlgorithmException {

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());

            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(DIRECT_TO_SEARCH_USERNAME, DIRECT_TO_SEARCH_PASSWORD));

            HttpHost host = new HttpHost(DIRECT_TO_SEARCH_HOSTNAME, DIRECT_TO_SEARCH_PORT, "https");

            client = RestClient.builder(host)
                    .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                            .setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .setDefaultCredentialsProvider(credentialsProvider))
                    .build();

            OpenSearchTransport transport = new RestClientTransport(client, new JacksonJsonpMapper());

            opensearchClient = new OpenSearchClient(transport);

        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    public SearchStatus insert(ArrayList<Ticket> ticketList, String indexName) {

        // 1. Check if index exists with retries
        ExistsRequest existsRequest = ExistsRequest.of(b -> b.index(indexName));
        boolean indexExists = false;

        for (int i = 0; i < BACKOFF_MAX_TRIES; i++) {
            System.out.println("Index exists test. Retry count=" + i);
            try {
                BooleanResponse exists = opensearchClient.indices().exists(existsRequest);

                if (exists.value()) {
                    System.out.println( "Index exists for " + indexName);
                    indexExists = true;
                } else {
                    System.out.println("Index does not exist" + indexName);
                    indexExists = false;
                }
                // If successful, break out of the loop
                break;
            } catch (Exception e) {
                System.out.println("Error checking if index exists: " + e.getMessage());
                // Hit an error for the maximum number of tries trying to see if the index exists
                // Terminate to avoid other operations
                if (BACKOFF_MAX_TRIES > 0 && i == BACKOFF_MAX_TRIES - 1) {
                    return new SearchStatus(Status.WARNING,
                            "Could not determine if opensearch index exists. This may be caused by opensearch being down");
                }
            }

            try {
                Thread.sleep(BACKOFF_TIME_DELAY_SEC * 1000);
            } catch (Exception e) {
                System.out.println("Error sleeping for the backoff: " + e.getMessage());
            }
        }

        // At this point, opensearch was reachable, since it was able to detect if the index exists or not.
        // 2. If the index does not exist, attempt to create one with retries
        if (!indexExists) {
            for (int i = 0; i < BACKOFF_MAX_TRIES; i++) {
                System.out.println("Creating index" + indexName + " retry count=" + i);
                try {
                    IndexSettings settings = new IndexSettings.Builder().numberOfShards(String.valueOf(SEARCH_SHARDS))
                            .numberOfReplicas(String.valueOf(SEARCH_REPLICAS)).build();
                    CreateIndexRequest createIndexRequest = new CreateIndexRequest.Builder().index(indexName)
                            .settings(settings).build();

                    opensearchClient.indices().create(createIndexRequest);
                    System.out.println("Created index: " + indexName);
                    System.out.println("Created index with replicas: " + SEARCH_REPLICAS + " and shards: " + SEARCH_SHARDS);
                    indexExists = true;
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(BACKOFF_TIME_DELAY_SEC * 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 3. If the index still doesn't exist, then return
        if (!indexExists) {
            return new SearchStatus(Status.WARNING, "The index couldn't be created so the data cannot be inserted");
        }

        // 4. If the index exists, then attempt to insert data with retry. The retry is in the insertBulk
        if (ticketList != null && ticketList.size() > 0) {

            System.out.println("Ticket list size: " + ticketList.size() + " Max batch size property: " + MAX_BATCH_SIZE);

            ArrayList<BulkOperation> ops = new ArrayList<>();

            int ticketLen = ticketList.size();
            int maxBatchSizeCounter = 0;
            SearchStatus status = null;

            for (int i = 0; i < ticketLen; i++) {
                Ticket doc = ticketList.get(i);
                ops.add(new BulkOperation.Builder()
                        .index(IndexOperation.of(io -> io.index(indexName).id(doc.getSys_id()).document(doc))).build());
                maxBatchSizeCounter++;
                if (maxBatchSizeCounter == MAX_BATCH_SIZE) {
                    // The max batch size was reached so insert
                    status = insertBulk(ops, indexName);
                    if (status.getStatus().equals(Status.OK)) {
                        // If everything was okay, reset the counter and the Bulk operations
                        maxBatchSizeCounter = 0;
                        ops.clear();
                    } else {
                        // In an error state, return a failure and stop inserting data
                        return status;
                    }
                }
            }
            // If the MAX_BATCH_SIZE isn't reached or there are remaining records not pushed, then finish inserting here
            if (ops.size() > 0)
                status = insertBulk(ops, indexName);

            // If no errors happened during bulk insert, return OK
            return new SearchStatus(Status.OK, "Data inserted into OpenSearch successfully");
        } else {
            // return status as warning
            return new SearchStatus(Status.WARNING, "Ticket list was null, no data was inserted");
        }
    }

    protected SearchStatus insertBulk(ArrayList<BulkOperation> ops, String indexName) {
        BulkResponse bulkResponse;
        Throwable elasticResponse = null;

        // Retry on failure equal to BACKOFF_MAX_TRIES
        // The time to wait until trying again is
        for (int i = 0; i < BACKOFF_MAX_TRIES; i++) {
            try {
                // If this isn't declared each time, you will get "java.lang.IllegalStateException: Object builders can
                // only be used once"
                BulkRequest.Builder bulkReq = new BulkRequest.Builder().index(indexName).operations(ops)
                        .refresh(Refresh.WaitFor);
                bulkResponse = opensearchClient.bulk(bulkReq.build());
                System.out.println("Bulk response size: " + bulkResponse.items().size());
                // If no errors, then return
                return new SearchStatus(Status.OK, "Data inserted into OpenSearch successfully");
            } catch (Exception e) {
                e.printStackTrace();
                elasticResponse = e;
            }

            try {
                Thread.sleep(BACKOFF_TIME_DELAY_SEC * 1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // A failure had occured when inserting data
        return new SearchStatus(Status.WARNING, "Data was not inserted into elastic", elasticResponse);
    }

    public void closeBulkProcessor() {
        // Do nothing, since BulkProcessor is not implemented in the OpenSearch library yet
    }
}
