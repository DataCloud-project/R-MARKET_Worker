#!/bin/bash

sudo chmod 666 /var/run/docker.sock
iexec wallet create --keystoredir . --password setim
sudo rm wallet_worker1.json
sudo mv UTC* wallet_worker1.json
docker network create iexec-worker-net
sudo apt-get install docker-ce=5:20.10.21~3-0~ubuntu-focal
sudo apt-get install docker-ce-cli=5:20.10.21~3-0~ubuntu-focal

