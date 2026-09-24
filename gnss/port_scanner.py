"""
port_scanner.py - TCP Port Scanner
Meridian M6 GNSS Receiver - Passive Diagnostic Tool

SAFETY: This module performs TCP connect scans only.
It NEVER sends arbitrary data to any port.
Banner grabbing is read-only: connect, wait for server to send, read.
Scanning speed is throttled to avoid overwhelming the target device.
"""

import socket
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

# Priority ports with service names
PRIORITY_PORTS = {
    21: 'FTP',
    22: 'SSH',
    23: 'Telnet',
    53: 'DNS',
    80: 'HTTP',
    443: 'HTTPS',
    554: 'RTSP',
    1883: 'MQTT',
    2101: 'NTRIP',
    3000: 'Dev Server',
    5000: 'API',
    5001: 'API-alt',
    8000: 'HTTP-alt',
    8080: 'HTTP-proxy',
    8081: 'HTTP-alt2',
    8888: 'HTTP-alt3',
    9000: 'API-alt2',
    9090: 'Web Console',
    10110: 'NMEA-TCP',
    4001: 'GNSS-TCP',
    4002: 'GNSS-TCP2',
}

# NMEA patterns for detection in banner data
NMEA_PREFIXES = [b'$GP', b'$GN', b'$GL', b'$GA', b'$GB']
RTCM3_PREAMBLE = b'\xd3'
UBX_SYNC = b'\xb5\x62'


def scan_port(ip: str, port: int, timeout: float = 1.0) -> dict:
    """Scan a single TCP port using connect scan.
    
    Returns dict with: port, is_open, service_name, banner
    
    SAFETY: Only socket.connect() is used. 
    NEVER calls socket.send() or socket.sendall().
    Banner is obtained by reading data the server sends voluntarily.
    """
    result = {
        'port': port,
        'is_open': False,
        'service_name': PRIORITY_PORTS.get(port, f'port-{port}'),
        'banner': '',
        'banner_hex': '',
        'has_nmea': False,
        'has_rtcm': False,
        'has_ubx': False,
    }
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        
        err = sock.connect_ex((ip, port))
        
        if err == 0:
            result['is_open'] = True
            
            # Try to read banner (server-initiated data only)
            # SAFETY: We NEVER send data. Only recv().
            try:
                sock.settimeout(2.0)  # Wait up to 2 seconds for banner
                banner_data = sock.recv(4096)
                
                if banner_data:
                    # Store both ASCII and hex representations
                    try:
                        result['banner'] = banner_data.decode('ascii', errors='replace').strip()
                    except Exception:
                        result['banner'] = repr(banner_data[:100])
                    
                    result['banner_hex'] = banner_data[:64].hex()
                    
                    # Check for GNSS protocols
                    for prefix in NMEA_PREFIXES:
                        if prefix in banner_data:
                            result['has_nmea'] = True
                            break
                    
                    if RTCM3_PREAMBLE in banner_data:
                        result['has_rtcm'] = True
                    
                    if UBX_SYNC in banner_data:
                        result['has_ubx'] = True
                        
            except socket.timeout:
                pass  # No banner sent by server
            except Exception:
                pass
        
        sock.close()
        
    except socket.timeout:
        pass
    except ConnectionRefusedError:
        pass
    except OSError:
        pass
    except Exception:
        pass
    
    return result


def scan_priority_ports(ip: str, timeout: float = 1.0) -> list[dict]:
    """Scan priority ports for known services.
    
    Uses ThreadPoolExecutor with max_workers=20.
    Returns list of results for all scanned ports (open ones highlighted).
    """
    print(f"\n  Scanning {len(PRIORITY_PORTS)} priority ports on {ip}...")
    
    results = []
    open_ports = []
    
    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = {
            executor.submit(scan_port, ip, port, timeout): port 
            for port in PRIORITY_PORTS.keys()
        }
        
        for future in as_completed(futures):
            try:
                result = future.result()
                if result['is_open']:
                    results.append(result)
                    open_ports.append(result)
                    
                    banner_info = ""
                    if result['banner']:
                        banner_preview = result['banner'][:60].replace('\n', ' ').replace('\r', '')
                        banner_info = f" | Banner: {banner_preview}"
                    
                    gnss_info = ""
                    if result['has_nmea']:
                        gnss_info += " [NMEA]"
                    if result['has_rtcm']:
                        gnss_info += " [RTCM]"
                    if result['has_ubx']:
                        gnss_info += " [UBX]"
                    
                    print(f"  [OPEN] Port {result['port']:5d} ({result['service_name']}){gnss_info}{banner_info}")
                    
            except Exception as e:
                print(f"  [ERROR] Port scan error: {e}")
    
    if not open_ports:
        print("  No open ports found among priority ports.")
    else:
        print(f"\n  Found {len(open_ports)} open port(s)")
    
    return results


