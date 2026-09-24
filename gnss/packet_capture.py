import sys
import time
import socket
import struct
import typing

try:
    from scapy.all import sniff, IP, TCP, UDP, get_if_list
    SCAPY_AVAILABLE = True
except ImportError:
    SCAPY_AVAILABLE = False
    print("Scapy not found. Falling back to raw sockets. (Scapy bulunamadı, raw socket kullanılacak)")

def capture_packets(interface: str = None, duration: float = 30.0, 
                    target_ip: str = None) -> list[dict]:
    """Capture network packets passively.
    Try scapy first, fall back to raw socket if not available.
    Returns list of packet dicts: {timestamp, src_ip, dst_ip, src_port, 
    dst_port, protocol (TCP/UDP), length, ascii_preview, hex_preview}"""
    if SCAPY_AVAILABLE:
        print("Using Scapy for packet capture...")
        return capture_with_scapy(interface, duration, target_ip)
    else:
        print("Using raw sockets for packet capture... (Requires Admin on Windows)")
        return capture_with_raw_socket(duration, target_ip)

def capture_with_scapy(interface: str, duration: float, target_ip: str) -> list[dict]:
    """Capture using scapy.sniff()."""
    packets_data = []
    
    # SAFETY: sniff() is inherently passive.
    def packet_callback(packet):
        if IP in packet:
            src_ip = packet[IP].src
            dst_ip = packet[IP].dst
            
            if target_ip and src_ip != target_ip and dst_ip != target_ip:
                return
                
            proto = "OTHER"
            src_port = 0
            dst_port = 0
            payload = bytes()
            
            if TCP in packet:
                proto = "TCP"
                src_port = packet[TCP].sport
                dst_port = packet[TCP].dport
                payload = bytes(packet[TCP].payload)
            elif UDP in packet:
                proto = "UDP"
                src_port = packet[UDP].sport
                dst_port = packet[UDP].dport
                payload = bytes(packet[UDP].payload)
            else:
                payload = bytes(packet[IP].payload)
                
            packets_data.append({
                'timestamp': time.time(),
                'src_ip': src_ip,
                'dst_ip': dst_ip,
                'src_port': src_port,
                'dst_port': dst_port,
                'protocol': proto,
                'length': len(payload),
                'ascii_preview': ''.join(chr(c) if 32 <= c < 127 else '.' for c in payload[:100]),
                'hex_preview': payload[:100].hex(),
                'raw': payload
            })
            
    try:
        kwargs = {"timeout": duration, "prn": packet_callback, "store": 0}
        if interface:
            kwargs["iface"] = interface
        sniff(**kwargs)
    except Exception as e:
        print(f"Scapy capture error: {e}")
        
    return packets_data

def capture_with_raw_socket(duration: float, target_ip: str) -> list[dict]:
    """Fallback capture using raw sockets (limited on Windows)."""
    packets_data = []
    try:
        # SAFETY: Raw sockets opened for listening only
        host = socket.gethostbyname(socket.gethostname())
        s = socket.socket(socket.AF_INET, socket.SOCK_RAW, socket.IPPROTO_IP)
        s.bind((host, 0))
        s.setsockopt(socket.IPPROTO_IP, socket.IP_HDRINCL, 1)
        s.ioctl(socket.SIO_RCVALL, socket.RCVALL_ON)
        s.settimeout(1.0)
        
        start_time = time.time()
        while time.time() - start_time < duration:
            try:
                data, addr = s.recvfrom(65565)
                # Parse IP header
                ip_header = data[0:20]
                iph = struct.unpack('!BBHHHBBH4s4s', ip_header)
                version_ihl = iph[0]
                ihl = version_ihl & 0xF
                iph_length = ihl * 4
                
                protocol = iph[6]
                src_ip = socket.inet_ntoa(iph[8])
                dst_ip = socket.inet_ntoa(iph[9])
                
                if target_ip and src_ip != target_ip and dst_ip != target_ip:
                    continue
                    
                payload = data[iph_length:]
                proto_name = "OTHER"
                src_port = 0
                dst_port = 0
                
                if protocol == 6: # TCP
                    proto_name = "TCP"
                    if len(payload) >= 20:
                        tcph = struct.unpack('!HHLLBBHHH', payload[0:20])
                        src_port = tcph[0]
                        dst_port = tcph[1]
                        tcph_length = (tcph[4] >> 4) * 4
                        payload = payload[tcph_length:]
                elif protocol == 17: # UDP
                    proto_name = "UDP"
                    if len(payload) >= 8:
                        udph = struct.unpack('!HHHH', payload[0:8])
                        src_port = udph[0]
                        dst_port = udph[1]
                        payload = payload[8:]
                        
                packets_data.append({
                    'timestamp': time.time(),
                    'src_ip': src_ip,
                    'dst_ip': dst_ip,
                    'src_port': src_port,
                    'dst_port': dst_port,
                    'protocol': proto_name,
                    'length': len(payload),
                    'ascii_preview': ''.join(chr(c) if 32 <= c < 127 else '.' for c in payload[:100]),
                    'hex_preview': payload[:100].hex(),
                    'raw': payload
                })
            except socket.timeout:
                continue
    except Exception as e:
        print(f"Raw socket capture error (Admin rights might be missing): {e}")
    finally:
        try:
            s.ioctl(socket.SIO_RCVALL, socket.RCVALL_OFF)
            s.close()
        except:
            pass
            
    return packets_data

