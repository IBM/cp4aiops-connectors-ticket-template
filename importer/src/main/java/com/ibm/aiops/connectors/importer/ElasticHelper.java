package com.ibm.aiops.connectors.importer;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.xcontent.XContentType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticHelper {
    private static final int MAX_BATCH_SIZE = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_MAX_BATCH_SIZE", "10"));
    private static final long FLUSH_INTERVAL_SECONDS = Long
            .parseLong(System.getenv().getOrDefault("SEARCH_BULK_INSERT_FLUSH_INTERVAL_SECONDS", "1"));
    private static final int MAX_BULK_SIZE_MB = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_MAX_SIZE_MB", "3"));
    private static final int CONCURRENT_REQUESTS = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_CONCURRENT_REQUESTS", "1"));
    private static final int BACKOFF_TIME_DELAY_SEC = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_BACKOFF_TIME_DELAY_SEC", "1"));
    private static final int BACKOFF_MAX_TRIES = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_INSERT_BACKOFF_MAX_TRIES", "3"));
    private static final int CLOSE_BULK = Integer
            .parseInt(System.getenv().getOrDefault("SEARCH_BULK_CLOSE_PROCESSOR", "1"));

    public String DIRECT_TO_SEARCH_PASSWORD = System.getenv().getOrDefault("DIRECT_TO_SEARCH_PASSWORD", "");
    public String DIRECT_TO_SEARCH_HOSTNAME = System.getenv().getOrDefault("DIRECT_TO_SEARCH_HOSTNAME", "");
    public int DIRECT_TO_SEARCH_PORT = Integer.parseInt(System.getenv().getOrDefault("DIRECT_TO_SEARCH_PORT", "1"));
    public String DIRECT_TO_SEARCH_USERNAME = System.getenv().getOrDefault("DIRECT_TO_SEARCH_USERNAME", "");;

    RestHighLevelClient client = null;
    BulkProcessor bulkProcessor;
    static final Logger logger = Logger.getLogger(ElasticHelper.class.getName());

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
     * Loads elastic variables using the service binding loader. It will look at a mounted directory to read the
     * variables in the kubernetes scenario. For the local case, create these files and use the env var
     * "SERVICE_BINDING_ROOT" to point to your local directory
     */
    public ElasticHelper() throws Exception {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());

            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(DIRECT_TO_SEARCH_USERNAME, DIRECT_TO_SEARCH_PASSWORD));
            RestClientBuilder builder = RestClient
                    .builder(new HttpHost(DIRECT_TO_SEARCH_HOSTNAME, DIRECT_TO_SEARCH_PORT, "https"))
                    .setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder
                            .setSSLContext(sslContext).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .setDefaultCredentialsProvider(credentialsProvider));
            this.client = new RestHighLevelClient(builder);
            this.bulkProcessor = createBulkProcessor(client);

            // Add index requests to the processor

        } catch (KeyManagementException e) {
            logger.log(Level.WARNING, "Error inserting into elastic: " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, "Error inserting into elastic: " + e.getMessage(), e);
        }
    }

    private static BulkProcessor createBulkProcessor(RestHighLevelClient client) {
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, BulkResponse bulkResponse) {
                for (BulkItemResponse bulkItemResponse : bulkResponse) {
                    if (bulkItemResponse.isFailed()) {
                        logger.log(Level.WARNING,
                                "Error bulk indexing into elastic: " + bulkItemResponse.getFailureMessage());
                    } else {
                        String documentId = bulkItemResponse.getResponse().getId();
                        logger.log(Level.FINEST, "Successfully indexed document with ID: " + documentId);

                        logger.log(Level.FINEST, "Operation Type " + bulkItemResponse.getOpType());
                    }
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
                logger.log(Level.WARNING, "Error bulk inserting into elastic: " + failure);
            }
        };

        // We can use BulkIngestor when we upgrade the elastic client version
        BulkProcessor bulkProcessor = BulkProcessor
                .builder((request, bulkListener) -> client.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                        listener)
                .setBulkActions(MAX_BATCH_SIZE).setBulkSize(new ByteSizeValue(MAX_BULK_SIZE_MB, ByteSizeUnit.MB)) // Max
                                                                                                                  // size
                                                                                                                  // of
                                                                                                                  // combined
                                                                                                                  // bulk
                                                                                                                  // requests
                .setConcurrentRequests(CONCURRENT_REQUESTS) // Number of concurrent requests
                .setFlushInterval(TimeValue.timeValueSeconds(FLUSH_INTERVAL_SECONDS)) // Time-based flushing
                .setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(BACKOFF_TIME_DELAY_SEC),
                        BACKOFF_MAX_TRIES)) // Backoff strategy
                .build();

        return bulkProcessor;
    }

    /**
     * Attempts to insert into elastic
     *
     * @param hashMap
     *            map containing data to be pushed to elastic
     * @param elasticIndex
     *            the elastic index to insert data into
     *
     * @throws JsonParseException
     *             HashMap could not be converted to valid JSON
     * @throws IOException
     *             data could not be inserted into elastic
     */
    public void insertIntoElastic(HashMap<String, Object> hashMap, String elasticIndex) throws IOException {

        String jsonString = new ObjectMapper().writeValueAsString(hashMap);
        IndexRequest indexRequest = new IndexRequest(elasticIndex).source(jsonString, XContentType.JSON)
                .id((String) hashMap.get("sys_id"));

        this.bulkProcessor.add(indexRequest);
    }

    public void closeBulkProcessor() {
        try {
            logger.log(Level.INFO, "Closing Bulk process in " + CLOSE_BULK + TimeUnit.MINUTES);
            this.bulkProcessor.awaitClose(CLOSE_BULK, TimeUnit.MINUTES);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Couldn't stop bulk processor", ex);
        }
    }
}
