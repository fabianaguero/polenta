#!/bin/bash

# MCP Server Smoke Test Script
# Tests the basic functionality of the JSON-RPC /mcp endpoint

set -e

SERVER_URL="http://localhost:8090/mcp"
INITIALIZE_ID="init-$(date +%s)"
PING_ID="ping-$(date +%s)"
TOOLS_LIST_ID="tools-$(date +%s)"
TOOLS_CALL_ID="call-$(date +%s)"

echo "🚀 Starting MCP Server Smoke Test..."
echo "Testing server at: $SERVER_URL"
echo

# Test 1: Initialize
echo "📋 Test 1: Initialize"
INIT_RESPONSE=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"$INITIALIZE_ID\",
    \"method\": \"initialize\",
    \"params\": {}
  }")

echo "Response: $INIT_RESPONSE"

# Check if initialize was successful
if echo "$INIT_RESPONSE" | grep -q "2024-11-05" && echo "$INIT_RESPONSE" | grep -q "protocolVersion"; then
  echo "✅ Initialize test PASSED"
else
  echo "❌ Initialize test FAILED"
  exit 1
fi
echo

# Test 2: Ping (should work after initialize)
echo "📋 Test 2: Ping"
PING_RESPONSE=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"$PING_ID\",
    \"method\": \"ping\",
    \"params\": {}
  }")

echo "Response: $PING_RESPONSE"

# Check if ping was successful
if echo "$PING_RESPONSE" | grep -q "pong" && echo "$PING_RESPONSE" | grep -q "timestamp"; then
  echo "✅ Ping test PASSED"
else
  echo "❌ Ping test FAILED"
  exit 1
fi
echo

# Test 3: Tools List
echo "📋 Test 3: Tools List"
TOOLS_RESPONSE=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"$TOOLS_LIST_ID\",
    \"method\": \"tools/list\",
    \"params\": {}
  }")

echo "Response: $TOOLS_RESPONSE"

# Check if tools list was successful
if echo "$TOOLS_RESPONSE" | grep -q "tools" && echo "$TOOLS_RESPONSE" | grep -q "query_data"; then
  echo "✅ Tools list test PASSED"
else
  echo "❌ Tools list test FAILED"
  exit 1
fi
echo

# Test 4: Tools Call - Get Suggestions
echo "📋 Test 4: Tools Call (get_suggestions)"
CALL_RESPONSE=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"$TOOLS_CALL_ID\",
    \"method\": \"tools/call\",
    \"params\": {
      \"name\": \"get_suggestions\",
      \"arguments\": {}
    }
  }")

echo "Response: $CALL_RESPONSE"

# Check if tools call was successful
if echo "$CALL_RESPONSE" | grep -q "suggestions" && echo "$CALL_RESPONSE" | grep -q "result"; then
  echo "✅ Tools call test PASSED"
else
  echo "❌ Tools call test FAILED"
  exit 1
fi
echo

# Test 5: Error handling - Unknown method
echo "📋 Test 5: Error Handling (unknown method)"
ERROR_RESPONSE=$(curl -s -X POST "$SERVER_URL" \
  -H "Content-Type: application/json" \
  -d "{
    \"jsonrpc\": \"2.0\",
    \"id\": \"error-test\",
    \"method\": \"unknown_method\",
    \"params\": {}
  }")

echo "Response: $ERROR_RESPONSE"

# Check if error was handled correctly
if echo "$ERROR_RESPONSE" | grep -q "error" && echo "$ERROR_RESPONSE" | grep -q "\-32601"; then
  echo "✅ Error handling test PASSED"
else
  echo "❌ Error handling test FAILED"
  exit 1
fi
echo

echo "🎉 All smoke tests PASSED!"
echo "✨ MCP Server is working correctly!"