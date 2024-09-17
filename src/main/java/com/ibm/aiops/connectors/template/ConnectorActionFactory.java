/***********************************************************************
 *
 *      IBM Confidential
 *
 *      (C) Copyright IBM Corp. 2024
 *
 *      5737-M96
 *
 **********************************************************************/

package com.ibm.aiops.connectors.template;

public class ConnectorActionFactory {
    public static Runnable getRunnableAction(ConnectorAction action) {
        if (action != null) {
            if (ConnectorConstants.ISSUE_POLL.equals(action.getActionType())) {
                return new IssuePollingAction(action);
            }
        }
        return null;
    }
}
