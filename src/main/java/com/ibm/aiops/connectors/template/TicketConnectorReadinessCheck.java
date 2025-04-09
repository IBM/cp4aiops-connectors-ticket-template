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

import jakarta.enterprise.context.ApplicationScoped;

import com.ibm.cp4waiops.connectors.sdk.SDKCheck;

import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class TicketConnectorReadinessCheck extends SDKCheck {
    public TicketConnectorReadinessCheck() {
        super(TicketConnectorReadinessCheck.class.getName(), Type.READINESS);
    }
}
