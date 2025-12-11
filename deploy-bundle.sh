#!/bin/bash

# CP4AIOps Bundle Deployment Script
# This script replaces variables in YAML files, builds and pushes the image, and deploys to OpenShift

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to prompt for input with validation
prompt_input() {
    local prompt="$1"
    local var_name="$2"
    local validation_func="$3"
    local value
    
    while true; do
        read -p "$prompt: " value
        if [[ -n "$value" ]]; then
            if [[ -z "$validation_func" ]] || $validation_func "$value"; then
                eval "$var_name='$value'"
                break
            fi
        else
            print_error "Value cannot be empty. Please try again."
        fi
    done
}

# Validation functions
validate_lowercase() {
    if [[ "$1" =~ ^[a-z0-9-]+$ ]]; then
        return 0
    else
        print_error "Bundle name must be lowercase and contain only letters, numbers, and hyphens."
        return 1
    fi
}

validate_image_name() {
    if [[ "$1" =~ ^[a-zA-Z0-9._/-]+$ ]]; then
        return 0
    else
        print_error "Image name contains invalid characters."
        return 1
    fi
}

validate_tag() {
    if [[ "$1" =~ ^[a-zA-Z0-9._-]+$ ]]; then
        return 0
    else
        print_error "Image tag contains invalid characters."
        return 1
    fi
}

# Function to check if required tools are available
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    local missing_tools=()
    
    if ! command -v podman &> /dev/null; then
        missing_tools+=("podman")
    fi
    
    if ! command -v oc &> /dev/null; then
        missing_tools+=("oc (OpenShift CLI)")
    fi
    
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        exit 1
    fi
    
    print_success "All prerequisites are available"
}

# Function to check OpenShift login status
check_oc_login() {
    print_info "Checking OpenShift login status..."
    
    if ! oc whoami &> /dev/null; then
        print_error "You are not logged into OpenShift. Please run 'oc login' first."
        exit 1
    fi
    
    local current_user=$(oc whoami)
    local current_server=$(oc whoami --show-server)
    print_success "Logged in as: $current_user"
    print_info "Server: $current_server"
}

# Function to collect user input
collect_input() {
    print_info "Please provide the following information:"
    echo
    
    print_info "Bundle Configuration:"
    prompt_input "Enter the CP4AIOps namespace" "CUSTOMNAMESPACE"
    prompt_input "Enter the bundle name (must be lowercase and alphanumeric)" "CUSTOMBUNDLENAME" "validate_lowercase"
    prompt_input "Enter the bundle image name (example: docker.io/example/test-connector-bundle)" "CUSTOMBUNDLEIMAGENAME" "validate_image_name"
    prompt_input "Enter the bundle image tag (example: 0.0.1)" "CUSTOMBUNDLEIMAGETAG" "validate_tag"
    
    echo
    print_info "Connector Configuration:"
    prompt_input "Enter the connector name (must be lowercase, dashes allowed)" "CUSTOMCONNECTORNAME" "validate_lowercase"
    prompt_input "Enter the connector display name" "CUSTOMCONNECTORDISPLAYNAME"
    prompt_input "Enter the connector image name (example: docker.io/example/test-connector)" "CUSTOMCONNECTORIMAGENAME" "validate_image_name"
    prompt_input "Enter the connector image tag (example: 0.0.1)" "CUSTOMCONNECTORIMAGETAG" "validate_tag"
    
    echo
    print_info "Configuration summary:"
    echo "  Namespace: $CUSTOMNAMESPACE"
    echo "  Bundle Name: $CUSTOMBUNDLENAME"
    echo "  Bundle Image: $CUSTOMBUNDLEIMAGENAME:$CUSTOMBUNDLEIMAGETAG"
    echo "  Connector Name: $CUSTOMCONNECTORNAME"
    echo "  Connector Display Name: $CUSTOMCONNECTORDISPLAYNAME"
    echo "  Connector Image: $CUSTOMCONNECTORIMAGENAME:$CUSTOMCONNECTORIMAGETAG"
    echo
    
    read -p "Continue with these settings? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        print_info "Deployment cancelled by user."
        exit 0
    fi
}

