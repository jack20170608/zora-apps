#!/bin/bash
#
# Helper script: loops 100 times, printing current time every second.
# Used to test long-running shell task execution.
#

for ((i=1; i<=100; i++))
do
    current_time=$(date "+%Y-%m-%d %H:%M:%S")
    echo "第 $i 次 | 当前时间：$current_time"
    sleep 1
done

echo -e "\n✅ loop 100 times finished!!"
