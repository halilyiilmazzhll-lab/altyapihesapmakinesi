"""
network_discovery.py - Wi-Fi/Network Discovery Module
Meridian M6 GNSS Receiver - Passive Diagnostic Tool

SAFETY: This module only reads network configuration and performs
standard ICMP ping and HTTP GET requests. No exploit, attack,
or aggressive scanning is performed.
"""

import os
import re
import socket
import subprocess
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime

try:
    import psutil
    HAS_PSUTIL = True
except ImportError:
    HAS_PSUTIL = False

try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False


def get_network_interfaces() -> list[dict]:
    """Get all network interfaces with IP, netmask, gateway, MAC.
    
    Uses ipconfig /all parsing and psutil.net_if_addrs() as fallback.
    
    Returns list of dicts with: name, ip, netmask, gateway, mac, is_wifi, is_up
    """
    interfaces = []
    
    # Method 1: Parse ipconfig /all
    try:
        result = subprocess.run(
            ['ipconfig', '/all'],
            capture_output=True, text=True, timeout=10,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0
        )
        
        if result.returncode == 0:
            current_if = None
            for line in result.stdout.split('\n'):
                line = line.rstrip()
                
                # New adapter section
                if line and not line.startswith(' ') and ':' in line:
                    if current_if and current_if.get('ip'):
                        interfaces.append(current_if)
                    adapter_name = line.split(':')[0].strip()
                    current_if = {
                        'name': adapter_name,
                        'ip': '',
                        'netmask': '',
                        'gateway': '',
                        'mac': '',
                        'is_wifi': any(w in adapter_name.lower() for w in 
                                      ['wi-fi', 'wifi', 'wireless', 'wlan', 'kablosuz']),
                        'is_up': False,
                        'dhcp': False,
                        'dns': [],
                        'description': ''
                    }
                elif current_if and line.strip():
                    line_lower = line.strip().lower()
                    
                    if 'ipv4' in line_lower and ':' in line:
                        ip_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                        if ip_match:
                            current_if['ip'] = ip_match.group(1)
                            current_if['is_up'] = True
                    
                    elif ('subnet mask' in line_lower or 'alt ağ maskesi' in line_lower) and ':' in line:
                        mask_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                        if mask_match:
                            current_if['netmask'] = mask_match.group(1)
                    
                    elif ('default gateway' in line_lower or 'varsayılan ağ geçidi' in line_lower) and ':' in line:
                        gw_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                        if gw_match:
                            current_if['gateway'] = gw_match.group(1)
                    
                    elif ('physical address' in line_lower or 'fiziksel adres' in line_lower) and ':' in line:
                        mac_match = re.search(r'([0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2}[-:][0-9A-Fa-f]{2})', line)
                        if mac_match:
                            current_if['mac'] = mac_match.group(1)
                    
                    elif ('dhcp' in line_lower) and ':' in line:
                        if 'yes' in line_lower or 'evet' in line_lower:
                            current_if['dhcp'] = True
                    
                    elif ('description' in line_lower or 'açıklama' in line_lower) and ':' in line:
                        desc = line.split(':', 1)[1].strip() if ':' in line else ''
                        current_if['description'] = desc
                    
                    elif ('dns' in line_lower) and ':' in line:
                        dns_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                        if dns_match:
                            current_if['dns'].append(dns_match.group(1))
            
            # Don't forget the last interface
            if current_if and current_if.get('ip'):
                interfaces.append(current_if)
                
    except Exception as e:
        print(f"[WARN] ipconfig parsing failed: {e}")
    
    # Method 2: Supplement with psutil if available
    if HAS_PSUTIL and not interfaces:
        try:
            addrs = psutil.net_if_addrs()
            stats = psutil.net_if_stats()
            
            for name, addr_list in addrs.items():
                iface = {
                    'name': name,
                    'ip': '',
                    'netmask': '',
                    'gateway': '',
                    'mac': '',
                    'is_wifi': any(w in name.lower() for w in ['wi-fi', 'wifi', 'wireless', 'wlan']),
                    'is_up': stats.get(name, None) and stats[name].isup,
                    'dhcp': False,
                    'dns': [],
                    'description': ''
                }
                
                for addr in addr_list:
                    if addr.family == socket.AF_INET:
                        iface['ip'] = addr.address
                        iface['netmask'] = addr.netmask or ''
                    elif addr.family == psutil.AF_LINK:
                        iface['mac'] = addr.address
                
                if iface['ip']:
                    interfaces.append(iface)
                    
        except Exception as e:
            print(f"[WARN] psutil network info failed: {e}")
    
    return interfaces


