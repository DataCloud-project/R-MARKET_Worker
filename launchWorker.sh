#!/bin/bash

# Function to check if a command is available
check_command() {
    local cmd="$1"
    if ! command -v "$cmd" &>/dev/null; then
        echo "$cmd is not installed."
        return 1
    fi
    return 0
}

# Check if running with sudo
if [ -z "$SUDO_UID" ]; then
    echo "This script requires superuser privileges. Please run with sudo."
    exit 1
fi

# Check if kubeadm is installed
if ! check_command "kubeadm"; then
    exit 1
fi

# Check if kubelet is installed
if ! check_command "kubelet"; then
    exit 1
fi

# Check if crio service is running
if systemctl is-active --quiet crio; then
    echo "crio service is running."
else
    echo "crio service is not running."
    exit 1
fi

# Check if kubelet service is running
if systemctl is-active --quiet kubelet; then
    echo "kubelet service is running."
else
    echo "kubelet service is not running."
    exit 1
fi

echo "All checks passed. Dependencies and services are configured correctly."

java -Djava.security.egd=file:/dev/./urandom -jar ./R-MARKET_Worker-7.0.0-Datacloud.jar 
