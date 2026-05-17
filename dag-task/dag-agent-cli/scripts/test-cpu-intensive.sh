#!/usr/bin/env bash
# Test CpuIntensiveExecution with sieve algorithm

source "$(dirname "$0")/run-local.sh"

run_cli \
    501 \
    "CpuIntensiveSieve" \
    "top.ilovemyhome.dagtask.agent.execution.CpuIntensiveExecution" \
    '{"upperBound":5000000,"algorithm":"sieve","iterations":5}'
