import socket
import select
import time
import struct
import typing
import binascii

GNSS_UDP_PORTS = [5000, 5001, 8000, 8001, 9000, 9001, 10110, 2101]
MDNS_ADDR = ('224.0.0.251', 5353)
SSDP_ADDR = ('239.255.255.250', 1900)

def listen_udp_port(port: int, duration: float = 10.0, bind_addr: str = '0.0.0.0') -> list[dict]:
    """Listen on a specific UDP port. PASSIVE ONLY - never send.
    Returns list of received packets: {timestamp, source_ip, source_port, 
    data_hex, data_ascii, length}"""
    packets = []
    try:
        # SAFETY: We only use socket for passive listening. NEVER call send(), sendto().
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((bind_addr, port))
        sock.settimeout(1.0)
        
        start_time = time.time()
        while time.time() - start_time < duration:
            try:
                # ONLY recvfrom is used.
                data, addr = sock.recvfrom(4096)
                if data:
                    packets.append({
                        'timestamp': time.time(),
                        'source_ip': addr[0],
                        'source_port': addr[1],
                        'data_hex': data.hex(),
                        'data_ascii': ''.join(chr(c) if 32 <= c < 127 else '.' for c in data),
                        'length': len(data)
                    })
            except socket.timeout:
                continue
            except Exception as e:
                print(f"Hata (Error) receiving on port {port}: {e}")
                break
    except Exception as e:
        print(f"Hata (Error) binding to port {port}: {e}")
    finally:
        try:
            sock.close()
        except:
            pass
    return packets

def listen_broadcast(duration: float = 10.0) -> list[dict]:
    """Listen for UDP broadcasts on common GNSS ports."""
    return listen_all_gnss_udp(duration).get('packets', [])

def listen_mdns(duration: float = 10.0) -> list[dict]:
    """Listen for mDNS announcements on 224.0.0.251:5353.
    Join multicast group, receive and parse mDNS packets."""
    packets = []
    try:
        # SAFETY: Passive mDNS listener only
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        # Windows bind specific
        sock.bind(('', 5353))
        
        mreq = struct.pack("4sl", socket.inet_aton(MDNS_ADDR[0]), socket.INADDR_ANY)
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
        sock.settimeout(1.0)
        
        start_time = time.time()
        while time.time() - start_time < duration:
            try:
                data, addr = sock.recvfrom(4096)
                packets.append({
                    'timestamp': time.time(),
                    'source_ip': addr[0],
                    'source_port': addr[1],
                    'data_hex': data.hex(),
                    'data_ascii': ''.join(chr(c) if 32 <= c < 127 else '.' for c in data),
                    'length': len(data),
                    'type': 'mdns'
                })
            except socket.timeout:
                continue
    except Exception as e:
        print(f"mDNS dinleme hatası (mDNS listen error): {e}")
    finally:
        try:
            sock.close()
        except:
            pass
    return packets

def listen_ssdp(duration: float = 10.0) -> list[dict]:
    """Listen for SSDP announcements on 239.255.255.250:1900."""
    packets = []
    try:
        # SAFETY: Passive SSDP listener only
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(('', 1900))
        
        mreq = struct.pack("4sl", socket.inet_aton(SSDP_ADDR[0]), socket.INADDR_ANY)
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)
        sock.settimeout(1.0)
        
        start_time = time.time()
        while time.time() - start_time < duration:
            try:
                data, addr = sock.recvfrom(4096)
                packets.append({
                    'timestamp': time.time(),
                    'source_ip': addr[0],
                    'source_port': addr[1],
                    'data_hex': data.hex(),
                    'data_ascii': ''.join(chr(c) if 32 <= c < 127 else '.' for c in data),
                    'length': len(data),
                    'type': 'ssdp'
                })
            except socket.timeout:
                continue
    except Exception as e:
        print(f"SSDP dinleme hatası (SSDP listen error): {e}")
    finally:
        try:
            sock.close()
        except:
            pass
    return packets

def listen_all_gnss_udp(duration: float = 15.0) -> dict:
    """Listen on all known GNSS UDP ports simultaneously.
    Returns dict with port -> list of packets."""
    results = {port: [] for port in GNSS_UDP_PORTS}
    results['packets'] = []
    sockets = {}
    
    try:
        for port in GNSS_UDP_PORTS:
            sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                sock.bind(('0.0.0.0', port))
                sock.setblocking(False)
                sockets[sock] = port
            except Exception as e:
                print(f"Port {port} açılamadı (Could not open port): {e}")
        
        start_time = time.time()
        while time.time() - start_time < duration:
            if not sockets:
                break
                
            readable, _, _ = select.select(list(sockets.keys()), [], [], 1.0)
            for sock in readable:
                try:
                    data, addr = sock.recvfrom(4096)
                    port = sockets[sock]
                    packet = {
                        'timestamp': time.time(),
                        'source_ip': addr[0],
                        'source_port': addr[1],
                        'data_hex': data.hex(),
                        'data_ascii': ''.join(chr(c) if 32 <= c < 127 else '.' for c in data),
                        'length': len(data),
                        'dest_port': port
                    }
                    results[port].append(packet)
                    results['packets'].append(packet)
                except Exception:
                    pass
    finally:
        for sock in sockets:
            try:
                sock.close()
            except:
                pass
                
    return results

def analyze_udp_data(packets: list[dict]) -> dict:
    """Analyze received UDP data for GNSS protocols.
    Returns: has_nmea, has_rtcm, has_ubx, nmea_sentences, summary"""
    analysis = {
        'has_nmea': False,
        'has_rtcm': False,
        'has_ubx': False,
        'nmea_sentences': [],
        'summary': f"Total {len(packets)} packets analyzed."
    }
    
    for pkt in packets:
        # Check NMEA ($G...)
        if '$G' in pkt['data_ascii']:
            analysis['has_nmea'] = True
            lines = pkt['data_ascii'].split('\r\n')
            for line in lines:
                if line.startswith('$G'):
                    if line not in analysis['nmea_sentences']:
                        analysis['nmea_sentences'].append(line)
                        
        # Check RTCM3 (0xD3)
        if pkt['length'] > 5:
            hex_str = pkt['data_hex']
            if 'd3' in hex_str:
                analysis['has_rtcm'] = True
                
        # Check UBX (0xB5 0x62)
        if pkt['length'] > 6:
            if 'b562' in pkt['data_hex']:
                analysis['has_ubx'] = True
                
    return analysis

def format_udp_report(results: dict) -> str:
    """Format human-readable UDP listener report."""
    report = "--- UDP Listener Report ---\n"
    for k, v in results.items():
        if isinstance(v, list) and k != 'packets' and k != 'nmea_sentences':
            report += f"Port {k}: {len(v)} packets received\n"
        elif k == 'summary':
            report += f"Summary: {v}\n"
        elif k == 'has_nmea':
            report += f"NMEA Found: {v}\n"
        elif k == 'has_rtcm':
            report += f"RTCM Found: {v}\n"
        elif k == 'has_ubx':
            report += f"UBX Found: {v}\n"
    return report

if __name__ == "__main__":
    print("UDP Listener running in passive mode...")
