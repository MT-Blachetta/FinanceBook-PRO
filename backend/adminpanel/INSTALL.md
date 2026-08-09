# Admin Panel Installation Guide

## Prerequisites

### System Requirements

- Python 3.10 or higher
- PostgreSQL 15+ (same database as Java backend)
- Linux, macOS, or Windows with WSL

### System Dependencies

The admin panel uses `psycopg` (v3) which has pre-compiled binary wheels for most platforms. If you encounter installation issues, you may need:

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y python3-dev python3-pip python3-venv
```

**macOS:**
```bash
brew install python@3.10
```

**RHEL/CentOS/Fedora:**
```bash
sudo dnf install -y python3-devel python3-pip
```

## Installation Steps

### Option 1: Automatic Setup (Recommended)

```bash
cd financeJava/backend/adminpanel
chmod +x run_admin.sh
./run_admin.sh
```

The script will:
1. Create a Python virtual environment
2. Install all dependencies
3. Start the admin server on port 8080

### Option 2: Manual Setup

```bash
cd financeJava/backend/adminpanel

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate  # On Linux/macOS
# OR
venv\Scripts\activate  # On Windows

# Upgrade pip and install dependencies
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt

# Start the server
python main.py
```

## Configuration

### Environment Variables

Create a `.env` file in the `adminpanel` directory:

```bash
# Database Configuration
DATABASE_URL=postgresql://yourself:secretPassword@localhost:5500/financebook

# Admin Session Configuration
ADMIN_SESSION_SECRET=change-this-secret-for-admin-sessions
ADMIN_SESSION_MAX_AGE=3600

# Server Configuration
ADMIN_HOST=0.0.0.0
ADMIN_PORT=8080

# Java Backend API
JAVA_BACKEND_URL=http://localhost:8000
```

### Database Connection

The admin panel connects to the **same PostgreSQL database** as the Java backend. Ensure:

1. PostgreSQL is running on port 5500
2. Database `financebook` exists
3. User `yourself` has access with password `secretPassword`

You can verify the connection:

```bash
psql -U yourself -d financebook -h localhost -p 5500
```

## Troubleshooting

### Error: `psycopg` installation fails

**Solution 1 - Use pre-compiled binaries:**
The requirements already use `psycopg[binary]` which includes pre-compiled binaries. Make sure you have the latest pip:

```bash
pip install --upgrade pip setuptools wheel
pip install -r requirements.txt
```

**Solution 2 - Install PostgreSQL development libraries:**

If you still get errors, install PostgreSQL dev packages:

```bash
# Ubuntu/Debian
sudo apt-get install libpq-dev

# macOS
brew install postgresql

# RHEL/CentOS
sudo dnf install libpq-devel
```

### Error: Port 8080 already in use

Change the port in `config.py` or set environment variable:

```bash
export ADMIN_PORT=8081
python main.py
```

### Error: Cannot connect to PostgreSQL

1. Check PostgreSQL is running:
   ```bash
   sudo systemctl status postgresql
   ```

2. Verify connection settings in `.env` or environment variables

3. Test connection:
   ```bash
   psql -U yourself -d financebook -h localhost -p 5500
   ```

### Error: Cannot import admin module

Make sure you're in the correct directory:

```bash
cd financeJava/backend/adminpanel
python main.py
```

## Verifying Installation

Once started, you should see:

```
=========================================
  Starting Admin Panel Server
=========================================
Port: 8080
Java Backend API: http://localhost:8000
Admin Login: http://localhost:8080/admin/login
Default credentials: admin / admin

INFO:     Started server process [12345]
INFO:     Waiting for application startup.
INFO:     Application startup complete.
INFO:     Uvicorn running on http://0.0.0.0:8080 (Press CTRL+C to quit)
```

Access the admin panel:
- URL: http://localhost:8080/admin/login
- Username: `admin`
- Password: `admin`

## Development Mode

For development with auto-reload:

```bash
cd financeJava/backend/adminpanel
source venv/bin/activate
uvicorn main:app --reload --port 8080
```

## Production Deployment

For production, use a production ASGI server:

```bash
# Install production dependencies
pip install gunicorn

# Run with Gunicorn
gunicorn main:app \
  --workers 4 \
  --worker-class uvicorn.workers.UvicornWorker \
  --bind 0.0.0.0:8080 \
  --access-logfile - \
  --error-logfile -
```

