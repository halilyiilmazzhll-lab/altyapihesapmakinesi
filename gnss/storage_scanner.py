"""
storage_scanner.py - Read-Only USB Storage Scanner
Meridian M6 GNSS Receiver - Passive Diagnostic Tool

SAFETY: This module ONLY reads files. It NEVER writes, deletes, or modifies
any file on any drive. All file operations use read-only mode ('r' or 'rb').
"""

import os
import hashlib
import time
from datetime import datetime

try:
    import psutil
    HAS_PSUTIL = True
except ImportError:
    HAS_PSUTIL = False
    print("[WARN] psutil not installed. Removable drive detection will be limited.")

# Target file extensions to search for
TARGET_EXTENSIONS = [
    '.log', '.txt', '.ini', '.cfg', '.conf', '.json', '.xml',
    '.nmea', '.ubx', '.rtcm', '.dat', '.bin'
]

# Keywords to search in text files
SEARCH_KEYWORDS = [
    'PPS', '1PPS', 'TIMEPULSE', 'NMEA', 'RTCM', 'TCP', 'UDP',
    'PORT', 'SERIAL', 'UART', 'GNSS', 'GPS', 'WIFI', 'IP', 'TIME', 'UTC'
]

# Maximum file size to search content (10 MB)
MAX_TEXT_SEARCH_SIZE = 10 * 1024 * 1024

# Maximum binary preview size
MAX_BINARY_PREVIEW = 256


def find_removable_drives() -> list[dict]:
    """Find removable/USB drives.
    
    Returns list of dicts with: drive_letter, fstype, total_size, used, free, label, opts
    
    SAFETY: Read-only operation - only queries system information.
    """
    drives = []
    
    if HAS_PSUTIL:
        try:
            partitions = psutil.disk_partitions(all=False)
            for part in partitions:
                # Check if removable
                is_removable = 'removable' in part.opts.lower() if part.opts else False
                
                # On Windows, also check drive type
                drive_info = {
                    'drive_letter': part.mountpoint,
                    'device': part.device,
                    'fstype': part.fstype,
                    'opts': part.opts,
                    'is_removable': is_removable,
                    'total_size': 0,
                    'used': 0,
                    'free': 0,
                    'label': ''
                }
                
                try:
                    usage = psutil.disk_usage(part.mountpoint)
                    drive_info['total_size'] = usage.total
                    drive_info['used'] = usage.used
                    drive_info['free'] = usage.free
                except (PermissionError, OSError):
                    pass
                
                # Try to get volume label on Windows
                try:
                    import ctypes
                    kernel32 = ctypes.windll.kernel32
                    volume_name = ctypes.create_unicode_buffer(256)
                    kernel32.GetVolumeInformationW(
                        part.mountpoint, volume_name, 256,
                        None, None, None, None, 0
                    )
                    drive_info['label'] = volume_name.value
                except Exception:
                    pass
                
                drives.append(drive_info)
        except Exception as e:
            print(f"[ERROR] Failed to enumerate drives: {e}")
    else:
        # Fallback: check common Windows drive letters
        import string
        for letter in string.ascii_uppercase:
            drive_path = f"{letter}:\\"
            if os.path.exists(drive_path):
                drive_info = {
                    'drive_letter': drive_path,
                    'device': f"{letter}:",
                    'fstype': 'unknown',
                    'opts': '',
                    'is_removable': False,
                    'total_size': 0,
                    'used': 0,
                    'free': 0,
                    'label': ''
                }
                drives.append(drive_info)
    
    return drives


