---
name: java-process-ports
description: Show listening ports for a given Java process PID. Works on Windows (netstat) and Unix (netstat/ss/lsof).
trigger: "java.*port|listening.*java|进程.*端口|端口.*进程|java.*监听|监听.*java"
---

# Java Process Ports

When the user asks to see which ports a Java process is listening on:

1. Identify the target PID from the user's message. If not provided, ask for it.
2. Use the appropriate OS command to list listening ports for that PID:
   - **Windows**: `netstat -ano | grep "<PID>" | grep "LISTENING"`
   - **Linux/macOS**: `netstat -tlnp | grep "<PID>"` or `ss -tlnp | grep "<PID>"` or `lsof -Pan -p <PID> -i`
3. Present the results as a clean table: Protocol | Bind Address | Port | State.
4. If no listening ports are found, mention that explicitly and suggest checking if the process is actually a Java process or if it's running.
