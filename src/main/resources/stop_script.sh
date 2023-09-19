#!/bin/bash

echo "kube config cleaning..."

kubeadm reset -f --cri-socket="unix:///var/run/crio/crio.sock" &&
rm -rf /etc/kubernetes /var/lib/kubelet /var/lib/etcd /etc/cni/net.d &&
rm -rf $HOME/.kube &&
unset KUBECONFIG &&

echo "kube config cleaned!"
sleep 1  # Sleep for 1 second (adjust as needed)
