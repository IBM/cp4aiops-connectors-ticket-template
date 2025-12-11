package com.ibm.aiops.connectors.template;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String key = ConnectorConstants.TICKET_TYPE;

    public static String encode(String token) {
        byte[] bytes = token.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= key.getBytes()[i % key.length()];
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String decode(String encodedToken) {
        byte[] bytes = Base64.getDecoder().decode(encodedToken);
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] ^= key.getBytes()[i % key.length()];
        }
        return new String(bytes);
    }

    // Function to calculate sleep duration until a given epoch time
    public static long getTimeDifference(String epochTimeString, long currentTime) {
        Long epochTime = Long.parseLong(epochTimeString) + 1; // Adding 1 to make sure that api is called after the
                                                              // reset time
        long timeDifference = epochTime - currentTime;
        return timeDifference;
    }

    public static String getReadableDateFromEpoch(String epochTimeString) {
        Long epochTimeSeconds = Long.parseLong(epochTimeString) + 1; // Adding 1 to make sure that api is called after
                                                                     // the reset time
        String timeZoneId = "UTC"; // Example timezone ID

        Instant instant = Instant.ofEpochSecond(epochTimeSeconds);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timeZoneId));

        // Define the desired date-time format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy hh:mm:ss a 'UTC'",
                Locale.ENGLISH);

        // Format the LocalDateTime to a readable date-time string
        String formattedDateTime = dateTime.format(formatter);

        return formattedDateTime;
    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }
}
