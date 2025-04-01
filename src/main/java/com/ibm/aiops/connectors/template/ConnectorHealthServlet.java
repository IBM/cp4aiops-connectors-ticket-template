
/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2023
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Required for health checks when being deployed. These health checks are used in the BundleManifests's deployment.yaml
 * file
 */
@WebServlet(urlPatterns = "/h/*")
public class ConnectorHealthServlet extends HttpServlet {
    static final Logger logger = Logger.getLogger(ConnectorHealthServlet.class.getName());

    @Inject
    ManagerInstance instance;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String subPath = req.getPathInfo();
        if (subPath == null) {
            super.doGet(req, resp);
            return;
        }

        switch (req.getPathInfo()) {
        case "/live":
            instance.getConnectorManager().handleLiveRequest(req, resp);
            break;
        case "/ready":
            instance.getConnectorManager().handleReadyRequest(req, resp);
            break;
        case "/metrics":
            instance.getConnectorManager().handleMetricsRequest(req, resp);
            break;
        default:
            super.doGet(req, resp);
        }
    }
}