def scan_all_ports(ip: str, timeout: float = 0.5, max_workers: int = 50) -> list[dict]:
    """Scan all 65535 TCP ports with throttling.
    
    Shows progress every 1000 ports.
    max_workers is limited to avoid overwhelming the target device.
    
    Returns list of open port results.
    
    SAFETY: Only TCP connect scan. No data is sent.
    """
    print(f"\n  Full port scan on {ip} (1-65535)...")
    print(f"  Timeout: {timeout}s, Workers: {max_workers}")
    print(f"  This may take several minutes...\n")
    
    results = []
    total_ports = 65535
    scanned = 0
    start_time = time.time()
    
    # Process in batches to control resource usage
    batch_size = 1000
    
    for batch_start in range(1, total_ports + 1, batch_size):
        batch_end = min(batch_start + batch_size - 1, total_ports)
        batch_ports = range(batch_start, batch_end + 1)
        
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = {
                executor.submit(scan_port, ip, port, timeout): port 
                for port in batch_ports
            }
            
            for future in as_completed(futures):
                scanned += 1
                try:
                    result = future.result()
                    if result['is_open']:
                        results.append(result)
                        
                        banner_preview = ""
                        if result['banner']:
                            banner_preview = f" | {result['banner'][:40]}"
                        
                        gnss_info = ""
                        if result['has_nmea']:
                            gnss_info += " [NMEA]"
                        if result['has_rtcm']:
                            gnss_info += " [RTCM]"
                        if result['has_ubx']:
                            gnss_info += " [UBX]"
                        
                        print(f"  [OPEN] Port {result['port']:5d} "
                              f"({result['service_name']}){gnss_info}{banner_preview}")
                except Exception:
                    pass
        
        # Progress update per batch
        elapsed = time.time() - start_time
        pct = (batch_end / total_ports) * 100
        rate = scanned / elapsed if elapsed > 0 else 0
        eta = (total_ports - scanned) / rate if rate > 0 else 0
        print(f"  Progress: {batch_end}/{total_ports} ({pct:.0f}%) | "
              f"Open: {len(results)} | "
              f"Rate: {rate:.0f} ports/s | "
              f"ETA: {eta:.0f}s")
        
        # Small delay between batches to be gentle on the device
        time.sleep(0.1)
    
    elapsed = time.time() - start_time
    print(f"\n  Scan complete in {elapsed:.1f}s. Found {len(results)} open port(s).")
    
    return sorted(results, key=lambda x: x['port'])


def grab_banner(ip: str, port: int, timeout: float = 3.0) -> str:
    """Try to read banner from an open port. READ-ONLY.
    
    Connect, wait for server to send data, read up to 4096 bytes.
    
    SAFETY: DO NOT send any data. Only socket.recv() is used.
    """
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(timeout)
        sock.connect((ip, port))
        
        # SAFETY: Only receive, never send
        try:
            data = sock.recv(4096)
            sock.close()
            return data.decode('ascii', errors='replace').strip()
        except socket.timeout:
            sock.close()
            return ""
        except Exception:
            sock.close()
            return ""
    except Exception:
        return ""


