#!/bin/bash

# Connector Image Build and Push Script
# This script builds the connector image from ticket-connector/container/Dockerfile and pushes it to a registry

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

# Function to check prerequisites
check_prerequisites() {
    print_info "Checking prerequisites..."
    
    if ! command -v podman &> /dev/null; then
        print_error "podman is not installed"
        exit 1
    fi
    
    if [[ ! -f "ticket-connector/container/Dockerfile" ]]; then
        print_error "Dockerfile not found at ticket-connector/container/Dockerfile"
        exit 1
    fi
    
    print_success "All prerequisites are available"
}

# Function to collect input
collect_input() {
    print_info "Please provide the connector image information:"
    echo
    
    prompt_input "Enter the connector image name (example: docker.io/example/test-connector)" "CUSTOMCONNECTORIMAGENAME" "validate_image_name"
    prompt_input "Enter the connector image tag (example: 0.0.1)" "CUSTOMCONNECTORIMAGETAG" "validate_tag"
    
    echo
    print_info "Configuration summary:"
    echo "  Connector Image: $CUSTOMCONNECTORIMAGENAME:$CUSTOMCONNECTORIMAGETAG"
    echo "  Dockerfile: ticket-connector/container/Dockerfile"
    echo "  Build Context: ticket-connector/container"
    echo
    
    read -p "Continue with these settings? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        print_info "Build cancelled by user."
        exit 0
    fi
}

# Function to build and push image
build_and_push_image() {
    local full_image="$CUSTOMCONNECTORIMAGENAME:$CUSTOMCONNECTORIMAGETAG"
    
    print_info "Building connector image..."
    echo
    
    # Build the image
    print_info "Building image: $full_image"
    print_info "Using Dockerfile: ticket-connector/container/Dockerfile"
    print_info "Build context: ticket-connector/container"
    echo
    
    if podman build -t "$full_image" -f ticket-connector/container/Dockerfile ticket-connector; then
        print_success "Image built successfully"
    else
        print_error "Failed to build image"
        exit 1
    fi
    
    echo
    
    # Push the image
    print_info "Pushing image: $full_image"
    if podman push "$full_image"; then
        print_success "Image pushed successfully"
    else
        print_error "Failed to push image"
        exit 1
    fi
}

# Function to show next steps
show_next_steps() {
    echo
    print_success "Connector image build and push completed successfully!"
    echo
    print_info "Next steps:"
    echo "1. Use this image in your deployment:"
    echo "   Image: $CUSTOMCONNECTORIMAGENAME:$CUSTOMCONNECTORIMAGETAG"
    echo
    echo "2. Update your bundle deployment with this connector image"
    echo "   Run: ./deploy-bundle.sh"
    echo
    echo "3. Verify the image is available:"
    echo "   podman images | grep $CUSTOMCONNECTORIMAGENAME"
}

# Main execution
main() {
    echo "=========================================="
    echo "  Connector Image Build & Push Script"
    echo "=========================================="
    echo
    
    check_prerequisites
    collect_input
    build_and_push_image
    show_next_steps
}

# Run main function
main "$@"