def scan_file_tree(root_path: str, max_depth: int = 10) -> dict:
    """Walk directory tree READ-ONLY.
    
    Returns dict with: total_files, total_dirs, file_tree (nested list),
    files_by_extension (dict of ext -> list of paths)
    
    SAFETY: Uses os.walk() which is read-only. No files are created, 
    modified, or deleted.
    """
    result = {
        'root': root_path,
        'total_files': 0,
        'total_dirs': 0,
        'file_list': [],
        'files_by_extension': {},
        'errors': []
    }
    
    try:
        for dirpath, dirnames, filenames in os.walk(root_path):
            # Check depth
            depth = dirpath.replace(root_path, '').count(os.sep)
            if depth > max_depth:
                dirnames.clear()
                continue
            
            result['total_dirs'] += 1
            
            for filename in filenames:
                result['total_files'] += 1
                filepath = os.path.join(dirpath, filename)
                rel_path = os.path.relpath(filepath, root_path)
                
                try:
                    stat = os.stat(filepath)
                    file_info = {
                        'path': rel_path,
                        'full_path': filepath,
                        'name': filename,
                        'extension': os.path.splitext(filename)[1].lower(),
                        'size': stat.st_size,
                        'modified': datetime.fromtimestamp(stat.st_mtime).isoformat()
                    }
                    result['file_list'].append(file_info)
                    
                    ext = file_info['extension']
                    if ext not in result['files_by_extension']:
                        result['files_by_extension'][ext] = []
                    result['files_by_extension'][ext].append(rel_path)
                    
                except (PermissionError, OSError) as e:
                    result['errors'].append(f"Cannot stat {rel_path}: {e}")
                    
    except (PermissionError, OSError) as e:
        result['errors'].append(f"Cannot walk {root_path}: {e}")
    
    return result


def find_interesting_files(root_path: str) -> list[dict]:
    """Find files with target extensions.
    
    Returns list of dicts with: path, name, extension, size, modified_time
    
    SAFETY: Read-only directory traversal.
    """
    interesting = []
    
    try:
        for dirpath, dirnames, filenames in os.walk(root_path):
            for filename in filenames:
                ext = os.path.splitext(filename)[1].lower()
                if ext in TARGET_EXTENSIONS:
                    filepath = os.path.join(dirpath, filename)
                    try:
                        stat = os.stat(filepath)
                        interesting.append({
                            'path': os.path.relpath(filepath, root_path),
                            'full_path': filepath,
                            'name': filename,
                            'extension': ext,
                            'size': stat.st_size,
                            'modified_time': datetime.fromtimestamp(stat.st_mtime).isoformat()
                        })
                    except (PermissionError, OSError) as e:
                        print(f"  [WARN] Cannot access {filepath}: {e}")
    except (PermissionError, OSError) as e:
        print(f"[ERROR] Cannot scan {root_path}: {e}")
    
    return interesting


def search_file_contents(filepath: str) -> dict:
    """Search a text file for GNSS-related keywords. READ-ONLY open.
    
    Returns: filepath, keywords_found (list of {keyword, line_number, line_content}),
    is_binary (bool), file_size, preview
    
    SAFETY: File is opened with mode='r' (read-only) ONLY.
    No write operations are performed.
    """
    result = {
        'filepath': filepath,
        'keywords_found': [],
        'is_binary': False,
        'file_size': 0,
        'preview': '',
        'error': None
    }
    
    try:
        file_size = os.path.getsize(filepath)
        result['file_size'] = file_size
        
        if file_size > MAX_TEXT_SEARCH_SIZE:
            result['error'] = f"File too large ({file_size} bytes), skipping content search"
            return result
        
        # First, check if file is binary by reading first 8192 bytes
        # SAFETY: mode='rb' is read-only binary
        with open(filepath, 'rb') as f:  # READ-ONLY
            header = f.read(8192)
        
        # Check for null bytes (binary indicator)
        if b'\x00' in header:
            result['is_binary'] = True
            result['preview'] = header[:MAX_BINARY_PREVIEW].hex()
            
            # Still search for keywords in binary
            try:
                text_parts = header.decode('ascii', errors='ignore')
                for keyword in SEARCH_KEYWORDS:
                    if keyword.lower() in text_parts.lower():
                        result['keywords_found'].append({
                            'keyword': keyword,
                            'line_number': 0,
                            'line_content': f'[binary file, keyword found in header]'
                        })
            except Exception:
                pass
            
            # Calculate hash for binary files (read-only)
            try:
                sha256 = hashlib.sha256()
                with open(filepath, 'rb') as f:  # READ-ONLY
                    while True:
                        chunk = f.read(65536)
                        if not chunk:
                            break
                        sha256.update(chunk)
                result['sha256'] = sha256.hexdigest()
            except Exception:
                pass
            
            return result
        
        # Text file - search line by line
        # SAFETY: mode='r' is read-only text
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:  # READ-ONLY
            for line_num, line in enumerate(f, 1):
                line_stripped = line.strip()
                for keyword in SEARCH_KEYWORDS:
                    if keyword.lower() in line_stripped.lower():
                        result['keywords_found'].append({
                            'keyword': keyword,
                            'line_number': line_num,
                            'line_content': line_stripped[:200]  # Truncate long lines
                        })
                
                # Save first few lines as preview
                if line_num <= 10:
                    result['preview'] += line
                    
    except PermissionError:
        result['error'] = "Permission denied"
    except UnicodeDecodeError:
        result['is_binary'] = True
        result['error'] = "Unicode decode error - treating as binary"
    except OSError as e:
        result['error'] = str(e)
    
    return result


