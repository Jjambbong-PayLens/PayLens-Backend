#!/bin/bash
set -euxo pipefail

dnf update -y
dnf install -y docker awscli amazon-ssm-agent amazon-cloudwatch-agent curl

systemctl enable --now docker
systemctl enable --now amazon-ssm-agent

mkdir -p /opt/paylens/app /opt/paylens/bin /opt/paylens/logs
touch /opt/paylens/app/.env

chmod 700 /opt/paylens/app /opt/paylens/bin /opt/paylens/logs
chmod 600 /opt/paylens/app/.env