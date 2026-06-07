#!/bin/bash

# 循环 10 次
for ((i=1; i<=10; i++))
do
    # 获取当前时间，格式：年-月-日 时:分:秒
    current_time=$(date "+%Y-%m-%d %H:%M:%S")

    # 打印次数 + 时间
    echo "第 $i 次 | 当前时间：$current_time"

    # 休眠 1 秒
    sleep 1
done

echo -e "\n✅ loop 100 times finished!!"
