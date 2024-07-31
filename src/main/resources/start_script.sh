#!/bin/bash

echo "kube worker starting..."

user=$1
taskid=$2
ip=$3
token=$4
hash=$5

echo "$ip"
echo "$token"
echo "$hash"

#file_path="/etc/wireguard/wg0c.conf"
#cat "$file_path"

systemctl start wg-quick@wg0c &&
./create-k8s-node.sh &&

#1) Add KUBELET_EXTRA_ARGS=--node-ip= <YOUR_VPN_IP>
#sudo systemctl restart kubelet
#sudo swapoff -a

kubeadm join $ip:6443 --token $token --discovery-token-ca-cert-hash $hash --cri-socket="unix:///var/run/containerd/containerd.sock"

echo "kube worker started..."