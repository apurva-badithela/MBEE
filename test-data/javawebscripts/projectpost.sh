#!/bin/bash

curl -w "%{http_code}\n" -X POST -u admin:admin -H "Content-Type:application/json" --data '{"name":"Clipper"}' "http://localhost:8080/view-repo/service/javawebscripts/sites/europa/projects/17_0123455?fix=true&createSite=true"
