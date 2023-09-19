#!/bin/bash

echo "kube worker starting..."

kubeadm join 10.0.0.9:6443 --token bd9xkn.586u3yetlmshw6ef --discovery-token-ca-cert-hash sha256:930a7672129aa3611cace2957b9e0e6d8a986afb8e3317fc61d4a63673cb9942 --cri-socket="unix:///var/run/crio/crio.sock" &&
	
echo "kube worker started..."