# Function to replace variables in files
replace_variables() {
    print_info "Replacing variables in YAML files..."
    
    local files=(
        "ticket-bundle/deploy/bundlemanifest.yaml"
        "ticket-bundle/deploy/certificate.yaml"
        "ticket-bundle/deploy/deploy.yaml"
        "ticket-bundle/deploy/service.yaml"
        "ticket-bundle/bundles/connectors/catalog/custom.yaml"
        "ticket-bundle/bundles/connectors/custom/prereqs/microedgeconfiguration.yaml"
        "ticket-bundle/bundles/connectors/custom/prereqs/connectorschema.yaml"
        "ticket-bundle/bundles/connectors/custom/prereqs/kustomization.yaml"
        "ticket-bundle/bundles/connectors/custom/connector/deployment.yaml"
        "ticket-bundle/bundles/connectors/custom/connector/kustomization.yaml"
        "ticket-bundle/bundles/connectors/custom/connector/service.yaml"
        "ticket-bundle/bundles/connectors/custom/connector/serviceaccount.yaml"
        "ticket-bundle/bundles/connectors/custom/connector/servicemonitor.yaml"
        "ticket-bundle/bundles/connectors/custom/connector-git-app.yaml"
        "ticket-bundle/bundles/connectors/custom/prereq-git-app.yaml"
    )
    
    # Create backup directory
    local backup_dir="backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$backup_dir"
    
    for file in "${files[@]}"; do
        if [[ -f "$file" ]]; then
            print_info "Processing $file..."
            
            # Create backup with directory structure
            local backup_file="$backup_dir/$file"
            mkdir -p "$(dirname "$backup_file")"
            cp "$file" "$backup_file"
            
            # Replace variables using a safer approach with temporary script
            cat > /tmp/replace_vars_$.pl << 'PERLSCRIPT'
use strict;
use warnings;

my %replacements = (
    'CUSTOMNAMESPACE' => $ENV{'CUSTOMNAMESPACE'},
    'CUSTOMBUNDLENAME' => $ENV{'CUSTOMBUNDLENAME'},
    'CUSTOMBUNDLEIMAGENAME' => $ENV{'CUSTOMBUNDLEIMAGENAME'},
    'CUSTOMBUNDLEIMAGETAG' => $ENV{'CUSTOMBUNDLEIMAGETAG'},
    'CUSTOMCONNECTORNAME' => $ENV{'CUSTOMCONNECTORNAME'},
    'CUSTOMCONNECTORDISPLAYNAME' => $ENV{'CUSTOMCONNECTORDISPLAYNAME'},
    'CUSTOMCONNECTORIMAGENAME' => $ENV{'CUSTOMCONNECTORIMAGENAME'},
    'CUSTOMCONNECTORIMAGETAG' => $ENV{'CUSTOMCONNECTORIMAGETAG'},
);

while (<>) {
    foreach my $key (keys %replacements) {
        my $value = quotemeta($replacements{$key});
        s/$key/$replacements{$key}/g;
    }
    print;
}
PERLSCRIPT
            
            # Export variables for Perl script
            export CUSTOMNAMESPACE CUSTOMBUNDLENAME CUSTOMBUNDLEIMAGENAME CUSTOMBUNDLEIMAGETAG
            export CUSTOMCONNECTORNAME CUSTOMCONNECTORDISPLAYNAME CUSTOMCONNECTORIMAGENAME CUSTOMCONNECTORIMAGETAG
            
            # Run the Perl script
            perl /tmp/replace_vars_$.pl "$file" > "$file.new"
            mv "$file.new" "$file"
            rm -f /tmp/replace_vars_$.pl
            
            # Remove temporary file
            rm -f "$file.tmp"
            
            print_success "Updated $file"
        else
            print_warning "File not found: $file"
        fi
    done
    
    print_success "Variable replacement completed. Backups saved in: $backup_dir"
}

# Function to build and push image
build_and_push_image() {
    print_info "Building and pushing container image..."
    
    local full_image="$CUSTOMBUNDLEIMAGENAME:$CUSTOMBUNDLEIMAGETAG"
    
    # Check if Dockerfile exists
    if [[ ! -f "ticket-bundle/Dockerfile" ]]; then
        print_error "Dockerfile not found at ticket-bundle/Dockerfile"
        exit 1
    fi
    
    # Build the image
    print_info "Building image: $full_image"
    if podman build -t "$full_image" ticket-bundle/; then
        print_success "Image built successfully"
    else
        print_error "Failed to build image"
        exit 1
    fi
    
    # Push the image
    print_info "Pushing image: $full_image"
    if podman push "$full_image"; then
        print_success "Image pushed successfully"
    else
        print_error "Failed to push image"
        exit 1
    fi
}

