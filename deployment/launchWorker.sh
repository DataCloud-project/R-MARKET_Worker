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

# Check if docker service is running
if systemctl is-active --quiet docker; then
    echo "docker service is running."
else
    echo "docker service is not running."
    exit 1
fi

# Check if kubelet service is running
if systemctl list-units --full --all | grep -q kubelet.service; then
    echo "kubelet service is running."
else
    echo "kubelet service is not running."
    exit 1
fi

# Check if Java is installed by running the java -version command
if java -version >/dev/null 2>&1; then
    # Java is installed, now check the version
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    
    # Check if the Java version is at least 11
    if [[ "$java_version" == 1.[1-9]* || "$java_version" == 11.* ]]; then
        echo "Java 11 or higher is installed. Version: $java_version"
    else
        echo "Java version $java_version is installed, but it's not Java 11 or higher, you can use 'sudo apt install openjdk-11-jre' command to install java 11." 
	 exit 1
    fi
else
    # Java is not installed
    echo "Java is not installed, you can use 'sudo apt install openjdk-11-jre' to install java 11."
    exit 1
fi

echo "All checks passed. Dependencies and services are configured correctly."


set -o allexport && source .env && set +o allexport


# Initialize a flag to determine whether detached mode is specified
detached_mode=false

# Process command-line options
while [[ $# -gt 0 ]]; do
    case "$1" in
        --detached|-d)  # Accept both --detached and -d
            detached_mode=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Define log file
timestamp=$(date +"%Y%m%d%H%M%S")
log_file="app_$timestamp.log"

# Run the Java app either in the background or foreground based on --detached
if [ "$detached_mode" = true ]; then
    # Use nohup to run the Java app in the background, disassociated from the terminal
    nohup java -Djava.security.egd=file:/dev/./urandom -jar ../build/libs/R-MARKET_Worker-7.0.0-Datacloud.jar  > "$log_file" 2>&1 &
    # Capture the PID of the background process
    java_app_pid=$!
    
    echo "Java app is running in the background with PID $java_app_pid"
    echo "You can stop it by running: kill $java_app_pid"
    echo "To access logs, run: cat $log_file"
    echo "To access logs as a stream, run: tail -f $log_file"
else
    # Run the Java app in the foreground
    java -Djava.security.egd=file:/dev/./urandom -jar ../build/libs/R-MARKET_Worker-7.0.0-Datacloud.jar 
fi


