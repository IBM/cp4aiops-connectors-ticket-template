# Github Integration Template

This page provides a complete end to end scenario to get the github integration template `github-grpc-connector-template` running in CP4AIOps.

# Prerequisites
- Podman (https://podman.io/docs/installation) or Rancher Desktop (https://rancherdesktop.io/)
- CPAIOps installed
- Docker image repository (ex: https://hub.docker.com)

# Development Environment Setup
1. Fork the repo: https://github.com/IBM/cp4aiops-connectors-ticket-template
This operation clones the repository and all its branches into your account.

2. Clone Your Fork to your local machine.
Replace `YourUsername` with your GitHub username
```bash
git clone https://github.com/YourUsername/cp4aiops-connectors-github-template.git
```
3. Checkout to the `github-connector-sample` branch

```bash
cd cp4aiops-connectors-github-template
git checkout -b github-connector-sample origin/github-connector-sample
```

 For example, the new repo I forked is in: https://github.com/cookcm/cp4aiops-connectors-github-template
1. Start podman 
    ```bash
    podman machine start
    ```
    Or start rancher desktop with docker daemon enabled via the user interface
1. Login to Docker repository, for example:
    ```bash
    docker login docker.io/<owner>
    ```
1. Build the image by calling the following command from the root of the project directory. Choose between podman or docker based on what you installed. For the build tag, use the Docker image location you had previously logged into.
    ```
    <podman or docker> build -f container/Dockerfile -t docker.io/<YourUsername>/sample-github-template:latest .
    ```
   
1. While the image is building (it can take several minutes), the template code requires some modifications for it to run. Begin by updating the GitHub location. Open [bundlemanifest.yaml](bundlemanifest.yaml)
1. Update the `repo` and `branch` to match your own location. 
   ```yaml
   apiVersion: connectors.aiops.ibm.com/v1beta1
    kind: BundleManifest
    metadata:
    name: github-grpc-connector-template
    spec:
    prereqs:
        repo: 'https://github.com/<YourUsername>/cp4aiops-connectors-github-template'
        branch: github-connector-sample
        authSecret:
        name: test-utilities-github-token
        components:
        - name: deployment
            path: /bundle-artifacts/prereqs
            type: kustomize
    instanced:
        repo: 'https://github.com/<YourUsername>/cp4aiops-connectors-github-template'
        branch: github-connector-sample
        authSecret:
        name: test-utilities-github-token
        components:
        - name: connector
            path: /bundle-artifacts/connector
            type: kustomize
   ```

   Later in CPAIOps, this repository will be retrieved using the secret `test-utilities-github-token`. You will need to create that [secret](#secret) in CPAIOps with your Git token's credentials. If you'd like to rename this secret to something else, you can do that here too.

   In CPAIOps, this repository will be loaded and the directories [/bundle-artifacts/prereqs](/bundle-artifacts/prereqs) and [/bundle-artifacts/connector](/bundle-artifacts/connector) will have the yaml files deployed.

   As part of the deployment, the image that is being built will be defined here.
1. If your image was successfully built, you'll see a message like:
   ```bash
   [2/2] COMMIT docker.io/<YourUsername>/sample-github-template:latest
    --> 9ee0cd654153
    Successfully tagged docker.io/<YourUsername>/sample-github-template:latest
    9ee0cd654153939823c8e5a896e17c33e4b5c81d827ce44a64c88b52169d10f8
   ```

   Next, push the image via the command:
   ```
   <podman or docker> push docker.io/<YourUsername>/sample-github-template:latest
   ```
1. Update the image addresses in the Bundlemanifest files. In [/bundle-artifacts/prereqs/kustomization.yaml](/bundle-artifacts/prereqs/kustomization.yaml) and [/bundle-artifacts/connector/kustomization.yaml](/bundle-artifacts/connector/kustomization.yaml) replace the PLACEHOLDER_REGISTRY_ADDRESS with the path to your image in the docker repository.
   ```yaml
   newName: PLACEHOLDER_REGISTRY_ADDRESS/cp/aiopsedge/github-grpc-connector-template
   newTag: latest
   ```
   
   with
   ```yaml
    newName: docker.io/<YourUsername>/sample-github-template
    newTag: latest
   ```

   If your tag is not `latest`, update `newTag` as needed


1. Commit the changes into GitHub into the `github-connector-sample` branch so the `bundlemanifest.yaml` will pickup the changes
1. Next, prepare the OpenShift cluster to pull from the GitHub repository. Ensure you generate a GitHub token that can read from your code repository
     
    <a name="secret"></a>
    ```bash
    oc create secret generic test-utilities-github-token --from-literal=username=<GitHub Username> --from-literal=password=<GitHub access token>
    ```

1. In the OpenShift Console, you can create an image pull secret. Create the name as `ibm-aiops-pull-secret` and put in your Docker information:
![Secret](image/../images/image-pull-secret.png)

1. Now to deploy the BundleManifest
    ```bash
    oc apply -f bundlemanifest.yaml
    ```

1. Check if the connector was successfully configured (this may take a few seconds):
    ```bash
    oc get BundleManifest | grep github-grpc-connector-template
    github-grpc-connector-template    Configured
    ```

1. Restart the `connections-ui` pod forcefully, or you can wait 5-10 minutes for it to auto refresh. Don't kill this pod if other people are using CP4AIOps:

    ```bash
    oc get pods | grep connections-ui
    aiops-connections-ui-57dc845f75-zls5c                             1/1     Running                  0               40h

    oc delete pod aiops-connections-ui-57dc845f75-zls5c
    ```

1. In the CP4AIOps UI, you will now see an integration for `Github gRPC Connector Template`
    ![integration](images/github-template/github-template-integration.png)

# Development with Maven + Generating Images
So how do you use maven in all of this?

You can make modifications to the connector. For example, you can modify [TicketConnector.java](src/main/java/com/ibm/aiops/connectors/template/TicketConnector.java). You can also modify the test cases to add new unit tests.

To verify the build and tests work, you would run:
```bash
mvn install
```

To build an updated image, you would do (replace with your own image repository):
```bash
<podman or docker> build -f container/Dockerfile -t docker.io/<YourUsername>/sample-github-template:latest .
<podman or docker>  push docker.io/<YourUsername>/sample-github-template:latest
```

Once the image is pushed to your repository, then you can restart the pod:
```bash
oc get pods | grep github-grpc-connector-template
github-grpc-connector-template-49445af7-ef2a-4692-b564-636a354bgq   1/1     Running                  0              12m
oc delete pod github-grpc-connector-template-49445af7-ef2a-4692-b564-636a354bgq
```

The new pod that starts up will pull the latest image.

# Test the Github Integration Template

Open [TestGithubIntegration.md](TestGithubIntegration.md)

# Development with Maven + Local deployment

In the OpenShift console, go to the `ConnectorConfiguration`. Go to `instances` and look for `githubtemplate`

![ConnectorConfiguration](images/github-template/connectorConfiguration-yaml.png)

The `uuid` is `537328bf-192a-457d-b0c7-9ebc41641c9a`

To find the information needed in src/main/liberty/config/bootstrap.properties, if you want to run teh github-template locally connected to your remote cluster, connect to your cluster and then run the following script:

```
#!/bin/bash

CONNECTOR_UID=537328bf-192a-457d-b0c7-9ebc41641c9aA

CONNECTOR_BRIDGE_SECRET=$(oc get secret connector-bridge-connection-info -o json)
CONNECTOR_OAUTH_SECRET=$(oc get secret "connector-${CONNECTOR_UID}" -o json)

CONNECTOR_BRIDGE_SERVER_CA=$(echo "${CONNECTOR_BRIDGE_SECRET}" | jq -r '.data["ca.crt"]' | base64 --decode)
CONNECTOR_BRIDGE_CLIENT_CA=$(echo "${CONNECTOR_BRIDGE_SECRET}" | jq -r '.data["tls.crt"]' | base64 --decode)
CONNECTOR_BRIDGE_CLIENT_KEY=$(echo "${CONNECTOR_BRIDGE_SECRET}" | jq -r '.data["tls.key"]' | base64 --decode)
CONNECTOR_BRIDGE_HOST=$(echo "${CONNECTOR_BRIDGE_SECRET}" | jq -r '.data["external-host"]' | base64 --decode)
CONNECTOR_BRIDGE_PORT=$(echo "${CONNECTOR_BRIDGE_SECRET}" | jq -r '.data["external-port"]' | base64 --decode)


echo -e " Server CA (place in /tmp/connector_ca.crt) " 
echo "${CONNECTOR_BRIDGE_SERVER_CA}"
echo ""

echo -e "Client CA (place in /tmp/connector_tls.crt)"
echo "${CONNECTOR_BRIDGE_CLIENT_CA}"
echo ""

echo -e "Client key (place in /tmp/connector_tls.key"
echo "${CONNECTOR_BRIDGE_CLIENT_KEY}"
echo ""


echo -e "Dev connector configuration (place in src/main/liberty/config/bootstrap.properties)"
cat <<EOF
# These settings are developer setting, they will not be used in the produced docker file
grpc-bridge.host=${CONNECTOR_BRIDGE_HOST}
grpc-bridge.port=${CONNECTOR_BRIDGE_PORT}
grpc-bridge.server-certificate-file="/tmp/connector_ca.crt"
grpc-bridge.client-certificate-file="/tmp/connector_tls.crt"
grpc-bridge.client-private-key-file="/tmp/connector_tls.key"
grpc-bridge.id="${CONNECTOR_UID}"

grpc-bridge.client-id="${CONNECTOR_OAUTH_CLIENT_ID}"
grpc-bridge.client-secret="${CONNECTOR_OAUTH_CLIENT_SECRET}"
EOF

```

Create a folder called `certs` in the root directory.
Copy the properties found above:
- Copy `caCertificate` property into `certs/ca.crt` (everything starting from `-----BEGIN CERTIFICATE-----` and ending with `-----END CERTIFICATE-----`)
- Copy `tlscrt` property into `certs/tls.crt` (everything starting from `-----BEGIN CERTIFICATE-----` and ending with `-----END CERTIFICATE-----`)
- Copy `tlskey` property into `certs/tls.key` (everything starting from `-----BEGIN RSA PRIVATE KEY-----` and ending with `-----END RSA PRIVATE KEY-----`)

Replace the correct information in the src/main/liberty/config/bootstrap.properties file:

For example (I hid the host):

```
# These settings are developer setting, they will not be used in the produced docker file

# From the secret connector-bridge-connection-info
grpc-bridge.host=HOSTHIDDEN
grpc-bridge.port=443
# When run locally, this file is copied to <PROJECT ROOT>/target/liberty/wlp/usr/servers/defaultServer
grpc-bridge.server-certificate-file="../../../../../../certs/ca.crt"
grpc-bridge.client-certificate-file="../../../../../../certs/tls.crt"
grpc-bridge.client-private-key-file="../../../../../../certs/tls.key"

# From the secret connector-local-orchestrator
grpc-bridge.client-id="782cb888-b565-4e0b-be17-5d08ad16d680"
grpc-bridge.client-secret="qO6ljMDvAUVtCe6IamSKmTj2Sr3Im6SYYqdSFeDW2XEoQazWEFKtKj5WA8fN"

# The UUID of the Service Now ConnectorConfiguration
connector-template.id="782cb888-b565-4e0b-be17-5d08ad16d680"

com.ibm.ws.logging.console.format=simple#json
com.ibm.ws.logging.console.source=message,trace
com.ibm.ws.logging.console.log.level=info
com.ibm.ws.logging.trace.file.name=stdout
com.ibm.ws.logging.trace.format=BASIC
com.ibm.ws.logging.trace.specification="*=warning:com.ibm.aiops.connectors.*=all"
```



Next go the `Deployments` and look for the integration. For example, mine is `github-grpc-connector-template-537328bf-192a-457d-b0c7-9ebc41641c9a` (part of the `uuid` is used as the suffix, the rest is padded by OpenShift, so searching for the first couple of letters of the `uuid` is the easiest way)

Open the yaml and set the `replica` to 0:
```yaml
spec:
  replicas: 0
```

For the gRPC bridge and connector communication, only ONE can exist for a particular `uuid`. If you have multiple integrations with the same `uuid`, then the bridge will only connect to one of them. In this case, we want the only connection to be our local connection on our machine for debugging, not the pod running on the cluster.


Now, go to the root of the project and run:
```
mvn liberty:run
```

This is how we do development on these connectors, since you don't need to generate an image each time. You can make changes, then do `mvn liberty:run` to pickup the new changes. To terminate it, you can end the process. If it's stuck running, you can do `mvn liberty:stop`. 