# Function to deploy to OpenShift
deploy_to_openshift() {
    print_info "Deploying to OpenShift..."
    
    # Switch to the target namespace
    print_info "Switching to namespace: $CUSTOMNAMESPACE"
    if oc project "$CUSTOMNAMESPACE" &> /dev/null; then
        print_success "Switched to namespace: $CUSTOMNAMESPACE"
    else
        print_error "Failed to switch to namespace: $CUSTOMNAMESPACE"
        print_info "Please ensure the namespace exists and you have access to it."
        exit 1
    fi
    
    # Apply the deployment files in order
    local deploy_files=(
        "ticket-bundle/deploy/certificate.yaml"
        "ticket-bundle/deploy/service.yaml"
        "ticket-bundle/deploy/deploy.yaml"
    )
    
    for file in "${deploy_files[@]}"; do
        if [[ -f "$file" ]]; then
            print_info "Applying $file..."
            if oc apply -f "$file"; then
                print_success "Applied $file"
            else
                print_error "Failed to apply $file"
                exit 1
            fi
        fi
    done
    
    # Ask user about applying bundlemanifest.yaml
    echo
    print_warning "The bundlemanifest.yaml file is ready to be applied."
    print_info "This will create the BundleManifest resource in your cluster."
    echo
    read -p "Do you want to apply bundlemanifest.yaml now? (y/N): " apply_bundle
    
    if [[ "$apply_bundle" =~ ^[Yy]$ ]]; then
        print_info "Applying bundlemanifest.yaml..."
        if oc apply -f "ticket-bundle/deploy/bundlemanifest.yaml"; then
            print_success "BundleManifest applied successfully"
        else
            print_error "Failed to apply bundlemanifest.yaml"
            exit 1
        fi
    else
        print_info "Skipping bundlemanifest.yaml application."
        print_info "To apply it later, run:"
        echo "  oc apply -f ticket-bundle/deploy/bundlemanifest.yaml"
    fi
}

# Function to show monitoring commands
show_monitoring_commands() {
    echo
    print_success "Deployment completed successfully!"
    echo
    print_info "To monitor the deployment status, use the following commands:"
    echo
    echo "Bundle Monitoring:"
    echo "1. Check bundle deployment status:"
    echo "   oc get deployment $CUSTOMBUNDLENAME"
    echo
    echo "2. Check bundle pod status:"
    echo "   oc get pods -l app=$CUSTOMBUNDLENAME"
    echo
    echo "3. View bundle deployment logs:"
    echo "   oc logs deployment/$CUSTOMBUNDLENAME"
    echo
    echo "4. Check bundle service status:"
    echo "   oc get service $CUSTOMBUNDLENAME"
    echo
    echo
    echo "Connector Monitoring:"
    echo "5. Check ConnectorSchema:"
    echo "   oc get connectorschema $CUSTOMCONNECTORNAME -o yaml"
    echo
    echo "6. Check MicroEdgeConfiguration:"
    echo "   oc get microedgeconfiguration $CUSTOMCONNECTORNAME -o yaml"
    echo
    echo "7. Check connector deployment:"
    echo "   oc get deployment $CUSTOMCONNECTORNAME"
    echo
    echo "8. Check connector pods:"
    echo "   oc get pods -l app=$CUSTOMCONNECTORNAME"
    echo
    echo "9. View connector logs:"
    echo "   oc logs deployment/$CUSTOMCONNECTORNAME"
    echo
    echo "10. Check connector service:"
    echo "   oc get service $CUSTOMCONNECTORNAME"
}

# Main execution
main() {
    echo "=========================================="
    echo "  CP4AIOps Bundle Deployment Script"
    echo "=========================================="
    echo
    
    check_prerequisites
    check_oc_login
    collect_input
    replace_variables
    build_and_push_image
    deploy_to_openshift
    show_monitoring_commands
}

# Run main function
main "$@"

