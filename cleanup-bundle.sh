#!/bin/bash

# CP4AIOps Bundle Cleanup Script
# This script removes deployed resources from OpenShift

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

# Function to prompt for input
prompt_input() {
    local prompt="$1"
    local var_name="$2"
    local value
    
    read -p "$prompt: " value
    if [[ -n "$value" ]]; then
        eval "$var_name='$value'"
    else
        print_error "Value cannot be empty."
        exit 1
    fi
}

# Function to check OpenShift login status
check_oc_login() {
    print_info "Checking OpenShift login status..."
    
    if ! command -v oc &> /dev/null; then
        print_error "oc (OpenShift CLI) is not installed"
        exit 1
    fi
    
    if ! oc whoami &> /dev/null; then
        print_error "You are not logged into OpenShift. Please run 'oc login' first."
        exit 1
    fi
    
    local current_user=$(oc whoami)
    local current_server=$(oc whoami --show-server)
    print_success "Logged in as: $current_user"
    print_info "Server: $current_server"
}

# Function to collect cleanup information
collect_cleanup_info() {
    print_info "Please provide the following information:"
    echo
    
    prompt_input "Enter the CP4AIOps namespace" "NAMESPACE"
    prompt_input "Enter the bundle name to cleanup" "BUNDLE_NAME"
    
    echo
    print_warning "This will delete the following resources in namespace '$NAMESPACE':"
    echo "  - BundleManifest: $BUNDLE_NAME"
    echo "  - Certificate: $BUNDLE_NAME"
    echo "  - Service: $BUNDLE_NAME"
    echo "  - Deployment: $BUNDLE_NAME"
    echo "  - Secret: $BUNDLE_NAME-cert-secret (if exists)"
    echo
    
    read -p "Are you sure you want to proceed? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        print_info "Cleanup cancelled by user."
        exit 0
    fi
}

# Function to cleanup resources
cleanup_resources() {
    print_info "Starting cleanup in namespace: $NAMESPACE"
    
    # Switch to the target namespace
    if oc project "$NAMESPACE" &> /dev/null; then
        print_success "Switched to namespace: $NAMESPACE"
    else
        print_error "Failed to switch to namespace: $NAMESPACE"
        print_info "Please ensure the namespace exists and you have access to it."
        exit 1
    fi
    
    local resources_deleted=0
    local resources_not_found=0
    
    # Delete BundleManifest
    print_info "Deleting BundleManifest: $BUNDLE_NAME"
    if oc delete bundlemanifest "$BUNDLE_NAME" --ignore-not-found=true 2>/dev/null; then
        if oc get bundlemanifest "$BUNDLE_NAME" &> /dev/null; then
            resources_not_found=$((resources_not_found + 1))
            print_warning "BundleManifest not found: $BUNDLE_NAME"
        else
            resources_deleted=$((resources_deleted + 1))
            print_success "Deleted BundleManifest: $BUNDLE_NAME"
        fi
    else
        resources_not_found=$((resources_not_found + 1))
        print_warning "BundleManifest not found: $BUNDLE_NAME"
    fi
    
    # Delete Deployment
    print_info "Deleting Deployment: $BUNDLE_NAME"
    if oc delete deployment "$BUNDLE_NAME" --ignore-not-found=true 2>/dev/null; then
        if oc get deployment "$BUNDLE_NAME" &> /dev/null; then
            resources_not_found=$((resources_not_found + 1))
            print_warning "Deployment not found: $BUNDLE_NAME"
        else
            resources_deleted=$((resources_deleted + 1))
            print_success "Deleted Deployment: $BUNDLE_NAME"
        fi
    else
        resources_not_found=$((resources_not_found + 1))
        print_warning "Deployment not found: $BUNDLE_NAME"
    fi
    
    # Delete Service
    print_info "Deleting Service: $BUNDLE_NAME"
    if oc delete service "$BUNDLE_NAME" --ignore-not-found=true 2>/dev/null; then
        if oc get service "$BUNDLE_NAME" &> /dev/null; then
            resources_not_found=$((resources_not_found + 1))
            print_warning "Service not found: $BUNDLE_NAME"
        else
            resources_deleted=$((resources_deleted + 1))
            print_success "Deleted Service: $BUNDLE_NAME"
        fi
    else
        resources_not_found=$((resources_not_found + 1))
        print_warning "Service not found: $BUNDLE_NAME"
    fi
    
    # Delete Certificate
    print_info "Deleting Certificate: $BUNDLE_NAME"
    if oc delete certificate "$BUNDLE_NAME" --ignore-not-found=true 2>/dev/null; then
        if oc get certificate "$BUNDLE_NAME" &> /dev/null; then
            resources_not_found=$((resources_not_found + 1))
            print_warning "Certificate not found: $BUNDLE_NAME"
        else
            resources_deleted=$((resources_deleted + 1))
            print_success "Deleted Certificate: $BUNDLE_NAME"
        fi
    else
        resources_not_found=$((resources_not_found + 1))
        print_warning "Certificate not found: $BUNDLE_NAME"
    fi
    
    # Delete Secret (created by Certificate)
    local secret_name="$BUNDLE_NAME-cert-secret"
    print_info "Deleting Secret: $secret_name"
    if oc delete secret "$secret_name" --ignore-not-found=true 2>/dev/null; then
        if oc get secret "$secret_name" &> /dev/null; then
            resources_not_found=$((resources_not_found + 1))
            print_warning "Secret not found: $secret_name"
        else
            resources_deleted=$((resources_deleted + 1))
            print_success "Deleted Secret: $secret_name"
        fi
    else
        resources_not_found=$((resources_not_found + 1))
        print_warning "Secret not found: $secret_name"
    fi
    
    echo
    print_success "Cleanup completed!"
    echo "  Resources deleted: $resources_deleted"
    echo "  Resources not found: $resources_not_found"
}

# Function to show verification commands
show_verification_commands() {
    echo
    print_info "To verify cleanup, use the following commands:"
    echo
    echo "# Check if bundlemanifest is deleted"
    echo "oc get bundlemanifest $BUNDLE_NAME"
    echo
    echo "# Check if deployment is deleted"
    echo "oc get deployment $BUNDLE_NAME"
    echo
    echo "# Check if service is deleted"
    echo "oc get service $BUNDLE_NAME"
    echo
    echo "# Check if certificate is deleted"
    echo "oc get certificate $BUNDLE_NAME"
    echo
    echo "# Check if secret is deleted"
    echo "oc get secret $BUNDLE_NAME-cert-secret"
    echo
    echo "# List all remaining resources with the bundle name"
    echo "oc get all -l app=$BUNDLE_NAME"
}

# Main execution
main() {
    echo "=========================================="
    echo "  CP4AIOps Bundle Cleanup Script"
    echo "=========================================="
    echo
    
    check_oc_login
    collect_cleanup_info
    cleanup_resources
    show_verification_commands
}

# Run main function
main "$@"
