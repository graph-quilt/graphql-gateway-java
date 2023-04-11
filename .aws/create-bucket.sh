#!/usr/bin/env bash
set -x
echo "create bucket = topics"
awslocal s3 mb s3://topics
set +x

if [ -d "/tmp/registrations" ]
then
  cd /tmp/registrations
  awslocal s3 cp . s3://topics/graphql-gateway-java/dev/ --recursive
else
    echo "Test registrations not found."
fi
