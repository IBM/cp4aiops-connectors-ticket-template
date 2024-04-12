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
![Integration Table](images/github-template/integration5.png)
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

![Policy 1](images/github/policy1.png)

Click on Save button
![Policy 2](images/github/policy2.png)
Check if the specification have the added github integration
![Policy 3](images/github/policy.png)

Make sure you set the event count increasing as a reason to generate an incident. Otherwise you need to modify the code to generate a new alert

## Incident Creation
Trigger Incident and see the details in incident details page
![Incident](images/github/Incident1.png)

See the similar tickets in the past resolution tickets.

Click on the Github Link that mentioned in the right side box to see the Github issue.
![Github](images/github/github1.png)
