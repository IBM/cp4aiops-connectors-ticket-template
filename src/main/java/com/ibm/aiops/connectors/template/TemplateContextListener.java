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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import com.ibm.cp4waiops.connectors.sdk.ConnectorManager;
import com.ibm.cp4waiops.connectors.sdk.SDKCheck;

@WebListener
public class TemplateContextListener implements ServletContextListener {
    static final Logger logger = Logger.getLogger(TemplateContextListener.class.getName());

    @Inject
    ManagerInstance instance;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.log(Level.INFO, "connector manager initializing");
        ConnectorManager manager = instance.getConnectorManager();
        manager.init();
        SDKCheck.addInstance(manager);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.log(Level.INFO, "template servlet destroying");
        ConnectorManager manager = instance.getConnectorManager();
        SDKCheck.removeInstance(manager);
        manager.destroy();
    }
}
