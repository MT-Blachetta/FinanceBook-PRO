"""
FinanceBook Admin Panel Server

This is a standalone Python/FastAPI server that provides the admin dashboard
for FinanceBook. It communicates with the Java backend API at port 8000.

Architecture:
- Admin Panel (Python/FastAPI): Port 8080
- Java Backend API: Port 8000
- PostgreSQL Database: Port 5500

The admin panel uses the same PostgreSQL database as the Java backend
but handles its own authentication with session cookies.
"""

import os
from pathlib import Path

from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles

from admin import admin_router

# Create FastAPI app
app = FastAPI(
    title="FinanceBook Admin Panel",
    version="1.0.0",
    description="Administration dashboard for FinanceBook",
)

# Mount static files
STATIC_DIR = Path(__file__).parent / "static"
STATIC_DIR.mkdir(exist_ok=True)
app.mount("/static", StaticFiles(directory=str(STATIC_DIR)), name="static")

# Include admin router
app.include_router(admin_router)


@app.get("/")
def root():
    """Redirect root to admin login."""
    from fastapi.responses import RedirectResponse
    return RedirectResponse(url="/admin/login")


@app.get("/health")
def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "admin-panel"}


# For running with: uvicorn main:app --reload
if __name__ == "__main__":
    import uvicorn
    
    # Get configuration from environment
    host = os.getenv("ADMIN_HOST", "0.0.0.0")
    port = int(os.getenv("ADMIN_PORT", "8080"))
    
    # Run the server
    print(f"Starting admin panel at http://{host}:{port}")
    uvicorn.run(
        "main:app",
        host=host,
        port=port,
        reload=True,
        log_level="info",
    )
