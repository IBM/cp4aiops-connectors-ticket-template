# Ticket-Template

An example of a ticket template, based off a snapshot of the GitHub connector in CP4AIOps, which is able to:
1. Pull in incident data from GitHub issues
2. Create incidents in GitHub from a CP4AIOps incident

GitHub is the example, since it can be used with public GitHub and the code required to query issues is best understod through an example.

## Setup
### ticket-bundle
The [ticket-bundle](/ticket-bundle) contains configuration files that defines the connector. This bundle is deployed as a gitserver, so the configuration can be picked up by CP4AIOps.

- [prereqs](/ticket-bundle/bundles/connectors/custom/prereqs) - contains the prerequisties for the connector, including the [connnectorschema.yaml](/ticket-bundle/bundles/connectors/custom/prereqs/connectorschema.yaml), which defines what properties the connector needs as input in the CP4AIOps UI and what properties will be passed into the connector.
- [connector](/ticket-bundle/bundles/connectors/custom/connector) - contains the deployment files for the connector
- [Dockerfile](/ticket-bundle/Dockerfile) - the Dockerfile used to deploy the bundles
- [deploy](/ticket-bundle/deploy) - the bundle deployment files. In order for CP4AIOps to trust the git server, a trusted certificate is required to be created

### ticket-connector
The GitHub snapshot for CP4AIOps.

The data source endpoint is queried for incident data, then normalized, and pushed into the OpenSearch database.

In [IssuePollingAction](/ticket-connector/src/main/java/com/ibm/aiops/connectors/template/IssuePollingAction.java), the incidents are pushed into CP4AIOps. This is done with this command:

```java
ticketAction.insertIncident(ticketList);
```

The normalization is done in:

```java
protected void processTickets(JSONObject obj, ArrayList<Ticket> ticketList)
```

## Deployment Instructions

### Customizable properties
The script customizes the following properties. This repository will have references to these variables:
- CUSTOMNAMESPACE: the namespace of the CP4AIOps installation
- CUSTOMBUNDLENAME: the name of the bundle. Must be lowercase
- CUSTOMBUNDLEIMAGENAME: the bundle image name
- CUSTOMBUNDLEIMAGETAG: the bundle image tag
- CUSTOMCONNECTORNAME: the name of the custom connector. Must be lowercase and dashes are allowed
- CUSTOMCONNECTORDISPLAYNAME: the name of the connector in the UI
- CUSTOMCONNECTORIMAGENAME: the custom connector image name
- CUSTOMCONNECTORIMAGETAG: the custom connector image tag

### Prerequisites

Make sure you have the following prerequisites:
- podman installed
- oc (OpenShift CLI) installed
- Logged in to your OpenShift cluster
- On an environment that can build amd64 architecture images. This sample was built on a RedHat linux environment. Ubuntu is another popular alternative

### Build The Integration
1. Run the build connector script:
   ```bash
   ./build-connector.sh
   ```

This connector image will later be used to deploy the integration.

### Deploy The Integration
1. Run the deployment script:
   ```bash
   ./deploy-bundle.sh
   ```

2. Follow the prompts to provide the required information:
   - CP4AIOps namespace
   - Bundle name (must be lowercase)
   - Bundle image name
   - Bundle image tag
   - Connector name (must be lowercase, dashes allowed)
   - Connector display name
   - Connector image name
   - Connector image tag

3. The script will:
   - Replace variables in all YAML files
   - Build and push the container image using podman
   - Deploy to OpenShift
   - Provide monitoring commands to check the deployment status

## Monitoring Commands

After deployment, use these commands to monitor the status:

```bash
# Bundle Monitoring
# Check bundle deployment status
oc get deployment <CUSTOMBUNDLENAME>

# Check bundle pod status
oc get pods -l app=<CUSTOMBUNDLENAME>

# View bundle deployment logs
oc logs deployment/<CUSTOMBUNDLENAME>

# Check bundle service status
oc get service <CUSTOMBUNDLENAME>

# Connector Monitoring
# Check ConnectorSchema
oc get connectorschema <CUSTOMCONNECTORNAME> -o yaml

# Check MicroEdgeConfiguration
oc get microedgeconfiguration <CUSTOMCONNECTORNAME> -o yaml

# Check connector deployment
oc get deployment <CUSTOMCONNECTORNAME>

# Check connector pods
oc get pods -l app=<CUSTOMCONNECTORNAME>

# View connector logs
oc logs deployment/<CUSTOMCONNECTORNAME>

# Check connector service
oc get service <CUSTOMCONNECTORNAME>
```

Replace `<CUSTOMBUNDLENAME>` and `<CUSTOMCONNECTORNAME>` with your actual bundle and connector names.

## Cleanup Instructions

To remove deployed resources from OpenShift:

1. Run the cleanup script:
   ```bash
   ./cleanup-bundle.sh
   ```

2. The script will prompt for:
   - CP4AIOps namespace
   - Bundle name to cleanup

3. Confirm the cleanup when prompted (y/N)

4. The script will remove:
   - BundleManifest
   - Certificate
   - Service
   - Deployment
   - Associated secrets