def scan_drive_full(drive_path: str) -> dict:
    """Complete read-only scan of a drive.
    
    Combines file tree, interesting files, and keyword search results.
    
    SAFETY: All operations are read-only. No files are written, deleted, 
    or modified on the target drive.
    """
    print(f"\n{'='*60}")
    print(f"  STORAGE SCAN: {drive_path}")
    print(f"  MODE: READ-ONLY (no modifications)")
    print(f"{'='*60}")
    
    result = {
        'drive_path': drive_path,
        'scan_time': datetime.now().isoformat(),
        'file_tree': None,
        'interesting_files': [],
        'keyword_results': [],
        'summary': {}
    }
    
    # Step 1: Scan file tree
    print("\n[1/3] Scanning file tree (read-only)...")
    result['file_tree'] = scan_file_tree(drive_path)
    print(f"  Found {result['file_tree']['total_files']} files in "
          f"{result['file_tree']['total_dirs']} directories")
    
    if result['file_tree']['files_by_extension']:
        print("  File extensions found:")
        for ext, files in sorted(result['file_tree']['files_by_extension'].items()):
            print(f"    {ext or '(no ext)'}: {len(files)} files")
    
    # Step 2: Find interesting files
    print("\n[2/3] Searching for interesting files...")
    result['interesting_files'] = find_interesting_files(drive_path)
    print(f"  Found {len(result['interesting_files'])} files with target extensions")
    
    for f in result['interesting_files']:
        size_str = _format_size(f['size'])
        print(f"    {f['extension']:8s} {size_str:>10s}  {f['path']}")
    
    # Step 3: Search file contents
    print("\n[3/3] Searching file contents for keywords (read-only)...")
    files_with_keywords = 0
    
    for file_info in result['interesting_files']:
        # Skip very large binary files
        if file_info['extension'] in ['.bin', '.dat'] and file_info['size'] > MAX_TEXT_SEARCH_SIZE:
            print(f"  [SKIP] {file_info['name']} - too large ({_format_size(file_info['size'])})")
            continue
        
        search_result = search_file_contents(file_info['full_path'])
        if search_result['keywords_found']:
            files_with_keywords += 1
            result['keyword_results'].append(search_result)
            
            unique_keywords = set(k['keyword'] for k in search_result['keywords_found'])
            print(f"  [FOUND] {file_info['name']}: {', '.join(unique_keywords)}")
    
    print(f"\n  Files with keywords: {files_with_keywords}")
    
    # Summary
    all_keywords = set()
    for kr in result['keyword_results']:
        for kf in kr['keywords_found']:
            all_keywords.add(kf['keyword'])
    
    result['summary'] = {
        'total_files': result['file_tree']['total_files'],
        'total_dirs': result['file_tree']['total_dirs'],
        'interesting_files_count': len(result['interesting_files']),
        'files_with_keywords': files_with_keywords,
        'unique_keywords_found': sorted(all_keywords),
        'has_nmea_files': any(f['extension'] == '.nmea' for f in result['interesting_files']),
        'has_ubx_files': any(f['extension'] == '.ubx' for f in result['interesting_files']),
        'has_rtcm_files': any(f['extension'] == '.rtcm' for f in result['interesting_files']),
        'has_config_files': any(f['extension'] in ['.ini', '.cfg', '.conf', '.json', '.xml'] 
                               for f in result['interesting_files']),
        'has_log_files': any(f['extension'] in ['.log', '.txt'] 
                            for f in result['interesting_files']),
    }
    
    return result