def get_default_gateway() -> str | None:
    """Get default gateway IP address.
    
    Parses 'route print' or 'ipconfig' output.
    """
    # Method 1: route print
    try:
        result = subprocess.run(
            ['route', 'print', '0.0.0.0'],
            capture_output=True, text=True, timeout=10,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0
        )
        if result.returncode == 0:
            for line in result.stdout.split('\n'):
                if '0.0.0.0' in line:
                    parts = line.split()
                    for part in parts:
                        if re.match(r'\d+\.\d+\.\d+\.\d+', part) and part != '0.0.0.0':
                            return part
    except Exception:
        pass
    
    # Method 2: ipconfig
    try:
        result = subprocess.run(
            ['ipconfig'],
            capture_output=True, text=True, timeout=10,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0
        )
        if result.returncode == 0:
            for line in result.stdout.split('\n'):
                line_lower = line.strip().lower()
                if ('default gateway' in line_lower or 'varsayılan ağ geçidi' in line_lower):
                    gw_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                    if gw_match:
                        return gw_match.group(1)
    except Exception:
        pass
    
    return None


def get_arp_table() -> list[dict]:
    """Parse 'arp -a' output.
    
    Returns list of dicts: ip, mac, type, interface
    """
    arp_entries = []
    
    try:
        result = subprocess.run(
            ['arp', '-a'],
            capture_output=True, text=True, timeout=10,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0
        )
        
        if result.returncode == 0:
            current_interface = ''
            for line in result.stdout.split('\n'):
                line = line.strip()
                
                # Interface header line
                if_match = re.search(r'Interface:\s*(\d+\.\d+\.\d+\.\d+)', line)
                if if_match:
                    current_interface = if_match.group(1)
                    continue
                
                # ARP entry line
                arp_match = re.match(
                    r'(\d+\.\d+\.\d+\.\d+)\s+([0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2}[-:][0-9a-fA-F]{2})\s+(\w+)',
                    line
                )
                if arp_match:
                    arp_entries.append({
                        'ip': arp_match.group(1),
                        'mac': arp_match.group(2),
                        'type': arp_match.group(3),
                        'interface': current_interface
                    })
                    
    except Exception as e:
        print(f"[ERROR] ARP table read failed: {e}")
    
    return arp_entries


def ping_host(ip: str, timeout: float = 1.0) -> bool:
    """Ping a single host using Windows ping command.
    
    Returns True if host responds.
    """
    try:
        timeout_ms = max(int(timeout * 1000), 100)
        result = subprocess.run(
            ['ping', '-n', '1', '-w', str(timeout_ms), ip],
            capture_output=True, text=True, timeout=timeout + 2,
            creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0
        )
        # Check for TTL in response (indicates success)
        return 'TTL=' in result.stdout or 'ttl=' in result.stdout.lower()
    except Exception:
        return False


def sweep_subnet(base_ip: str, netmask: str = '255.255.255.0',
                 timeout: float = 0.5) -> list[str]:
    """Ping sweep the subnet to find active hosts.
    
    Uses ThreadPoolExecutor with max_workers=30 to avoid flooding.
    
    Returns list of responding IPs.
    """
    # Calculate subnet range
    ip_parts = base_ip.split('.')
    mask_parts = netmask.split('.')
    
    # Simple /24 calculation for most cases
    if netmask == '255.255.255.0':
        network_base = '.'.join(ip_parts[:3])
        ip_range = [f"{network_base}.{i}" for i in range(1, 255)]
    else:
        # More general calculation
        network = []
        broadcast = []
        for ip_byte, mask_byte in zip(ip_parts, mask_parts):
            network.append(int(ip_byte) & int(mask_byte))
            broadcast.append(int(ip_byte) | (~int(mask_byte) & 0xFF))
        
        # Generate IPs (limit to /24 or smaller to be safe)
        ip_range = []
        if broadcast[3] - network[3] <= 254:
            base = '.'.join(str(b) for b in network[:3])
            for i in range(network[3] + 1, broadcast[3]):
                ip_range.append(f"{base}.{i}")
        else:
            # Too large subnet, just scan .1-.254 of current /24
            network_base = '.'.join(ip_parts[:3])
            ip_range = [f"{network_base}.{i}" for i in range(1, 255)]
    
    active_hosts = []
    total = len(ip_range)
    
    print(f"  Scanning {total} hosts in subnet...")
    
    with ThreadPoolExecutor(max_workers=30) as executor:
        futures = {executor.submit(ping_host, ip, timeout): ip for ip in ip_range}
        done_count = 0
        
        for future in as_completed(futures):
            done_count += 1
            ip = futures[future]
            
            if done_count % 50 == 0:
                print(f"  Progress: {done_count}/{total} ({done_count*100//total}%)")
            
            try:
                if future.result():
                    active_hosts.append(ip)
                    print(f"  [ALIVE] {ip}")
            except Exception:
                pass
    
    return sorted(active_hosts, key=lambda x: [int(p) for p in x.split('.')])


