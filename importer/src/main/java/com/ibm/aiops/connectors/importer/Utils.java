package com.ibm.aiops.connectors.importer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String getStringFromDate(String inputString) {
        String validatedTime = validatedDateTime(inputString);
        if (validatedTime != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date date = inputFormat.parse(inputString);

                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // This is Snow pattern for
                                                                                             // Date strings
                String formatDateString = outputFormat.format(date);
                return formatDateString;
            } catch (ParseException ex) {
                return inputString; // return the same input string if it in different format.
            }
        }
        return inputString;
    }

    public static String validatedDateTime(String dateTimeString) {
        String regex = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(dateTimeString);
        if (matcher.find()) {
            return dateTimeString.substring(matcher.start(), matcher.end());
        }
        return null;
    }
}
