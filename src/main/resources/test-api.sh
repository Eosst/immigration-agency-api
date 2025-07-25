#!/bin/bash

# API Testing Script
BASE_URL="http://localhost:8080"

echo "🧪 Testing Immigration API"
echo "=========================="

# Test 1: Health Check
echo "1. Testing Health Endpoint..."
curl -s "$BASE_URL/api/health" | jq .

# Test 2: Create Appointment
echo -e "\n2. Creating Test Appointment..."
APPOINTMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/api/appointments" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "phone": "+1234567890",
    "country": "Canada",
    "appointmentDate": "2025-08-25T14:00:00",
    "duration": 60,
    "consultationType": "General Consultation",
    "clientPresentation": "I need help with my visa application",
    "currency": "CAD"
  }')

echo $APPOINTMENT_RESPONSE | jq .

# Extract appointment ID
APPOINTMENT_ID=$(echo $APPOINTMENT_RESPONSE | jq -r '.id')
echo "Appointment ID: $APPOINTMENT_ID"

# Test 3: Get Availability
echo -e "\n3. Checking Availability for 2025-08-25..."
curl -s "$BASE_URL/api/availability/day/2025-08-25" | jq .

# Test 4: Admin Login
echo -e "\n4. Testing Admin Login..."
TOKEN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo $TOKEN_RESPONSE | jq .

TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.token')

# Test 5: Get Upcoming Appointments (Admin)
echo -e "\n5. Getting Upcoming Appointments (Admin)..."
curl -s "$BASE_URL/api/appointments/upcoming" \
  -H "Authorization: Bearer $TOKEN" | jq .

echo -e "\n✅ API Tests Complete!"