def find_m6_device(active_hosts: list[str], gateway: str = None) -> list[dict]:
    """Try to identify Meridian M6 among active hosts.
    
    Checks each host: tries HTTP GET on port 80/8080, looks for 'Meridian' 
    or 'M6' in response.
    
    SAFETY: Only GET requests are used. No POST/PUT/DELETE.
    
    Returns list of candidate dicts with: ip, reason, http_title
    """
    candidates = []
    
    if not HAS_REQUESTS:
        print("[WARN] requests library not available. HTTP identification disabled.")
        # Still return non-gateway hosts as potential candidates
        for ip in active_hosts:
            if ip != gateway:
                candidates.append({
                    'ip': ip,
                    'reason': 'Active host (HTTP check unavailable)',
                    'http_title': ''
                })
        return candidates
    
    print(f"  Checking {len(active_hosts)} active hosts for M6 identification...")
    
    for ip in active_hosts:
        if ip == gateway:
            continue  # Skip gateway
        
        candidate = {
            'ip': ip,
            'reason': 'Active host',
            'http_title': '',
            'http_server': '',
            'http_ports': []
        }
        
        # Try HTTP on common ports - SAFETY: GET only
        for port in [80, 8080, 8081, 443]:
            try:
                scheme = 'https' if port == 443 else 'http'
                url = f"{scheme}://{ip}:{port}/"
                
                # SAFETY: Only GET request
                response = requests.get(url, timeout=2, verify=False, 
                                       allow_redirects=True)
                
                candidate['http_ports'].append(port)
                
                # Extract title
                title_match = re.search(r'<title[^>]*>(.*?)</title>', 
                                       response.text, re.IGNORECASE | re.DOTALL)
                if title_match:
                    candidate['http_title'] = title_match.group(1).strip()
                
                # Get server header
                candidate['http_server'] = response.headers.get('Server', '')
                
                # Check for M6 / Meridian identifiers
                content_lower = response.text.lower()
                headers_lower = str(response.headers).lower()
                
                if any(kw in content_lower or kw in headers_lower 
                       for kw in ['meridian', 'm6', 'gnss', 'gps receiver']):
                    candidate['reason'] = 'Meridian/GNSS keywords found in HTTP response'
                elif response.status_code == 200:
                    candidate['reason'] = f'HTTP service on port {port}'
                
            except requests.exceptions.SSLError:
                candidate['http_ports'].append(port)
                candidate['reason'] = f'HTTPS service on port {port} (SSL)'
            except (requests.exceptions.ConnectionError, 
                    requests.exceptions.Timeout):
                pass
            except Exception:
                pass
        
        if candidate['http_ports'] or ip != gateway:
            candidates.append(candidate)
    
    return candidates


