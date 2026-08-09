#!/bin/bash
#
# FinanceBook Admin Panel Startup Script
#
# This script starts the Python admin panel server on port 8080.
# The admin panel communicates with the Java backend API on port 8000.
#

set -e

echo "========================================="
echo "  FinanceBook Admin Panel"
echo "========================================="
echo ""

# Check Python version
PYTHON_CMD=$(command -v python3 || command -v python)
if [ -z "$PYTHON_CMD" ]; then
    echo "Error: Python 3 not found. Please install Python 3.10 or higher."
    exit 1
fi

PYTHON_VERSION=$($PYTHON_CMD --version 2>&1 | awk '{print $2}')
echo "Using Python $PYTHON_VERSION"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "Creating Python virtual environment..."
    $PYTHON_CMD -m venv venv
fi

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Install/upgrade dependencies
echo "Installing dependencies..."
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt

echo ""
echo "========================================="
echo "  Starting Admin Panel Server"
echo "========================================="
echo "Port: 8080"
echo "Java Backend API: http://localhost:8000"
echo "Admin Login: http://localhost:8080/admin/login"
echo "Default credentials: admin / admin"
echo ""

# Start the server with uvicorn
uvicorn main:app --host 0.0.0.0 --port 8080 --reload
