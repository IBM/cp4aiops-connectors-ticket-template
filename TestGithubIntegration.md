# Github Integration Template

## Testing
### Create The Github Integration
![Add Integration](images/github-template/Integration.png)

Search for `Github` to add your newly added integration.
![Search Integration](images/github-template/github-template-integration.png)
![Search Integration](images/github-template/integration2.png)

You can fill in any valid data.

![Integration 1](images/github-template/integration3.png)
![Integration 2](images/github-template/integration4.png)
![Integration 3](images/github-template/integration5.png)

Press `Done`
![Integration Table](images/github-template/integration-instance-running.png)
`Running` indicates that it is configured properly


### Train the AI for Similar Incident and Change Risk
Go to the AI management page

![AI Training](images/TrainAI01.png)

Click on Similiar Incident. Go through the wizard with the defaults. Then precheck the data, train models, and deploy. There should be no errors.
![AI Training](images/TrainAI02.png)


### Policy Creation

To create a policy that will create an incident based on the classification, go to the automations page, click `Create policy`

![Policy](images/Policy01.png)

Choose `Promote alerts to an incident`

![Policy](images/Policy02.png)

Fill in the details and add github integration in the ticket section.

![Policy 1](images/github-template/policy1.png)

Click on Save button
![Policy 2](images/github-template/policy2.png)
Check if the specification have the added github integration
![Policy 3](images/github-template/policy.png)

Make sure you set the event count increasing as a reason to generate an incident. Otherwise you need to modify the code to generate a new alert

## Incident Creation

Create a Webhook in Integration page
Search for webhook and select Generic Webhook
![Webhook 1](images/incidentcreation/webhook1.png)
Click on Get Started 
![Webhook 2](images/incidentcreation/webhook2.png)
Give it some name and choose none as authentication. You can provide authentication if you would to here but you should pass the credentials when using curl command.
![Webhook 3](images/incidentcreation/webhook3.png)
Click on Load Sample Mapping
![Webhook 4](images/incidentcreation/webhook4.png)
Select IBM Sev One from the dropdown and hit on Load Sample Mapping Button 
![Webhook 5](images/incidentcreation/webhook5.png)
Give it some time and you will get to see the route. Copy the webhook route
![Webhook 6](images/incidentcreation/webhook6.png)

Change the titles in sevone-payload.json file in `/sample-scripts`
Be in this folder `cd sample-scripts/sevone-payload.json file` or the path where you have your sevone-payload.json file
Login to the cluster 
`oc login https://api.cluster.cp.fyre.ibm.com:port -u username -p password -n aiops --insecure-skip-tls-verify=true`

`WEBHOOK_URL=paste webhook url here
curl -X POST --insecure \
-H 'Content-Type: application/json' ${WEBHOOK_URL} \
-d @./sevone-payload.json`


Trigger Incident and see the details in incident details page
![Incident](images/github-template/Incident1.png)

See the similar tickets in the past resolution tickets.

Click on the Github Link that mentioned in the right side box to see the Github issue.
![Github](images/github-template/github1.png)
