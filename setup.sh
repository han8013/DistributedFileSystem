#!/bin/bash

# Require sudo

sudo yum -y install https://centos7.iuscommunity.org/ius-release.rpm
sudo yum -y install python36u
sudo yum -y install python36u-pip
sudo pip3.6 install flask

# Step required:
# Copy public and private ssh key, and chmod 640 for pub, chmod 600 for pri
# Note: the key on hosts is for CS425 only

ssh-keyscan -H gitlab.engr.illinois.edu >> ~/.ssh/known_hosts
