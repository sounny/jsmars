#!/usr/bin/env python3
"""
ASU JMARS SVN Sync Utility
--------------------------
Syncs or checks for updates from ASU's public JMARS Subversion repository
(https://oss.mars.asu.edu/svn/jmars/default/trunk/) to the local 'jmars/' reference folder.
Works without needing a native 'svn' command-line binary.
"""

import os
import sys
import re
import urllib.request
import urllib.parse
from html.parser import HTMLParser
from pathlib import Path

SVN_BASE_URL = "https://oss.mars.asu.edu/svn/jmars/default/trunk/"
SVN_ROOT_URL = "https://oss.mars.asu.edu/svn/jmars/"
DEFAULT_TARGET_DIR = Path(__file__).resolve().parent.parent / "jmars"

class SVNDirParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.entries = []
        self.revision = None

    def handle_starttag(self, tag, attrs):
        if tag == "a":
            for k, v in attrs:
                if k == "href" and v not in ("../", "./"):
                    self.entries.append(v)

    def handle_data(self, data):
        # Look for "Revision X:" in headings/titles
        m = re.search(r"Revision\s+(\d+)", data)
        if m and not self.revision:
            self.revision = int(m.group(1))

def get_remote_revision():
    req = urllib.request.Request(SVN_ROOT_URL, headers={"User-Agent": "JSMARS-Sync/1.0"})
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode("utf-8", errors="ignore")
    parser = SVNDirParser()
    parser.feed(html)
    return parser.revision

def list_svn_dir(url):
    req = urllib.request.Request(url, headers={"User-Agent": "JSMARS-Sync/1.0"})
    with urllib.request.urlopen(req) as resp:
        html = resp.read().decode("utf-8", errors="ignore")
    parser = SVNDirParser()
    parser.feed(html)
    return parser.entries

def fetch_file(remote_url, local_path, dry_run=False):
    local_path = Path(local_path)
    if not dry_run:
        local_path.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(remote_url, headers={"User-Agent": "JSMARS-Sync/1.0"})
    with urllib.request.urlopen(req) as resp:
        data = resp.read()
    
    if local_path.exists():
        with open(local_path, "rb") as f:
            if f.read() == data:
                return "identical"
    
    if not dry_run:
        with open(local_path, "wb") as f:
            f.write(data)
        return "updated"
    return "would_update"

def sync_directory(remote_dir_url, local_dir, dry_run=False, depth=0):
    entries = list_svn_dir(remote_dir_url)
    stats = {"identical": 0, "updated": 0, "created": 0}
    
    for entry in entries:
        is_dir = entry.endswith("/")
        item_name = entry.rstrip("/")
        item_remote_url = urllib.parse.urljoin(remote_dir_url, entry)
        item_local_path = local_dir / item_name
        
        if is_dir:
            sub_stats = sync_directory(item_remote_url, item_local_path, dry_run=dry_run, depth=depth+1)
            for k in stats:
                stats[k] += sub_stats[k]
        else:
            exists = item_local_path.exists()
            status = fetch_file(item_remote_url, item_local_path, dry_run=dry_run)
            if status == "identical":
                stats["identical"] += 1
            else:
                if exists:
                    stats["updated"] += 1
                    print(f"  [MODIFIED] {item_local_path.relative_to(DEFAULT_TARGET_DIR)}")
                else:
                    stats["created"] += 1
                    print(f"  [NEW]      {item_local_path.relative_to(DEFAULT_TARGET_DIR)}")
    return stats

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Check or sync ASU JMARS SVN repository.")
    parser.add_argument("--check", action="store_true", help="Check remote revision without downloading.")
    parser.add_argument("--dry-run", action="store_true", help="Simulate sync without modifying local files.")
    parser.add_argument("--dest", type=str, default=str(DEFAULT_TARGET_DIR), help="Destination directory for jmars reference.")
    args = parser.parse_args()

    dest_dir = Path(args.dest)
    print(f"Connecting to ASU JMARS SVN: {SVN_BASE_URL}...")
    try:
        rev = get_remote_revision()
        print(f"Remote SVN Server Revision: r{rev}")
    except Exception as e:
        print(f"Error connecting to SVN server: {e}")
        sys.exit(1)

    if args.check:
        print("Check completed.")
        return

    print(f"Target directory: {dest_dir}")
    if args.dry_run:
        print("Running in DRY-RUN mode (no files will be written)...")
    else:
        print("Syncing reference files...")

    stats = sync_directory(SVN_BASE_URL, dest_dir, dry_run=args.dry_run)
    print("\nSync Summary:")
    print(f"  Identical files : {stats['identical']}")
    print(f"  Updated files   : {stats['updated']}")
    print(f"  New files       : {stats['created']}")

if __name__ == "__main__":
    main()