def detect_gnss_stream(ip: str, port: int, duration: float = 5.0) -> dict:
    """Connect to a port and listen for GNSS data for up to duration seconds.
    
    Returns dict with: has_data, data_preview_ascii, data_preview_hex,
    detected_protocol, nmea_sentences, data_size, duration_actual
    
    SAFETY: Connect and receive only. NEVER sends data to the port.
    """
    result = {
        'ip': ip,
        'port': port,
        'has_data': False,
        'data_preview_ascii': '',
        'data_preview_hex': '',
        'detected_protocols': [],
        'nmea_sentences': [],
        'data_size': 0,
        'duration_actual': 0,
        'error': None
    }
    
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3.0)  # Connection timeout
        sock.connect((ip, port))
        
        # SAFETY: Read-only - never send data
        all_data = b''
        start_time = time.time()
        sock.settimeout(1.0)  # Read timeout
        
        while time.time() - start_time < duration:
            try:
                chunk = sock.recv(4096)
                if not chunk:
                    break
                all_data += chunk
            except socket.timeout:
                continue
            except Exception:
                break
        
        sock.close()
        result['duration_actual'] = time.time() - start_time
        
        if all_data:
            result['has_data'] = True
            result['data_size'] = len(all_data)
            
            # ASCII preview
            try:
                ascii_text = all_data.decode('ascii', errors='replace')
                result['data_preview_ascii'] = ascii_text[:500]
            except Exception:
                pass
            
            # Hex preview
            result['data_preview_hex'] = all_data[:128].hex()
            
            # Detect NMEA
            try:
                text = all_data.decode('ascii', errors='ignore')
                import re
                nmea_matches = re.findall(r'\$[A-Z]{2,5},[^*]*\*[0-9A-Fa-f]{2}', text)
                if nmea_matches:
                    result['detected_protocols'].append('NMEA')
                    result['nmea_sentences'] = nmea_matches[:20]  # First 20
            except Exception:
                pass
            
            # Detect RTCM3
            if RTCM3_PREAMBLE in all_data:
                # Verify it looks like RTCM3 (check length field makes sense)
                idx = all_data.index(RTCM3_PREAMBLE)
                if idx + 3 <= len(all_data):
                    length = ((all_data[idx + 1] & 0x03) << 8) | all_data[idx + 2]
                    if 0 < length < 1024:  # Reasonable RTCM3 message length
                        result['detected_protocols'].append('RTCM3')
            
            # Detect UBX
            if UBX_SYNC in all_data:
                idx = all_data.index(UBX_SYNC)
                if idx + 6 <= len(all_data):
                    result['detected_protocols'].append('UBX')
            
            # If no known protocol detected
            if not result['detected_protocols']:
                result['detected_protocols'].append('Unknown/Binary')
    
    except ConnectionRefusedError:
        result['error'] = 'Connection refused'
    except socket.timeout:
        result['error'] = 'Connection timed out'
    except OSError as e:
        result['error'] = str(e)
    except Exception as e:
        result['error'] = str(e)
    
    return result


def format_port_scan_report(results: list[dict]) -> str:
    """Format human-readable port scan report."""
    lines = []
    lines.append("=" * 60)
    lines.append("  PORT SCAN REPORT")
    lines.append("=" * 60)
    lines.append(f"Scan Time: {datetime.now().isoformat()}")
    lines.append(f"Open Ports: {len(results)}")
    lines.append("")
    
    if not results:
        lines.append("No open ports found.")
    else:
        lines.append(f"{'Port':>6s}  {'Service':<16s}  {'GNSS':>8s}  Banner")
        lines.append("-" * 70)
        
        for r in sorted(results, key=lambda x: x['port']):
            gnss = ""
            if r.get('has_nmea'):
                gnss += "NMEA "
            if r.get('has_rtcm'):
                gnss += "RTCM "
            if r.get('has_ubx'):
                gnss += "UBX "
            
            banner = r.get('banner', '')[:40].replace('\n', ' ').replace('\r', '')
            lines.append(f"{r['port']:6d}  {r['service_name']:<16s}  {gnss:>8s}  {banner}")
    
    return '\n'.join(lines)


if __name__ == '__main__':
    print("Port Scanner - Meridian M6 GNSS Diagnostic Tool")
    print("=" * 50)
    print("SAFETY: TCP connect scan only. No data is sent.\n")
    
    target = input("Enter target IP address: ").strip()
    if target:
        print("\n[1] Scanning priority ports...")
        results = scan_priority_ports(target, timeout=1.0)
        print("\n" + format_port_scan_report(results))
        
        choice = input("\n[?] Scan all 65535 ports? (y/n): ").strip().lower()
        if choice == 'y':
            results = scan_all_ports(target, timeout=0.5)
            print("\n" + format_port_scan_report(results))
    else:
        print("No target IP provided.")
