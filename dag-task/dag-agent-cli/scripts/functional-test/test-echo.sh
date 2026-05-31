#!/usr/bin/env bash
# Test EchoExecution

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    301 \
    "EchoTest" \
    "top.ilovemyhome.dagtask.agent.execution.EchoExecution" \
    '{"greeting":"hello","target":"dag-agent-cli"}'
