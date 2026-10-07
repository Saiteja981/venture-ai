"""
Pytest configuration.

Adds the project root to sys.path so that `from training.predict_startup import ...`
resolves correctly when tests are run from the ai-service/ directory.
"""

import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent

if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))