def discover_network_full() -> dict:
    """Run complete network discovery.
    
    Returns dict with: interfaces, gateway, arp_table, active_hosts, m6_candidates
    """
    print("\n" + "=" * 60)
    print("  NETWORK DISCOVERY")
    print("  MODE: Passive / Safe scanning")
    print("=" * 60)
    
    result = {
        'scan_time': datetime.now().isoformat(),
        'interfaces': [],
        'gateway': None,
        'arp_table': [],
        'active_hosts': [],
        'm6_candidates': [],
        'wifi_interface': None
    }
    
    # Step 1: Get network interfaces
    print("\n[1/5] Discovering network interfaces...")
    result['interfaces'] = get_network_interfaces()
    
    for iface in result['interfaces']:
        wifi_tag = " [Wi-Fi]" if iface['is_wifi'] else ""
        up_tag = " [UP]" if iface['is_up'] else " [DOWN]"
        print(f"  {iface['name']}{wifi_tag}{up_tag}")
        if iface['ip']:
            print(f"    IP: {iface['ip']}")
            print(f"    Netmask: {iface['netmask']}")
            if iface['gateway']:
                print(f"    Gateway: {iface['gateway']}")
            if iface['mac']:
                print(f"    MAC: {iface['mac']}")
        
        if iface['is_wifi'] and iface['is_up'] and iface['ip']:
            result['wifi_interface'] = iface
    
    # Step 2: Get default gateway
    print("\n[2/5] Finding default gateway...")
    result['gateway'] = get_default_gateway()
    if result['gateway']:
        print(f"  Default Gateway: {result['gateway']}")
    else:
        print("  [WARN] Could not determine default gateway")
    
    # Step 3: Get ARP table
    print("\n[3/5] Reading ARP table...")
    result['arp_table'] = get_arp_table()
    print(f"  Found {len(result['arp_table'])} ARP entries")
    for entry in result['arp_table']:
        print(f"    {entry['ip']:16s} {entry['mac']:20s} {entry['type']}")
    
    # Step 4: Subnet sweep (only if we have a Wi-Fi interface)
    wifi_if = result['wifi_interface']
    if wifi_if:
        print(f"\n[4/5] Subnet sweep on {wifi_if['ip']} / {wifi_if['netmask']}...")
        result['active_hosts'] = sweep_subnet(
            wifi_if['ip'], 
            wifi_if['netmask'] or '255.255.255.0',
            timeout=0.5
        )
        print(f"  Found {len(result['active_hosts'])} active hosts")
    else:
        print("\n[4/5] Subnet sweep - skipped (no Wi-Fi interface found)")
        # Try using any interface with gateway
        for iface in result['interfaces']:
            if iface['ip'] and iface['gateway']:
                print(f"  Using {iface['name']} ({iface['ip']}) instead...")
                result['active_hosts'] = sweep_subnet(
                    iface['ip'],
                    iface['netmask'] or '255.255.255.0',
                    timeout=0.5
                )
                print(f"  Found {len(result['active_hosts'])} active hosts")
                break
    
    # Step 5: Try to identify M6
    print("\n[5/5] Identifying Meridian M6...")
    if result['active_hosts']:
        result['m6_candidates'] = find_m6_device(
            result['active_hosts'], 
            result['gateway']
        )
        
        if result['m6_candidates']:
            print(f"\n  Potential M6 candidates:")
            for c in result['m6_candidates']:
                print(f"    {c['ip']} - {c['reason']}")
                if c.get('http_title'):
                    print(f"      Title: {c['http_title']}")
                if c.get('http_server'):
                    print(f"      Server: {c['http_server']}")
        else:
            print("  No M6 candidates identified")
    else:
        print("  Skipped (no active hosts found)")
    
    return result


def format_network_report(results: dict) -> str:
    """Format human-readable network discovery report."""
    lines = []
    lines.append("=" * 60)
    lines.append("  NETWORK DISCOVERY REPORT")
    lines.append("=" * 60)
    lines.append(f"Scan Time: {results.get('scan_time', 'N/A')}")
    lines.append("")
    
    # Interfaces
    lines.append("Network Interfaces:")
    for iface in results.get('interfaces', []):
        wifi_tag = " [Wi-Fi]" if iface['is_wifi'] else ""
        lines.append(f"  {iface['name']}{wifi_tag}")
        lines.append(f"    IP: {iface['ip']}, Netmask: {iface['netmask']}")
        lines.append(f"    Gateway: {iface.get('gateway', 'N/A')}")
        lines.append(f"    MAC: {iface.get('mac', 'N/A')}")
    lines.append("")
    
    # Gateway
    lines.append(f"Default Gateway: {results.get('gateway', 'N/A')}")
    lines.append("")
    
    # ARP table
    lines.append(f"ARP Table ({len(results.get('arp_table', []))} entries):")
    for entry in results.get('arp_table', []):
        lines.append(f"  {entry['ip']:16s} {entry['mac']:20s} {entry['type']}")
    lines.append("")
    
    # Active hosts
    active = results.get('active_hosts', [])
    lines.append(f"Active Hosts ({len(active)}):")
    for ip in active:
        lines.append(f"  {ip}")
    lines.append("")
    
    # M6 candidates
    candidates = results.get('m6_candidates', [])
    lines.append(f"M6 Candidates ({len(candidates)}):")
    for c in candidates:
        lines.append(f"  {c['ip']} - {c['reason']}")
    
    return '\n'.join(lines)


if __name__ == '__main__':
    print("Network Discovery - Meridian M6 GNSS Diagnostic Tool")
    print("=" * 50)
    print("SAFETY: Passive/safe scanning only\n")
    
    results = discover_network_full()
    print("\n" + format_network_report(results))
