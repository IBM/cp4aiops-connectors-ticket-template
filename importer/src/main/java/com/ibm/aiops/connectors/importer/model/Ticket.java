package com.ibm.aiops.connectors.importer.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ibm.aiops.connectors.importer.Utils;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Ticket {
    protected String sys_id = "";
    protected String number = "";
    protected String opened_at = "";
    protected String state = "";
    protected String closed_at = "";
    protected String close_notes = "";
    protected String short_description = "";
    protected String description = "";
    protected String caused_by = "";
    protected String[] work_notes = {};
    protected String assigned_to = "";
    protected String sys_created_by = "";
    protected String sys_domain = "";
    protected String business_service = "";
    protected String type = "";
    protected String impact = "";
    protected String reason = "";
    protected String justification = "";
    protected String backout_plan = "";
    protected String close_code = "";
    protected String source = "";
    protected String source_name = "";
    protected String sys_updated_by = "";
    protected String caller_id = "";
    protected String connectionmode = "";

    // Extra fields from Snowincident index. Adding to be in sync with SNow
    protected String active = "";
    protected String activity_due = "";
    protected String additional_assignee_list = "";
    protected String approval = "";
    protected String approval_history = "";
    protected String approval_set = "";
    protected String assignment_group = "";
    protected String business_duration = "";
    protected String business_stc = "";
    protected String calendar_duration = "";
    protected String calendar_stc = "";
    protected String category = "";
    protected String child_incidents = "";
    protected String closed_by = "";
    protected String cmdb_ci = "";
    protected String comments = "";
    protected String comments_and_work_notes = "";
    protected String company = "";
    protected String connection_id = "";
    protected String contact_type = "";
    protected String correlation_display = "";
    protected String correlation_id = "";
    protected String due_date = "";
    protected String escalation = "";
    protected String expected_start = "";
    protected String follow_up = "";
    protected String group_list = "";
    protected String hold_reason = "";
    protected String instance = "";
    protected String knowledge = "";
    protected String location = "";
    protected String made_sla = "";
    protected String notify = "";
    protected String opened_by = "";
    protected String order = "";
    protected String parent = "";
    protected String parent_incident = "";
    protected String priority = "";
    protected String problem_id = "";
    protected String reassignment_count = "";
    protected String reopen_count = "";
    protected String reopened_by = "";
    protected String reopened_time = "";
    protected String resolved_at = "";
    protected String resolved_by = "";
    protected String rfc = "";
    protected String service_offering = "";
    protected String severity = "";
    protected String sla_due = "";
    protected String subcategory = "";
    protected String sys_class_name = "";
    protected String sys_created_on = "";
    protected String sys_domain_path = "";
    protected String sys_mod_count = "";
    protected String sys_tags = "";
    protected String sys_updated_on = "";
    protected String time_worked = "";
    protected String upon_approval = "";
    protected String upon_reject = "";
    protected String urgency = "";
    protected String user_input = "";
    protected String watch_list = "";
    protected String work_end = "";
    protected String work_notes_list = "";
    protected String work_start = "";
    protected String incidents_sync_to_aiops = "";

    public String getActive() {
        return active;
    }

    public String getActivity_due() {
        return activity_due;
    }

    public String getAdditional_assignee_list() {
        return additional_assignee_list;
    }

    public String getApproval() {
        return approval;
    }

    public String getApproval_history() {
        return approval_history;
    }

    public String getApproval_set() {
        return approval_set;
    }

    public String getAssignment_group() {
        return assignment_group;
    }

    public String getBusiness_duration() {
        return business_duration;
    }

    public String getBusiness_stc() {
        return business_stc;
    }

    public String getCalendar_duration() {
        return calendar_duration;
    }

    public String getCalendar_stc() {
        return calendar_stc;
    }

    public String getCategory() {
        return category;
    }

    public String getChild_incidents() {
        return child_incidents;
    }

    public String getClosed_by() {
        return closed_by;
    }

    public String getCmdb_ci() {
        return cmdb_ci;
    }

    public String getComments() {
        return comments;
    }

    public String getComments_and_work_notes() {
        return comments_and_work_notes;
    }

    public String getCompany() {
        return company;
    }

    public String getConnection_id() {
        return connection_id;
    }

    public String getContact_type() {
        return contact_type;
    }

    public String getCorrelation_display() {
        return correlation_display;
    }

    public String getCorrelation_id() {
        return correlation_id;
    }

    public String getDue_date() {
        return due_date;
    }

    public String getEscalation() {
        return escalation;
    }

    public String getExpected_start() {
        return expected_start;
    }

    public String getFollow_up() {
        return follow_up;
    }

    public String getGroup_list() {
        return group_list;
    }

    public String getHold_reason() {
        return hold_reason;
    }

    public String getInstance() {
        return instance;
    }

    public String getKnowledge() {
        return knowledge;
    }

    public String getLocation() {
        return location;
    }

    public String getMade_sla() {
        return made_sla;
    }

    public String getNotify() {
        return notify;
    }

    public String getOpened_by() {
        return opened_by;
    }

    public String getOrder() {
        return order;
    }

    public String getParent() {
        return parent;
    }

    public String getParent_incident() {
        return parent_incident;
    }

    public String getPriority() {
        return priority;
    }

    public String getProblem_id() {
        return problem_id;
    }

    public String getReassignment_count() {
        return reassignment_count;
    }

    public String getReopen_count() {
        return reopen_count;
    }

    public String getReopened_by() {
        return reopened_by;
    }

    public String getReopened_time() {
        return reopened_time;
    }

    public String getResolved_at() {
        return resolved_at;
    }

    public String getResolved_by() {
        return resolved_by;
    }

    public String getRfc() {
        return rfc;
    }

    public String getService_offering() {
        return service_offering;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSla_due() {
        return sla_due;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public String getSys_class_name() {
        return sys_class_name;
    }

    public String getSys_created_on() {
        return sys_created_on;
    }

    public String getSys_domain_path() {
        return sys_domain_path;
    }

    public String getSys_mod_count() {
        return sys_mod_count;
    }

    public String getSys_tags() {
        return sys_tags;
    }

    public String getSys_updated_on() {
        return sys_updated_on;
    }

    public String getTime_worked() {
        return time_worked;
    }

    public String getUpon_approval() {
        return upon_approval;
    }

    public String getUpon_reject() {
        return upon_reject;
    }

    public String getUrgency() {
        return urgency;
    }

    public String getUser_input() {
        return user_input;
    }

    public String getWatch_list() {
        return watch_list;
    }

    public String getWork_end() {
        return work_end;
    }

    public String getWork_notes_list() {
        return work_notes_list;
    }

    public String getWork_start() {
        return work_start;
    }

    public String getSys_id() {
        return this.sys_id;
    }

    public String getNumber() {
        return this.number;
    }

    public String getOpened_at() {
        return Utils.getStringFromDate(this.opened_at);
    }

    public String getState() {
        return this.state;
    }

    public String getClosed_at() {
        return Utils.getStringFromDate(this.closed_at);
    }

    public String getClose_notes() {
        return this.close_notes;
    }

    public String getShort_description() {
        return this.short_description;
    }

    public String getDescription() {
        return this.description;
    }

    public String getCaused_by() {
        return this.caused_by;
    }

    public String[] getWork_notes() {
        return this.work_notes;
    }

    public String getAssigned_to() {
        return this.assigned_to;
    }

    public String getSys_created_by() {
        return this.sys_created_by;
    }

    public String getSys_domain() {
        return this.sys_domain;
    }

    public String getBusiness_service() {
        return this.business_service;
    }

    public String getType() {
        return this.type;
    }

    public String getImpact() {
        return this.impact;
    }

    public String getReason() {
        return this.reason;
    }

    public String getJustification() {
        return this.justification;
    }

    public String getBackout_plan() {
        return this.backout_plan;
    }

    public String getClose_code() {
        return this.close_code;
    }

    public String getSource() {
        return this.source;
    }

    public String getSource_name() {
        return this.source_name;
    }

    public String getSys_updated_by() {
        return this.sys_updated_by;
    }

    public String getConnectionmode() {
        return this.connectionmode;
    }

    public String getCaller_id() {
        return this.caller_id;
    }

    public String getIncidents_sync_to_aiops() {
        return this.incidents_sync_to_aiops;
    }

    public static final String key_active = "active";
    public static final String key_activity_due = "activity_due";
    public static final String key_additional_assignee_list = "additional_assignee_list";
    public static final String key_approval = "approval";
    public static final String key_approval_history = "approval_history";
    public static final String key_approval_set = "approval_set";
    public static final String key_assignment_group = "assignment_group";
    public static final String key_business_duration = "business_duration";
    public static final String key_business_stc = "business_stc";
    public static final String key_calendar_duration = "calendar_duration";
    public static final String key_calendar_stc = "calendar_stc";
    public static final String key_category = "category";
    public static final String key_child_incidents = "child_incidents";
    public static final String key_closed_by = "closed_by";
    public static final String key_cmdb_ci = "cmdb_ci";
    public static final String key_comments = "comments";
    public static final String key_comments_and_work_notes = "comments_and_work_notes";
    public static final String key_company = "company";
    public static final String key_connection_id = "connection_id";
    public static final String key_contact_type = "contact_type";
    public static final String key_correlation_display = "correlation_display";
    public static final String key_correlation_id = "correlation_id";
    public static final String key_due_date = "due_date";
    public static final String key_escalation = "escalation";
    public static final String key_expected_start = "expected_start";
    public static final String key_follow_up = "follow_up";
    public static final String key_group_list = "group_list";
    public static final String key_hold_reason = "hold_reason";
    public static final String key_incident_state = "incident_state"; // incident_state is state
    public static final String key_instance = "instance";
    public static final String key_knowledge = "knowledge";
    public static final String key_location = "location";
    public static final String key_made_sla = "made_sla";
    public static final String key_notify = "notify";
    public static final String key_opened_by = "opened_by";
    public static final String key_order = "order";
    public static final String key_parent = "parent";
    public static final String key_parent_incident = "parent_incident";
    public static final String key_priority = "priority";
    public static final String key_problem_id = "problem_id";
    public static final String key_reassignment_count = "reassignment_count";
    public static final String key_reopen_count = "reopen_count";
    public static final String key_reopened_by = "reopened_by";
    public static final String key_reopened_time = "reopened_time";
    public static final String key_resolved_at = "resolved_at";
    public static final String key_resolved_by = "resolved_by";
    public static final String key_rfc = "rfc";
    public static final String key_service_offering = "service_offering";
    public static final String key_severity = "severity";
    public static final String key_sla_due = "sla_due";
    public static final String key_subcategory = "subcategory";
    public static final String key_sys_class_name = "sys_class_name";
    public static final String key_sys_created_on = "sys_created_on";
    public static final String key_sys_domain_path = "sys_domain_path";
    public static final String key_sys_mod_count = "sys_mod_count";
    public static final String key_sys_tags = "sys_tags";
    public static final String key_sys_updated_on = "sys_updated_on";
    public static final String key_time_worked = "time_worked";
    public static final String key_upon_approval = "upon_approval";
    public static final String key_upon_reject = "upon_reject";
    public static final String key_urgency = "urgency";
    public static final String key_user_input = "user_input";
    public static final String key_watch_list = "watch_list";
    public static final String key_work_end = "work_end";
    public static final String key_work_notes_list = "work_notes_list";
    public static final String key_work_start = "work_start";
    public static final String key_incidents_sync_to_aiops = "incidents_sync_to_aiops";

    // Required for Similar Incident types
    public static final String key_sys_id = "sys_id";
    public static final String key_number = "number";
    public static final String key_opened_at = "opened_at";
    public static final String key_state = "state";
    public static final String key_closed_at = "closed_at";
    public static final String key_close_notes = "close_notes";

    // In addition to the Similar Incident, the following is required for
    // Change Risk for incident ticket types
    public static final String key_short_description = "short_description";
    public static final String key_description = "description";
    public static final String key_caused_by = "caused_by";
    // needs to be String[]
    public static final String key_work_notes = "work_notes";

    // Required for Change Risk types
    // sys_id (already defined above)
    // number (already defined above)
    // close_notes
    // closed_at
    public static final String key_assigned_to = "assigned_to";
    public static final String key_sys_created_by = "sys_created_by";
    public static final String key_sys_domain = "sys_domain";
    public static final String key_business_service = "business_service";
    public static final String key_type = "type";
    // state (already defined above)
    // short_description
    public static final String key_impact = "impact";
    public static final String key_reason = "reason";
    public static final String key_justification = "justification";
    public static final String key_backout_plan = "backout_plan";
    public static final String key_close_code = "close_code";
    public static final String key_source = "source";
    public static final String key_source_name = "source_name";
    public static final String key_sys_updated_by = "sys_updated_by";
    public static final String key_caller_id = "caller_id";
    public static final String key_connectionmode = "connectionmode";

    /*
     * Converting to HashMap is required for search insertion
     */
    public HashMap<String, Object> getHashMap() {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        hashMap.put(key_sys_id, sys_id);
        hashMap.put(key_number, number);
        hashMap.put(key_state, state);
        hashMap.put(key_close_notes, close_notes);
        hashMap.put(key_short_description, short_description);
        hashMap.put(key_description, description);
        hashMap.put(key_caused_by, caused_by);
        hashMap.put(key_work_notes, work_notes);
        hashMap.put(key_assigned_to, assigned_to);
        hashMap.put(key_sys_created_by, sys_created_by);
        hashMap.put(key_sys_domain, sys_domain);
        hashMap.put(key_business_service, business_service);
        hashMap.put(key_type, type);
        hashMap.put(key_impact, impact);
        hashMap.put(key_reason, reason);
        hashMap.put(key_justification, justification);
        hashMap.put(key_backout_plan, backout_plan);
        hashMap.put(key_close_code, close_code);
        hashMap.put(key_source, source);
        hashMap.put(key_source_name, source_name);
        hashMap.put(key_sys_updated_by, sys_updated_by);
        hashMap.put(key_connectionmode, connectionmode);

        // Converting Dates to String to sync Github, Jira with existing SNow Data.
        hashMap.put(key_closed_at, Utils.getStringFromDate(closed_at));
        hashMap.put(key_opened_at, Utils.getStringFromDate(opened_at));
        hashMap.put(key_sys_updated_on, Utils.getStringFromDate(sys_updated_on));
        hashMap.put(key_sys_created_on, Utils.getStringFromDate(sys_created_on));

        // All other dates to be handled from connector end.
        hashMap.put(key_due_date, due_date);
        hashMap.put(key_resolved_at, resolved_at);

        hashMap.put(key_active, active);
        hashMap.put(key_activity_due, activity_due);
        hashMap.put(key_additional_assignee_list, additional_assignee_list);
        hashMap.put(key_approval, approval);
        hashMap.put(key_approval_history, approval_history);
        hashMap.put(key_approval_set, approval_set);
        hashMap.put(key_assignment_group, assignment_group);
        hashMap.put(key_business_duration, business_duration);
        hashMap.put(key_business_stc, business_stc);
        hashMap.put(key_calendar_duration, calendar_duration);
        hashMap.put(key_calendar_stc, calendar_stc);
        hashMap.put(key_category, category);
        hashMap.put(key_child_incidents, child_incidents);
        hashMap.put(key_closed_by, closed_by);
        hashMap.put(key_cmdb_ci, cmdb_ci);
        hashMap.put(key_comments, comments);
        hashMap.put(key_comments_and_work_notes, comments_and_work_notes);
        hashMap.put(key_company, company);
        hashMap.put(key_connection_id, connection_id);
        hashMap.put(key_contact_type, contact_type);
        hashMap.put(key_correlation_display, correlation_display);
        hashMap.put(key_correlation_id, correlation_id);
        hashMap.put(key_escalation, escalation);
        hashMap.put(key_expected_start, expected_start);
        hashMap.put(key_follow_up, follow_up);
        hashMap.put(key_group_list, group_list);
        hashMap.put(key_hold_reason, hold_reason);
        hashMap.put(key_incident_state, state); // incident_state is state
        hashMap.put(key_instance, instance);
        hashMap.put(key_knowledge, knowledge);
        hashMap.put(key_location, location);
        hashMap.put(key_made_sla, made_sla);
        hashMap.put(key_notify, notify);
        hashMap.put(key_opened_by, opened_by);
        hashMap.put(key_order, order);
        hashMap.put(key_opened_by, opened_by);
        hashMap.put(key_parent, parent);
        hashMap.put(key_parent_incident, parent_incident);
        hashMap.put(key_priority, priority);
        hashMap.put(key_problem_id, problem_id);
        hashMap.put(key_reassignment_count, reassignment_count);
        hashMap.put(key_reopen_count, reopen_count);
        hashMap.put(key_reopened_by, reopened_by);
        hashMap.put(key_reopened_time, reopened_time);
        hashMap.put(key_resolved_by, resolved_by);
        hashMap.put(key_rfc, rfc);
        hashMap.put(key_service_offering, service_offering);
        hashMap.put(key_severity, severity);
        hashMap.put(key_sla_due, sla_due);
        hashMap.put(key_subcategory, subcategory);
        hashMap.put(key_sys_class_name, sys_class_name);
        hashMap.put(key_sys_domain_path, sys_domain_path);
        hashMap.put(key_sys_mod_count, sys_mod_count);
        hashMap.put(key_sys_tags, sys_tags);
        hashMap.put(key_time_worked, time_worked);
        hashMap.put(key_upon_approval, upon_approval);
        hashMap.put(key_upon_reject, upon_reject);
        hashMap.put(key_urgency, urgency);
        hashMap.put(key_user_input, user_input);
        hashMap.put(key_watch_list, watch_list);
        hashMap.put(key_work_end, work_end);
        hashMap.put(key_work_notes_list, work_notes_list);
        hashMap.put(key_work_start, work_start);
        hashMap.put(key_incidents_sync_to_aiops, incidents_sync_to_aiops);

        return hashMap;
    }

    @Override
    public String toString() {
        HashMap<String, Object> map = getHashMap();
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, Object> set : map.entrySet()) {
            buffer.append(set.getKey() + " = " + set.getValue() + ", ");
        }
        String result = buffer.toString();
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }
}