def format_storage_report(results: dict) -> str:
    """Format human-readable storage scan report."""
    lines = []
    lines.append("=" * 60)
    lines.append("  STORAGE SCAN REPORT")
    lines.append("=" * 60)
    lines.append(f"Drive: {results.get('drive_path', 'N/A')}")
    lines.append(f"Scan Time: {results.get('scan_time', 'N/A')}")
    lines.append("")
    
    summary = results.get('summary', {})
    lines.append(f"Total Files: {summary.get('total_files', 0)}")
    lines.append(f"Total Directories: {summary.get('total_dirs', 0)}")
    lines.append(f"Interesting Files: {summary.get('interesting_files_count', 0)}")
    lines.append(f"Files with Keywords: {summary.get('files_with_keywords', 0)}")
    lines.append("")
    
    lines.append("GNSS Data Files:")
    lines.append(f"  NMEA files: {'YES' if summary.get('has_nmea_files') else 'NO'}")
    lines.append(f"  UBX files:  {'YES' if summary.get('has_ubx_files') else 'NO'}")
    lines.append(f"  RTCM files: {'YES' if summary.get('has_rtcm_files') else 'NO'}")
    lines.append(f"  Config files: {'YES' if summary.get('has_config_files') else 'NO'}")
    lines.append(f"  Log files: {'YES' if summary.get('has_log_files') else 'NO'}")
    lines.append("")
    
    keywords = summary.get('unique_keywords_found', [])
    if keywords:
        lines.append(f"Keywords Found: {', '.join(keywords)}")
    else:
        lines.append("Keywords Found: None")
    lines.append("")
    
    # List interesting files
    interesting = results.get('interesting_files', [])
    if interesting:
        lines.append("Interesting Files:")
        for f in interesting:
            lines.append(f"  [{f['extension']}] {f['path']} ({_format_size(f['size'])})")
    
    lines.append("")
    
    # Keyword search details
    keyword_results = results.get('keyword_results', [])
    if keyword_results:
        lines.append("Keyword Search Details:")
        for kr in keyword_results:
            lines.append(f"\n  File: {kr['filepath']}")
            for kf in kr['keywords_found'][:20]:  # Limit output
                lines.append(f"    Line {kf['line_number']}: [{kf['keyword']}] {kf['line_content'][:100]}")
    
    return '\n'.join(lines)


def _format_size(size_bytes: int) -> str:
    """Format file size in human-readable format."""
    if size_bytes < 1024:
        return f"{size_bytes} B"
    elif size_bytes < 1024 * 1024:
        return f"{size_bytes / 1024:.1f} KB"
    elif size_bytes < 1024 * 1024 * 1024:
        return f"{size_bytes / (1024 * 1024):.1f} MB"
    else:
        return f"{size_bytes / (1024 * 1024 * 1024):.1f} GB"


if __name__ == '__main__':
    print("Storage Scanner - Meridian M6 GNSS Diagnostic Tool")
    print("=" * 50)
    print("SAFETY: All operations are READ-ONLY\n")
    
    drives = find_removable_drives()
    
    if not drives:
        print("No drives found.")
    else:
        print(f"Found {len(drives)} drive(s):\n")
        for i, d in enumerate(drives):
            removable_str = " [REMOVABLE]" if d.get('is_removable') else ""
            label_str = f" ({d['label']})" if d.get('label') else ""
            size_str = _format_size(d['total_size']) if d['total_size'] else "unknown size"
            print(f"  [{i}] {d['drive_letter']}{label_str} - {d['fstype']} - {size_str}{removable_str}")
        
        print("\nTo scan a drive, run: scan_drive_full('D:\\\\')")
