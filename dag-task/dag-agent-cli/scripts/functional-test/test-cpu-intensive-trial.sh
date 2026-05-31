#!/usr/bin/env bash
# Test CpuIntensiveExecution with trial division (heavier CPU load)

source "$(dirname "$0")/../common/run-local.sh"

run_cli \
    502 \
    "CpuIntensiveTrial" \
    "top.ilovemyhome.dagtask.agent.execution.CpuIntensiveExecution" \
    '{"upperBound":50000,"algorithm":"trial","iterations":10}'