def analyze_captured_packets(packets: list[dict]) -> dict:
    """Analyze captured packets.
    Returns: unique_connections (set of src:port->dst:port), 
    protocol_stats (TCP count, UDP count),
    gnss_packets (list of packets containing NMEA/RTCM/UBX),
    binary_patterns (recurring header bytes)"""
    unique_connections = set()
    protocol_stats = {"TCP": 0, "UDP": 0, "OTHER": 0}
    gnss_packets = []
    
    for pkt in packets:
        conn = f"{pkt['src_ip']}:{pkt['src_port']} -> {pkt['dst_ip']}:{pkt['dst_port']} ({pkt['protocol']})"
        unique_connections.add(conn)
        protocol_stats[pkt['protocol']] = protocol_stats.get(pkt['protocol'], 0) + 1
        
        # Check ASCII content for NMEA ($G...)
        if '$G' in pkt['ascii_preview']:
            gnss_packets.append(pkt)
            continue
            
        # Check binary for RTCM3 (0xD3) and UBX (0xB5 0x62)
        if 'd3' in pkt['hex_preview'] or 'b562' in pkt['hex_preview']:
            gnss_packets.append(pkt)
            
    binary_patterns = find_recurring_headers(packets)
            
    return {
        'unique_connections': list(unique_connections),
        'protocol_stats': protocol_stats,
        'gnss_packets': gnss_packets,
        'binary_patterns': binary_patterns
    }

def find_recurring_headers(packets: list[dict], min_length: int = 4) -> list[dict]:
    """Find recurring byte patterns at the start of packet payloads.
    Useful for identifying proprietary protocols."""
    headers = {}
    for pkt in packets:
        if pkt['length'] >= min_length:
            header_hex = pkt['hex_preview'][:min_length*2]
            headers[header_hex] = headers.get(header_hex, 0) + 1
            
    recurring = [{'pattern': k, 'count': v} for k, v in headers.items() if v > 1]
    return sorted(recurring, key=lambda x: x['count'], reverse=True)

def format_capture_report(results: dict) -> str:
    """Format human-readable packet capture report."""
    report = "--- Packet Capture Report ---\n"
    report += f"Protocols: {results['protocol_stats']}\n"
    report += f"Unique Connections: {len(results['unique_connections'])}\n"
    report += f"GNSS Packets Found: {len(results['gnss_packets'])}\n"
    report += "Recurring Binary Headers (potential proprietary protocols):\n"
    for pattern in results['binary_patterns'][:5]:
        report += f"  - 0x{pattern['pattern']}: {pattern['count']} times\n"
    return report

def list_interfaces() -> list[str]:
    """List available network interfaces for capture."""
    if SCAPY_AVAILABLE:
        try:
            return get_if_list()
        except Exception:
            return []
    return []

if __name__ == "__main__":
    print("Packet capture module loaded.")
