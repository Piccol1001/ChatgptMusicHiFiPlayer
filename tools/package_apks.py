#!/usr/bin/env python3
"""Compatibility entry point: explicit verified release options are now required.

Example: python tools/package_apks.py --bump patch --notes releases/0.5.1.json
This builds and verifies; it never blindly relabels or deletes older APKs.
"""
from release import main

if __name__ == "__main__":
    main()
