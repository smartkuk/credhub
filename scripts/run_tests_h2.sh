#!/usr/bin/env bash

function set_bash_error_handling() {
    set -euo pipefail
}

function go_to_project_root_directory() {
    local -r script_dir=$( dirname "${BASH_SOURCE[0]}")

    cd "$script_dir/.."
}

function run_tests_h2() {
    local gradle_test_command="test"
    echo "✨ Parallel test mode enabled"
    echo "🚀 Running h2 tests"
    echo ""

    ./gradlew clean $gradle_test_command -Dspring.profiles.active=unit-test,unit-test-h2
}

function main() {
    set_bash_error_handling
    go_to_project_root_directory

    run_tests_h2
}

main
