

# JSONImporter
## Modify the Mapping
[JSONImporter.java](src/main/java/com/ibm/aiops/connectors/importer/JSONImporter.java) to match your data source's mapping.

Modify [sample.json](src/main/resources/sample.json) to include your sample data.

## Port Forward Elastic
Login to OpenShift

In one terminal, run the following:
```bash
export EL_USER=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export EL_PWD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
kubectl port-forward iaf-system-elasticsearch-es-aiops-0 9201:9200
```

## Working with Elastic
On the second terminal, run the following:
```bash
export EL_USER=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export EL_PWD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
curl -X POST --user $EL_USER:$EL_PWD https://localhost:9201/snowincident/_search -k
```

You can see if anything was put into the incident index (`snowincident`) before. If there are entries, this can affect your similar incident query.
```bash
curl -X DELETE --user $EL_USER:$EL_PWD https://localhost:9201/snowincident -k
```

If you had previously trained AI models, run:
```bash
curl -X DELETE --user $EL_USER:$EL_PWD https://localhost:9201/normalized-incidents-1000-1000 -k
```

If successful, you will see:
```bash
{"acknowledged":true}
```

## Running The Application
For running this Java program:
```bash
mvn install
export DIRECT_TO_SEARCH_PASSWORD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
export DIRECT_TO_SEARCH_USERNAME=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export DIRECT_TO_SEARCH_PORT="9201"
export DIRECT_TO_SEARCH_HOSTNAME="127.0.0.1"
mvn exec:java -Dexec.mainClass="com.ibm.aiops.connectors.importer.JSONImporter"
```

An example output is:
```bash
Valid incidents to insert: 10
```

Verify by calling:
```bash
curl -X POST --user $EL_USER:$EL_PWD https://localhost:9201/snowincident/_count -k
{"count":10,"_shards":{"total":1,"successful":1,"skipped":0,"failed":0}}%    
```

## Training and Testing Similar Incident
Train the AI model for Similar Incident and the sample will be successful. Only closed incidents are examined.
![AI model](images/aimodeltrain.png)

In a terminal window, run:
```bash
for i in {1..1000}; do oc port-forward svc/$(oc get svc | grep similar-incidents | awk '{print $1;}') 8000; done
```

In a second terminal, run the following, but replace `YOUR_USERNAME_HERE` and `YOUR_PASSWORD_HERE`:
```bash
ZENURL=`oc get route cpd -o json | jq -r .spec.host`
TOKEN=`curl -ks https://$ZENURL/icp4d-api/v1/authorize -H 'Content-Type: application/json' -d '{"username": "YOUR_USERNAME_HERE", "password": "YOUR_PASSWORD_HERE"}'`
export TOKEN=`echo $TOKEN | jq '.token'`
TOKEN=`echo "$TOKEN" | tr -d '"'`
echo $TOKEN 
```

Test similar incident (modify the `text` in the `curl` command to test different incident texts)
```bash
curl --location 'https://127.0.0.1:8000/v2/similar_incidents/search' \
-H 'Content-Type: application/json' \
-H "Authorization: Bearer $TOKEN" \
-d '{
  "story_id": "1",
  "application_id": "1000",
  "application_group_id": "1000",
  "text": "Server not running"
}' -k

```

You will get the output (made pretty for readability):
```json
{
    "similar_incidents": [
        {
            "title": "Server not running.",
            "incident_id": "1f0f3686-ba2a-444a-b3d2-c3e01047f638",
            "source_incident_id": "INCIDENTID02",
            "url": "https://example.com/incident/INCIDENTID02",
            "explanation": "Based on similarity from query and incidents",
            "resolution": "Fixed manually by restarting the server several times",
            "score": 4.5393684e-31,
            "started_at": "1970-01-12T05:20:54+00:00",
            "closed_by": "",
            "actions": [
                {
                    "action": {
                        "text": "restarting",
                        "begin_offset": 18,
                        "end_offset": 28
                    },
                    "components": [
                        {
                            "text": "server",
                            "begin_offset": 33,
                            "end_offset": 39
                        }
                    ]
                }
            ]
        },
        {
            "title": "Server is frozen",
            "incident_id": "9a09346e-fcac-45bc-9e07-0ac96c21f823",
            "source_incident_id": "INCIDENTID07",
            "url": "https://example.com/incident/INCIDENTID07",
            "explanation": "Based on similarity from query and incidents",
            "resolution": "Force shutdown the server and let it sit for 5 minutes before turning it back on",
            "score": 9.6339497e-32,
            "started_at": "1970-01-12T05:20:54+00:00",
            "closed_by": "",
            "actions": []
        },
        {
            "title": "Server under denial of service attack",
            "incident_id": "4d7f18b7-67da-4ed4-a315-65ad115d0840",
            "source_incident_id": "INCIDENTID10",
            "url": "https://example.com/incident/INCIDENTID10",
            "explanation": "Based on similarity from query and incidents",
            "resolution": "Blocked problematic IPs via filter to lighten the load",
            "score": 6.759733e-32,
            "started_at": "1970-01-12T05:20:54+00:00",
            "closed_by": "",
            "actions": []
        }
    ],
    "recommended_actions": [
        {
            "incident_id": "1f0f3686-ba2a-444a-b3d2-c3e01047f638",
            "sentence": "Fixed manually by restarting the server several times",
            "actions": [
                {
                    "action": {
                        "text": "restarting",
                        "begin_offset": 18,
                        "end_offset": 28
                    },
                    "components": [
                        {
                            "text": "server",
                            "begin_offset": 33,
                            "end_offset": 39
                        }
                    ]
                }
            ]
        }
    ]
}
```

# JSONImporterChangeRequest

## Port Forward Elastic
Login to OpenShift

In one terminal, run the following:
```bash
export EL_USER=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export EL_PWD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
kubectl port-forward iaf-system-elasticsearch-es-aiops-0 9201:9200
```

## Working with Elastic
On the second terminal, run the following:
```bash
export EL_USER=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export EL_PWD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
curl -X POST --user $EL_USER:$EL_PWD https://localhost:9201/snowchangerequest/_search -k
```

You can see if anything was put into the incident index (`snowchangerequest`) before. If there are entries, this can affect your similar incident query.
```bash
curl -X DELETE --user $EL_USER:$EL_PWD https://localhost:9201/snowchangerequest -k
```

If successful, you will see:
```bash
{"acknowledged":true}
```

## Running The Application
For running this Java program:
```bash
mvn install
export DIRECT_TO_SEARCH_PASSWORD=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.password|base64decode}}"`
export DIRECT_TO_SEARCH_USERNAME=`oc get secret iaf-system-elasticsearch-es-default-user -o go-template --template="{{.data.username|base64decode}}"`
export DIRECT_TO_SEARCH_PORT="9201"
export DIRECT_TO_SEARCH_HOSTNAME="127.0.0.1"
mvn exec:java -Dexec.mainClass="com.ibm.aiops.connectors.importer.JSONImporterChangeRequest"
```

An example output is:
```bash
Valid change requests to insert: 15
```

Verify by calling:
```bash
curl -X POST --user $EL_USER:$EL_PWD https://localhost:9201/snowchangerequest/_count -k
{"count":15,"_shards":{"total":1,"successful":1,"skipped":0,"failed":0}}%    
```

You can query for a particular record (for example, querying `sysid02` for aggregated close notes):
```bash
curl -X GET --user $EL_USER:$EL_PWD https://localhost:9201/snowchangerequest/_search --header 'Content-Type: application/json' --data '{"query" : {"match" : { "sys_id" : "sysid02" }}}' -k 
```
