#!/bin/bash

set -e

if rpm -q jq &>/dev/null; then
  :
else
  echo "jq installing..."
  yum -y install jq &>/dev/null
fi

if kubectl get clusterrolebinding | grep admin-service-account &>/dev/null; then
  :
else
  echo "clusterrolebinding binding..."
  kubectl create clusterrolebinding admin-service-account --clusterrole=cluster-admin --serviceaccount=kube-system:default
fi

MASTER_URL=$(kubectl config view --minify | grep server | cut -f 2- -d ":" | tr -d " ")
TOKEN_NAME=$(kubectl describe sa default -n kube-system | grep Tokens | awk '{print $2}')
DATA_TOKEN=$(kubectl get secret $TOKEN_NAME -n kube-system -o json | jq -r '.data["token"]')
CA_CRT=$(kubectl get secret "$TOKEN_NAME" -n kube-system -o json | jq -r '.data["ca.crt"]')

echo -e "*************k8s info*************"
echo -e "k8s.api-server=$MASTER_URL"
echo -e "k8s.cacrt=$CA_CRT"
echo -e "k8s.token=$DATA_TOKEN"