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

import org.eclipse.microprofile.health.Liveness;

@Liveness
@ApplicationScoped
public class TicketConnectorLivenessCheck extends SDKCheck {
    public TicketConnectorLivenessCheck() {
        super(TicketConnectorLivenessCheck.class.getName(), Type.LIVENESS);
    }
}
