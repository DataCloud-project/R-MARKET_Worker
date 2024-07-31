#!/bin/bash

echo "kube config cleaning..."

kubeadm reset -f --cri-socket="unix:///var/run/containerd/containerd.sock" &&
rm -rf /etc/cni/net.d &&
rm -rf /var/lib/cni/ &&
rm -rf /var/lib/kubelet/* &&
rm -rf /etc/kubernetes/ &&

wg-quick down wg0 &&

echo "kube config cleaned!"
sleep 1  # Sleep for 1 second (adjust as needed)
