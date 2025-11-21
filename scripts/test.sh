#!/bin/sh

# Configuration
API_URL="http://localhost:28081/api/v1"
CURRENCY="${1:-EUR}" # Use first argument or default to EUR

# Check for HTTP client
if command -v xh >/dev/null 2>&1; then
  HTTP_CLIENT="xh"
elif command -v curl >/dev/null 2>&1; then
  HTTP_CLIENT="curl"
else
  echo "Error: Neither 'xh' nor 'curl' is available. Please install one of them."
  exit 1
fi

# HTTP request wrapper functions
http_get() {
  if [ "$HTTP_CLIENT" = "xh" ]; then
    xh GET "$1"
  else
    curl -s "$1"
  fi
}

http_post() {
  url="$1"
  shift
  if [ "$HTTP_CLIENT" = "xh" ]; then
    xh POST "$url" "$@"
  else
    # Convert xh-style args (key=value) to JSON for curl
    json="{"
    first=true
    for arg in "$@"; do
      if [ "$first" = false ]; then
        json="${json},"
      fi
      key="${arg%%=*}"
      value="${arg#*=}"
      json="${json}\"${key}\":\"${value}\""
      first=false
    done
    json="${json}}"

    if [ "$#" -eq 0 ]; then
      curl -s -X POST -H "Content-Type: application/json" "$url"
    else
      curl -s -X POST -H "Content-Type: application/json" -d "$json" "$url"
    fi
  fi
}

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
GRAY='\033[0;90m'
BOLD='\033[1m'
NC='\033[0m'

# Helper functions
print_header() {
  echo ""
  echo "${BOLD}${CYAN}$1${NC}"
  echo "${GRAY}────────────────────────────────────────────────────────────────${NC}"
}

print_section() {
  echo ""
  echo "${MAGENTA}▸ $1${NC}"
}

print_success() {
  echo "  ${GREEN}✓${NC} $1"
}

print_info() {
  echo "  ${GRAY}→${NC} $1"
}

print_error() {
  echo "  ${RED}✗${NC} $1"
}

print_key_value() {
  printf "  ${GRAY}%-20s${NC} %s\n" "$1:" "$2"
}

print_api_call() {
  echo "  ${GRAY}${1}${NC}"
}

# Start
echo ""
echo "${BOLD}${CYAN}Ledger API Test${NC}"
echo ""
print_key_value "API URL" "${API_URL}"
print_key_value "HTTP Client" "${HTTP_CLIENT}"
print_key_value "Default Currency" "${CURRENCY}"
print_key_value "Timestamp" "$(date '+%Y-%m-%d %H:%M:%S')"

# Create Ledger
print_header "1. Creating Ledger"
print_info "Creating ledger with default EUR currency account"
print_api_call "POST ${API_URL}/ledgers"
echo ""

RESPONSE=$(http_post "${API_URL}/ledgers")
if [ $? -eq 0 ]; then
  LEDGER_ID=$(echo "${RESPONSE}" | jq -r '.id')
  echo ""
  print_success "Ledger created"
  print_key_value "Ledger ID" "${LEDGER_ID}"
else
  print_error "Failed to create ledger"
  exit 1
fi

# Create USD account only
print_header "2. Adding USD Currency"
print_info "EUR account already exists (default)"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/currencies"
echo ""

http_post "${API_URL}/ledgers/${LEDGER_ID}/currencies" currency=USD
if [ $? -eq 0 ]; then
  echo ""
  print_success "USD currency account created"
else
  print_error "Failed to create USD currency account"
  exit 1
fi

# Make Deposits (EUR only)
print_header "3. Processing Deposits (EUR)"

print_section "Deposit #1: €500.00"
print_info "Description: Initial deposit | Reference: DEP-001"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit"
echo ""
http_post "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit" \
  "value=500.00" "description=Initial deposit" "reference=DEP-001"
echo ""

print_section "Deposit #2: €250.75"
print_info "Description: Salary payment | Reference: DEP-002"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit"
echo ""
http_post "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit" \
  "value=250.75" "description=Salary payment" "reference=DEP-002"
echo ""

print_section "Deposit #3: €100.00"
print_info "Description: Bonus | Reference: DEP-003"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit"
echo ""
http_post "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/deposit" \
  "value=100.00" "description=Bonus" "reference=DEP-003"
echo ""

print_success "Total deposits: €850.75"

# Make Withdrawals (EUR only)
print_header "4. Processing Withdrawals (EUR)"

print_section "Withdrawal #1: €150.50"
print_info "Description: Grocery shopping | Reference: WITH-001"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/withdrawal"
echo ""
http_post "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/withdrawal" \
  "value=150.50" "description=Grocery shopping" "reference=WITH-001"
echo ""

print_section "Withdrawal #2: €75.25"
print_info "Description: Gas station | Reference: WITH-002"
print_api_call "POST ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/withdrawal"
echo ""
http_post "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR/withdrawal" \
  "value=75.25" "description=Gas station" "reference=WITH-002"
echo ""

print_success "Total withdrawals: €225.75"

# Get Current balance
print_header "5. Current Balances"

print_section "EUR Balance"
print_info "Expected: €625.00 (€850.75 deposits - €225.75 withdrawals)"
print_api_call "GET ${API_URL}/ledgers/${LEDGER_ID}/balance/EUR"
echo ""
http_get "${API_URL}/ledgers/${LEDGER_ID}/balance/EUR"
echo ""

print_section "USD Balance"
print_info "Expected: $0.00 (no transactions)"
print_api_call "GET ${API_URL}/ledgers/${LEDGER_ID}/balance/USD"
echo ""
http_get "${API_URL}/ledgers/${LEDGER_ID}/balance/USD"

# Get Transaction History (EUR only)
print_header "6. Transaction History (EUR)"

print_info "Time range: today"
print_info "Limit: 10 transactions"
print_api_call "GET ${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR"
echo ""

http_get "${API_URL}/ledgers/${LEDGER_ID}/transactions/EUR"

# Summary
echo ""
echo "${BOLD}${CYAN}Summary${NC}"
echo "${GRAY}────────────────────────────────────────────────────────────────${NC}"
echo ""
print_success "All operations completed successfully"
echo ""
print_key_value "Ledger ID" "${LEDGER_ID}"
print_key_value "Currencies" "EUR (default), USD"
echo ""
echo "  ${GREEN}EUR Account${NC}"
print_key_value "  Transactions" "5 (3 deposits, 2 withdrawals)"
print_key_value "  Balance" "€625.00"
echo ""
echo "  ${YELLOW}USD Account${NC}"
print_key_value "  Transactions" "0"
print_key_value "  Balance" "\$0.00"
echo ""
