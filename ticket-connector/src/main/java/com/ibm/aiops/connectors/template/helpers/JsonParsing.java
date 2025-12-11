/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/
package com.ibm.aiops.connectors.template.helpers;

import com.api.jsonata4java.expressions.EvaluateException;
import com.api.jsonata4java.expressions.EvaluateRuntimeException;
import com.api.jsonata4java.expressions.Expressions;
import com.api.jsonata4java.expressions.ParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonParsing {

    static final Logger logger = Logger.getLogger(JsonParsing.class.getName());

    public static String jsonataMap(String json, String expression) {
        JsonNode result;
        Expressions expr = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonObj = null;

        try {
            jsonObj = mapper.readTree(json);
            // read necessary environment vars
            ((ObjectNode) jsonObj).put("URL_PREFIX", System.getenv("URL_PREFIX"));
            logger.log(Level.WARNING, "URL_PREFIX: " + ((ObjectNode) jsonObj).get("URL_PREFIX"));

        } catch (IOException e1) {
            logger.log(Level.WARNING, "Mapper unable to read json object: " + e1.getLocalizedMessage());
        }

        try {
            logger.log(Level.FINER, "Parse expression: " + expression);
            expr = Expressions.parse(expression);
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Parsing exception: " + e.getLocalizedMessage());
        } catch (EvaluateRuntimeException ere) {
            logger.log(Level.WARNING, "Evaluate runtime exception: " + ere.getLocalizedMessage());
        } catch (JsonProcessingException e) {
            logger.log(Level.WARNING, "JSON processing exception: " + e.getLocalizedMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO Exception: " + e.getLocalizedMessage());
        }
        try {
            if (expr != null) {
                result = expr.evaluate(jsonObj);
                if (result == null || result.isEmpty()) {
                    logger.log(Level.INFO, "Expression not matched. ");
                } else {
                    logger.log(Level.FINE, "Parsed result: " + mapper.writeValueAsString(result));
                    return result.toString();
                }
            }
        } catch (EvaluateException | JsonProcessingException e) {
            logger.log(Level.WARNING, "Evaluate | JSON processing exception: " + e.getLocalizedMessage());
        }
        return json;
    }
}
