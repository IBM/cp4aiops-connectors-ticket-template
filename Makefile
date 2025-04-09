REGISTRY ?= PLACEHOLDER_REGISTRY_ADDRESS
TAG ?= latest

IMAGE := $(REGISTRY)/cp/aiopsedge/cp4waiops-connector-ticket-template:$(TAG)

docker-login:
	docker login $(REGISTRY) -u "$$REGISTRY_USERNAME" -p "$$REGISTRY_PASSWORD"

docker-build:
	chmod ug+x container/import-certs.sh
	docker build -f container/Dockerfile -t $(IMAGE) .

docker-push:
	docker push $(IMAGE)

docker-build-ci:
	chmod ug+x container/import-certs.sh
	docker build -f container/Dockerfile -t $(IMAGE) .

docker-push-ci:
	docker push $(IMAGE)