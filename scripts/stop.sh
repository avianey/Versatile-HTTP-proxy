#!/bin/bash
ps ax | grep -i 'vhp.jar' | grep -v grep | awk '{print $1}' | xargs kill -SIGTERM
