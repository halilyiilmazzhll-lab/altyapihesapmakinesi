"""
main.py - Meridian M6 GNSS Receiver - Passive Diagnostic Tool
Main entry point and CLI menu orchestrator.

SAFETY: All operations are READ-ONLY. No data will be written to the device.
No firmware writing, config changes, device reset, factory reset,
format, or unknown binary commands are ever performed.

Version: v1.0.0
"""

import sys
import os
import time
import json
from datetime import datetime
from typing import Dict, Any, Optional

# Graceful optional imports
def safe_import(module_name: str):
    """Import a module with graceful fallback if not available."""
    try:
        return __import__(module_name)
    except ImportError as e:
        print(f"\033[93m[WARN] {module_name} not available: {e}\033[0m")
        return None
    except Exception as e:
        print(f"\033[91m[ERROR] Failed to import {module_name}: {e}\033[0m")
        return None

# Import all modules
usb_discovery = safe_import('usb_discovery')
usb_listener = safe_import('usb_listener')
storage_scanner = safe_import('storage_scanner')
network_discovery = safe_import('network_discovery')
port_scanner = safe_import('port_scanner')
udp_listener = safe_import('udp_listener')
packet_capture = safe_import('packet_capture')
gnss_parser = safe_import('gnss_parser')
protocol_detector = safe_import('protocol_detector')
timing_analyzer = safe_import('timing_analyzer')
report_generator = safe_import('report_generator')
http_scanner = safe_import('http_scanner')

VERSION = 'v1.0.0'

# Store all collected results globally
collected_results: Dict[str, Any] = {}


def print_banner():
    """Display the application banner."""
    print(f"""\033[96m
+----------------------------------------------------------+
|    Meridian M6 GNSS Receiver - Passive Diagnostic Tool   |
|                      Version {VERSION}                        |
|                                                          |
|  \033[91m!!  READ-ONLY MODE - No modifications to device\033[96m        |
+----------------------------------------------------------+
\033[0m""")


def print_menu():
    """Display the main menu."""
    print("\n\033[96m" + "-" * 50 + "\033[0m")
    print("\033[96m  MAIN MENU\033[0m")
    print("\033[96m" + "-" * 50 + "\033[0m")
    print("\033[92m [1] \033[0m USB cihazlarini tara")
    print("\033[92m [2] \033[0m Meridian M6 olabilecek USB cihazini detayli incele")
    print("\033[92m [3] \033[0m COM portlarini tara")
    print("\033[92m [4] \033[0m USB endpoint'lerini listele")
    print("\033[92m [5] \033[0m USB pasif veri dinle")
    print("\033[92m [6] \033[0m M6 depolamasini read-only tara")
    print("\033[92m [7] \033[0m Wi-Fi/network kesfi")
    print("\033[92m [8] \033[0m TCP port taramasi")
    print("\033[92m [9] \033[0m UDP pasif dinleme")
    print("\033[92m[10] \033[0m Network packet capture")
    print("\033[92m[11] \033[0m GNSS/NMEA stream ara")
    print("\033[92m[12] \033[0m Timing/PPS benzeri event analizi")
    print("\033[93m[13] \033[0m Tam otomatik teshis (Full Auto-Diagnostic)")
    print("\033[94m[14] \033[0m Rapor olustur")
    print("\033[91m [0] \033[0m Cikis")
    print("\033[96m" + "-" * 50 + "\033[0m")


def safe_input(prompt: str, default: str = "") -> str:
    """Get user input with a default value."""
    try:
        value = input(f"{prompt} [{default}]: ").strip()
        return value if value else default
    except (KeyboardInterrupt, EOFError):
        return default


# ---------------------------------------------
# Menu Option 1: USB Device Scan
# ---------------------------------------------
def run_usb_scan() -> Optional[dict]:
    """Scan all USB devices connected to the system."""
    if not usb_discovery:
        print("\033[91m[ERROR] usb_discovery module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  USB DEVICE SCAN')
    print('=' * 50 + '\033[0m')

    try:
        devices = usb_discovery.discover_usb_devices()
        com_ports = usb_discovery.discover_com_ports()
        device_types = usb_discovery.check_device_types()

        print(f"\n\033[92mFound {len(devices)} USB device(s)\033[0m")
        for i, dev in enumerate(devices):
            print(f"\n  [{i+1}] {dev.get('product', dev.get('name', 'Unknown Device'))}")
            if dev.get('vid'):
                print(f"      VID: {dev['vid']}  PID: {dev.get('pid', 'N/A')}")
            if dev.get('manufacturer'):
                print(f"      Manufacturer: {dev['manufacturer']}")
            if dev.get('serial'):
                print(f"      Serial: {dev['serial']}")
            if dev.get('device_id'):
                print(f"      Device ID: {dev['device_id']}")

        print(f"\n\033[92mFound {len(com_ports)} COM port(s)\033[0m")
        for port in com_ports:
            if isinstance(port, dict):
                print(f"  {port.get('port', port.get('device', 'N/A'))} - "
                      f"{port.get('description', 'N/A')}")
                if port.get('vid'):
                    print(f"    VID: {port['vid']}  PID: {port.get('pid', 'N/A')}")
            else:
                print(f"  {port}")

        print(f"\n\033[92mDevice Types:\033[0m")
        for dtype, present in device_types.items():
            status = "\033[92mYES\033[0m" if present else "\033[90mNO\033[0m"
            print(f"  {dtype}: {status}")

        return {
            'usb_devices': devices,
            'serial_ports': com_ports,
            'device_types': device_types
        }

    except Exception as e:
        print(f"\033[91m[ERROR] USB scan failed: {e}\033[0m")
        import traceback
        traceback.print_exc()
        return None


# ---------------------------------------------
# Menu Option 2: Meridian M6 Detail
# ---------------------------------------------
def run_m6_detail() -> Optional[dict]:
    """Try to find and examine the Meridian M6 device in detail."""
    if not usb_discovery:
        print("\033[91m[ERROR] usb_discovery module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  MERIDIAN M6 DETAILED INVESTIGATION')
    print('=' * 50 + '\033[0m')

    try:
        m6 = usb_discovery.find_meridian_device()
        if m6:
            print(f"\n\033[92mPotential M6 device found:\033[0m")
            for key, value in m6.items():
                print(f"  {key}: {value}")
        else:
            print("\n\033[93mNo device identified as Meridian M6.\033[0m")
            print("Listing all USB devices for manual identification:")
            devices = usb_discovery.discover_usb_devices()
            for i, dev in enumerate(devices):
                print(f"\n  [{i+1}] {dev.get('product', dev.get('name', 'Unknown'))}")
                if dev.get('vid'):
                    print(f"      VID:PID = {dev['vid']}:{dev.get('pid', '?')}")
                if dev.get('device_id'):
                    print(f"      ID: {dev['device_id']}")

        # Also show interfaces
        interfaces = usb_discovery.list_usb_interfaces_powershell()
        if interfaces:
            print(f"\n\033[92mUSB Interfaces ({len(interfaces)}):\033[0m")
            for iface in interfaces:
                if isinstance(iface, dict):
                    print(f"  - {iface.get('name', iface.get('description', str(iface)))}")
                else:
                    print(f"  - {iface}")

        return {'m6_device': m6, 'usb_interfaces': interfaces}

    except Exception as e:
        print(f"\033[91m[ERROR] M6 investigation failed: {e}\033[0m")
        import traceback
        traceback.print_exc()
        return None


# ---------------------------------------------
# Menu Option 3: COM Port Scan
# ---------------------------------------------
def run_com_scan() -> Optional[dict]:
    """Scan all COM ports."""
    if not usb_discovery:
        print("\033[91m[ERROR] usb_discovery module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  COM PORT SCAN')
    print('=' * 50 + '\033[0m')

    try:
        com_ports = usb_discovery.discover_com_ports()
        print(f"\n\033[92mFound {len(com_ports)} COM port(s):\033[0m")

        for port in com_ports:
            if isinstance(port, dict):
                print(f"\n  Port: {port.get('port', port.get('device', 'N/A'))}")
                print(f"    Description: {port.get('description', 'N/A')}")
                print(f"    HWID: {port.get('hwid', 'N/A')}")
                if port.get('vid'):
                    print(f"    VID: {port['vid']}  PID: {port.get('pid', 'N/A')}")
                if port.get('serial_number'):
                    print(f"    Serial: {port['serial_number']}")
                if port.get('manufacturer'):
                    print(f"    Manufacturer: {port['manufacturer']}")
            else:
                print(f"  {port}")

        if not com_ports:
            print("  No COM ports found.")

        return {'serial_ports': com_ports}

    except Exception as e:
        print(f"\033[91m[ERROR] COM scan failed: {e}\033[0m")
        return None


# ---------------------------------------------
# Menu Option 4: USB Endpoints
# ---------------------------------------------
def run_usb_endpoints() -> Optional[dict]:
    """List USB endpoints and interfaces."""
    if not usb_discovery:
        print("\033[91m[ERROR] usb_discovery module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  USB ENDPOINTS / INTERFACES')
    print('=' * 50 + '\033[0m')

    try:
        interfaces = usb_discovery.list_usb_interfaces_powershell()
        device_types = usb_discovery.check_device_types()

        print(f"\n\033[92mUSB Interfaces ({len(interfaces)}):\033[0m")
        for iface in interfaces:
            if isinstance(iface, dict):
                for k, v in iface.items():
                    print(f"  {k}: {v}")
                print()
            else:
                print(f"  {iface}")

        print(f"\n\033[92mDetected Device Types:\033[0m")
        for dtype, present in device_types.items():
            status = "\033[92mYES\033[0m" if present else "\033[90mNO\033[0m"
            print(f"  {dtype}: {status}")

        return {'usb_interfaces': interfaces, 'device_types': device_types}

    except Exception as e:
        print(f"\033[91m[ERROR] Endpoint listing failed: {e}\033[0m")
        return None


# ---------------------------------------------
# Menu Option 5: USB Passive Listen
# ---------------------------------------------
def run_usb_listen() -> Optional[dict]:
    """Passively listen on a USB serial port."""
    if not usb_listener:
        print("\033[91m[ERROR] usb_listener module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  USB PASSIVE LISTEN (READ-ONLY)')
    print('=' * 50 + '\033[0m')

    # List available COM ports first
    if usb_discovery:
        com_ports = usb_discovery.discover_com_ports()
        if com_ports:
            print("\nAvailable COM ports:")
            for p in com_ports:
                if isinstance(p, dict):
                    print(f"  {p.get('port', p.get('device', '?'))} - "
                          f"{p.get('description', '')}")
                else:
                    print(f"  {p}")

    port = safe_input("\nEnter COM port (e.g. COM3)", "COM3")
    
    mode = safe_input("Scan mode: [1] Single baud rate  [2] Scan all baud rates", "1")
    
    if mode == "2":
        duration = float(safe_input("Duration per baud rate (seconds)", "3"))
        print(f"\n\033[93mScanning all baud rates on {port}... (passive read-only)\033[0m")
        try:
            results = usb_listener.scan_all_bauds(port, duration)
            
            print(f"\n\033[92mBaud Rate Scan Results:\033[0m")
            for baud, info in results.items():
                data_str = "\033[92mDATA\033[0m" if info['has_data'] else "\033[90m----\033[0m"
                nmea_str = " [NMEA]" if info.get('nmea') else ""
                rtcm_str = " [RTCM]" if info.get('rtcm') else ""
                ubx_str = " [UBX]" if info.get('ubx') else ""
                print(f"  {baud:>7d} baud: {data_str}{nmea_str}{rtcm_str}{ubx_str}")
                if info.get('sample'):
                    print(f"           Sample: {info['sample'][:80]}")
            
            return {'baud_scan': results}
        except Exception as e:
            print(f"\033[91m[ERROR] Baud scan failed: {e}\033[0m")
            return None
    else:
        baud = int(safe_input("Baud rate", "115200"))
        duration = float(safe_input("Duration (seconds)", "10"))
        
        print(f"\n\033[93mListening on {port} at {baud} baud for {duration}s... "
              f"(passive read-only)\033[0m")
        
        try:
            result = usb_listener.listen_serial(port, baud, duration)
            
            raw_size = len(result.get('raw_data', b''))
            print(f"\n\033[92mReceived {raw_size} bytes\033[0m")
            
            if raw_size > 0:
                if result.get('nmea_found'):
                    print(f"  NMEA sentences: {result['nmea_found']}")
                if result.get('rtcm_found'):
                    print(f"  \033[92mRTCM3 data detected!\033[0m")
                if result.get('ubx_found'):
                    print(f"  \033[92mUBX data detected!\033[0m")
                if result.get('keywords_found'):
                    print(f"  Keywords: {result['keywords_found']}")
                
                # Show hex dump preview
                if result.get('hex_dump'):
                    print(f"\n  HEX/ASCII dump (first 512 bytes):")
                    for line in result['hex_dump'].split('\n')[:20]:
                        print(f"    {line}")
                
                # Run protocol detector if available
                if protocol_detector and result.get('raw_data'):
                    print(f"\n\033[93mRunning protocol detection...\033[0m")
                    detection = protocol_detector.detect_protocols(result['raw_data'])
                    print(protocol_detector.format_detection_report(detection))
            else:
                print("  No data received on this port/baud rate.")
            
            return {'usb_listen': result}
        except Exception as e:
            print(f"\033[91m[ERROR] Listen failed: {e}\033[0m")
            return None


# ---------------------------------------------
# Menu Option 6: Storage Scan
# ---------------------------------------------
def run_storage_scan() -> Optional[dict]:
    """Read-only scan of removable storage."""
    if not storage_scanner:
        print("\033[91m[ERROR] storage_scanner module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  READ-ONLY STORAGE SCAN')
    print('=' * 50 + '\033[0m')
    print("\033[93m!! All operations are READ-ONLY. No files will be modified.\033[0m")

    try:
        drives = storage_scanner.find_removable_drives()
        
        if not drives:
            print("\nNo drives found.")
            return None
        
        print(f"\nFound {len(drives)} drive(s):")
        for i, d in enumerate(drives):
            removable_tag = " \033[92m[REMOVABLE]\033[0m" if d.get('is_removable') else ""
            label = f" ({d['label']})" if d.get('label') else ""
            size = ""
            if d.get('total_size'):
                size_mb = d['total_size'] / (1024 * 1024)
                size = f" - {size_mb:.0f} MB"
            print(f"  [{i}] {d['drive_letter']}{label} - {d.get('fstype', '?')}{size}{removable_tag}")
        
        drive_idx = safe_input("\nSelect drive to scan (number)", "")
        if not drive_idx:
            print("No drive selected.")
            return None
        
        try:
            idx = int(drive_idx)
            if 0 <= idx < len(drives):
                drive_path = drives[idx]['drive_letter']
                print(f"\n\033[93mScanning {drive_path} (READ-ONLY)...\033[0m")
                result = storage_scanner.scan_drive_full(drive_path)
                print("\n" + storage_scanner.format_storage_report(result))
                return {'storage_scan': result}
            else:
                print("Invalid selection.")
        except ValueError:
            print("Invalid input.")
        
        return None

    except Exception as e:
        print(f"\033[91m[ERROR] Storage scan failed: {e}\033[0m")
        return None


# ---------------------------------------------
# Menu Option 7: Network Discovery
# ---------------------------------------------
def run_network_discovery() -> Optional[dict]:
    """Discover network configuration and find M6."""
    if not network_discovery:
        print("\033[91m[ERROR] network_discovery module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  NETWORK DISCOVERY')
    print('=' * 50 + '\033[0m')

    try:
        result = network_discovery.discover_network_full()
        
        # Print summary
        print("\n\033[92m" + "-" * 40 + "\033[0m")
        print("\033[92m  NETWORK DISCOVERY SUMMARY\033[0m")
        print("\033[92m" + "-" * 40 + "\033[0m")
        
        gateway = result.get('gateway', 'N/A')
        print(f"  Default Gateway: {gateway}")
        
        wifi = result.get('wifi_interface')
        if wifi:
            print(f"  Wi-Fi Interface: {wifi.get('name', 'N/A')}")
            print(f"  Wi-Fi IP: {wifi.get('ip', 'N/A')}")
        
        active = result.get('active_hosts', [])
        print(f"  Active Hosts: {len(active)}")
        
        candidates = result.get('m6_candidates', [])
        if candidates:
            print(f"\n  \033[92mPotential M6 Candidates:\033[0m")
            for c in candidates:
                print(f"    {c['ip']} - {c['reason']}")
                if c.get('http_title'):
                    print(f"      Title: {c['http_title']}")
        
        return {'network': result}

    except Exception as e:
        print(f"\033[91m[ERROR] Network discovery failed: {e}\033[0m")
        import traceback
        traceback.print_exc()
        return None


# ---------------------------------------------
# Menu Option 8: TCP Port Scan
# ---------------------------------------------
def run_port_scan() -> Optional[dict]:
    """TCP port scan on a target IP."""
    if not port_scanner:
        print("\033[91m[ERROR] port_scanner module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  TCP PORT SCAN')
    print('=' * 50 + '\033[0m')
    print("\033[93m!! Connect scan only. No data is sent to ports.\033[0m")

    # Suggest M6 IP if found
    m6_ip = ""
    net = collected_results.get('network', {})
    if net:
        candidates = net.get('m6_candidates', [])
        if candidates:
            m6_ip = candidates[0].get('ip', '')
            print(f"\n  Suggested M6 IP: {m6_ip}")
    
    target_ip = safe_input("\nEnter target IP", m6_ip or "192.168.1.1")
    
    scan_type = safe_input(
        "Scan type: [1] Priority ports only  [2] Full 1-65535 scan", "1")

    try:
        if scan_type == "2":
            timeout = float(safe_input("Timeout per port (seconds)", "0.5"))
            workers = int(safe_input("Max concurrent connections", "50"))
            results = port_scanner.scan_all_ports(target_ip, timeout, workers)
        else:
            timeout = float(safe_input("Timeout per port (seconds)", "1.0"))
            results = port_scanner.scan_priority_ports(target_ip, timeout)
        
        print("\n" + port_scanner.format_port_scan_report(results))
        
        # Check for GNSS streams on open ports
        gnss_ports = [r for r in results if r.get('has_nmea') or r.get('has_rtcm') or r.get('has_ubx')]
        if gnss_ports:
            print(f"\n\033[92m{'='*40}\033[0m")
            print(f"\033[92m  GNSS STREAMS DETECTED!\033[0m")
            print(f"\033[92m{'='*40}\033[0m")
            for gp in gnss_ports:
                protocols = []
                if gp['has_nmea']: protocols.append('NMEA')
                if gp['has_rtcm']: protocols.append('RTCM3')
                if gp['has_ubx']: protocols.append('UBX')
                print(f"  Port {gp['port']}: {', '.join(protocols)}")
        
        return {'open_ports': results}

    except Exception as e:
        print(f"\033[91m[ERROR] Port scan failed: {e}\033[0m")
        return None


# ---------------------------------------------
# Menu Option 9: UDP Passive Listen
# ---------------------------------------------
def run_udp_listen() -> Optional[dict]:
    """Passive UDP listening."""
    if not udp_listener:
        print("\033[91m[ERROR] udp_listener module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  UDP PASSIVE LISTEN')
    print('=' * 50 + '\033[0m')
    print("\033[93m!! Passive only. No UDP packets are sent.\033[0m")

    listen_type = safe_input(
        "Listen type: [1] mDNS  [2] SSDP  [3] GNSS UDP ports  [4] All", "4")
    duration = float(safe_input("Duration (seconds)", "15"))

    try:
        if listen_type == "1":
            print(f"\n\033[93mListening for mDNS announcements ({duration}s)...\033[0m")
            packets = udp_listener.listen_mdns(duration)
            print(f"  Received {len(packets)} mDNS packets")
            for pkt in packets[:10]:
                print(f"    {pkt}")
            return {'udp_mdns': packets}
        
        elif listen_type == "2":
            print(f"\n\033[93mListening for SSDP announcements ({duration}s)...\033[0m")
            packets = udp_listener.listen_ssdp(duration)
            print(f"  Received {len(packets)} SSDP packets")
            for pkt in packets[:10]:
                print(f"    {pkt}")
            return {'udp_ssdp': packets}
        
        elif listen_type == "3":
            print(f"\n\033[93mListening on GNSS UDP ports ({duration}s)...\033[0m")
            result = udp_listener.listen_all_gnss_udp(duration)
            analysis = udp_listener.analyze_udp_data(
                [pkt for pkts in result.values() if isinstance(pkts, list) for pkt in pkts])
            print(udp_listener.format_udp_report(result))
            return {'udp_gnss': result, 'udp_analysis': analysis}
        
        else:  # All
            all_results = {}
            
            print(f"\n\033[93mListening for mDNS ({duration}s)...\033[0m")
            all_results['mdns'] = udp_listener.listen_mdns(duration)
            print(f"  mDNS: {len(all_results['mdns'])} packets")
            
            print(f"\n\033[93mListening for SSDP ({duration}s)...\033[0m")
            all_results['ssdp'] = udp_listener.listen_ssdp(duration)
            print(f"  SSDP: {len(all_results['ssdp'])} packets")
            
            print(f"\n\033[93mListening on GNSS UDP ports ({duration}s)...\033[0m")
            all_results['gnss_udp'] = udp_listener.listen_all_gnss_udp(duration)
            
            return {'udp_results': all_results}

    except Exception as e:
        print(f"\033[91m[ERROR] UDP listen failed: {e}\033[0m")
        import traceback
        traceback.print_exc()
        return None


# ---------------------------------------------
# Menu Option 10: Network Packet Capture
# ---------------------------------------------
def run_packet_capture() -> Optional[dict]:
    """Passive network packet capture."""
    if not packet_capture:
        print("\033[91m[ERROR] packet_capture module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  NETWORK PACKET CAPTURE (PASSIVE)')
    print('=' * 50 + '\033[0m')
    print("\033[93m!! Requires admin/elevated privileges for raw sockets.\033[0m")

    # List interfaces
    try:
        ifaces = packet_capture.list_interfaces()
        if ifaces:
            print("\nAvailable interfaces:")
            for i, iface in enumerate(ifaces):
                print(f"  [{i}] {iface}")
    except Exception:
        pass

    target_ip = safe_input("Filter by target IP (leave empty for all)", "")
    duration = float(safe_input("Capture duration (seconds)", "30"))

    try:
        print(f"\n\033[93mCapturing packets for {duration}s...\033[0m")
        
        packets = packet_capture.capture_packets(
            duration=duration,
            target_ip=target_ip or None
        )
        
        print(f"\n\033[92mCaptured {len(packets)} packets\033[0m")
        
        # Analyze
        analysis = packet_capture.analyze_captured_packets(packets)
        print(packet_capture.format_capture_report(analysis))
        
        return {'capture_results': analysis}

    except Exception as e:
        print(f"\033[91m[ERROR] Packet capture failed: {e}\033[0m")
        print("  This usually requires running as Administrator.")
        return None


# ---------------------------------------------
# Menu Option 11: GNSS/NMEA Stream Search
# ---------------------------------------------
def run_gnss_stream() -> Optional[dict]:
    """Search for GNSS/NMEA streams on serial and TCP ports."""
    print('\n\033[94m' + '=' * 50)
    print('  GNSS/NMEA STREAM SEARCH')
    print('=' * 50 + '\033[0m')

    results = {'streams': []}
    
    source = safe_input(
        "Source: [1] Serial/COM port  [2] TCP port  [3] Both", "3")

    # Serial port search
    if source in ("1", "3") and usb_listener and usb_discovery:
        com_ports = usb_discovery.discover_com_ports()
        if com_ports:
            print(f"\n\033[93mSearching {len(com_ports)} COM port(s) for GNSS data...\033[0m")
            for p in com_ports:
                port_name = p.get('port', p.get('device', '')) if isinstance(p, dict) else str(p)
                if not port_name:
                    continue
                
                print(f"\n  Trying {port_name}...")
                try:
                    data = usb_listener.listen_serial(port_name, 115200, 5.0)
                    if data.get('nmea_found') or data.get('rtcm_found') or data.get('ubx_found'):
                        stream_info = {
                            'source': f'Serial:{port_name}',
                            'type': 'serial',
                            'port': port_name,
                            'nmea': bool(data.get('nmea_found')),
                            'rtcm': data.get('rtcm_found', False),
                            'ubx': data.get('ubx_found', False),
                            'data_size': len(data.get('raw_data', b'')),
                        }
                        results['streams'].append(stream_info)
                        
                        protocols = []
                        if stream_info['nmea']: protocols.append('NMEA')
                        if stream_info['rtcm']: protocols.append('RTCM')
                        if stream_info['ubx']: protocols.append('UBX')
                        print(f"    \033[92mGNSS data found: {', '.join(protocols)}\033[0m")
                        
                        # If NMEA found, do live parsing
                        if data.get('nmea_found') and gnss_parser and data.get('raw_data'):
                            try:
                                text = data['raw_data'].decode('ascii', errors='ignore')
                                stream = gnss_parser.NMEAStream()
                                stream.feed(text)
                                status = stream.get_current_status()
                                print(f"\n    {status}")
                                
                                rates = stream.get_message_rates()
                                if rates:
                                    print(f"    Message rates:")
                                    for msg_type, rate in rates.items():
                                        print(f"      {msg_type}: {rate:.1f} Hz")
                            except Exception:
                                pass
                    else:
                        print(f"    No GNSS data at 115200 baud")
                except Exception as e:
                    print(f"    Error: {e}")
        else:
            print("  No COM ports found.")

    # TCP port search
    if source in ("2", "3") and port_scanner:
        target_ip = safe_input("\nTarget IP for TCP stream search", 
                              collected_results.get('network', {}).get('gateway', '192.168.1.1'))
        
        open_ports = collected_results.get('open_ports', [])
        
        if not open_ports:
            print(f"\n\033[93mScanning priority ports on {target_ip}...\033[0m")
            open_ports = port_scanner.scan_priority_ports(target_ip)
        
        if open_ports:
            print(f"\n\033[93mChecking {len(open_ports)} open port(s) for GNSS streams...\033[0m")
            for op in open_ports:
                port_num = op['port'] if isinstance(op, dict) else op
                print(f"  Checking port {port_num}...")
                
                try:
                    stream_data = port_scanner.detect_gnss_stream(target_ip, port_num, 5.0)
                    if stream_data.get('has_data') and stream_data.get('detected_protocols'):
                        stream_info = {
                            'source': f'TCP:{target_ip}:{port_num}',
                            'type': 'tcp',
                            'ip': target_ip,
                            'port': port_num,
                            'protocols': stream_data['detected_protocols'],
                            'data_size': stream_data.get('data_size', 0),
                        }
                        results['streams'].append(stream_info)
                        print(f"    \033[92mData found: {', '.join(stream_data['detected_protocols'])}\033[0m")
                        
                        if stream_data.get('nmea_sentences'):
                            print(f"    NMEA preview:")
                            for sent in stream_data['nmea_sentences'][:5]:
                                print(f"      {sent}")
                except Exception as e:
                    print(f"    Error: {e}")

    # Summary
    if results['streams']:
        print(f"\n\033[92m{'='*40}\033[0m")
        print(f"\033[92m  GNSS STREAMS FOUND: {len(results['streams'])}\033[0m")
        print(f"\033[92m{'='*40}\033[0m")
        for s in results['streams']:
            print(f"  {s['source']}")
    else:
        print(f"\n\033[93mNo GNSS streams found.\033[0m")

    return results


# ---------------------------------------------
# Menu Option 12: Timing/PPS Analysis
# ---------------------------------------------
def run_timing_analysis() -> Optional[dict]:
    """Analyze timing/PPS events."""
    if not timing_analyzer:
        print("\033[91m[ERROR] timing_analyzer module not available\033[0m")
        return None

    print('\n\033[94m' + '=' * 50)
    print('  TIMING / PPS EVENT ANALYSIS')
    print('=' * 50 + '\033[0m')
    print("\033[93m!! Note: USB/network timing cannot confirm physical PPS.\033[0m")
    print("  Results indicate potential timing events only.\n")

    source = safe_input("Source: [1] Serial port monitoring  [2] Analyze existing data", "1")

    try:
        if source == "1":
            port = safe_input("COM port", "COM3")
            baud = int(safe_input("Baud rate", "115200"))
            duration = float(safe_input("Monitoring duration (seconds)", "60"))
            
            print(f"\n\033[93mMonitoring {port} for {duration}s...\033[0m")
            print("  Recording packet arrival times with high-resolution timer...")
            
            result = timing_analyzer.monitor_serial_timing(port, baud, duration)
            
            if result.get('timing_analysis'):
                ta = result['timing_analysis']
                print(f"\n\033[92mTiming Analysis Results:\033[0m")
                print(f"  Total packets: {ta.get('count', 0)}")
                print(f"  Duration: {ta.get('duration', 0):.1f}s")
                print(f"  Mean interval: {ta.get('mean_interval_ms', 0):.3f} ms")
                print(f"  Min interval: {ta.get('min_interval_ms', 0):.3f} ms")
                print(f"  Max interval: {ta.get('max_interval_ms', 0):.3f} ms")
                print(f"  Std deviation: {ta.get('std_dev_ms', 0):.3f} ms")
                print(f"  Jitter: {ta.get('jitter_ms', 0):.3f} ms")
                
                if ta.get('is_potential_pps'):
                    print(f"\n  \033[92m>> POTENTIAL PPS-LIKE EVENT DETECTED\033[0m")
                    print(f"  Packets arrive at ~1Hz with low jitter.")
                    print(f"  This MAY indicate timing pulses but cannot be confirmed")
                    print(f"  without oscilloscope measurement of physical signals.")
                
                if ta.get('is_periodic'):
                    freq = ta.get('estimated_frequency_hz', 0)
                    print(f"\n  Periodic pattern detected: ~{freq:.2f} Hz")
            
            # Also check for timing keywords in data
            if result.get('data_chunks'):
                all_data = b''.join(chunk for _, chunk in result.get('data_chunks', []) 
                                   if isinstance(chunk, bytes))
                if all_data:
                    timing_indicators = timing_analyzer.search_timing_in_stream(all_data)
                    if timing_indicators:
                        print(f"\n  \033[92mTiming indicators found in stream:\033[0m")
                        for ti in timing_indicators[:10]:
                            print(f"    [{ti.get('type', '?')}] {ti.get('description', '')}")
            
            print("\n" + timing_analyzer.format_timing_report(
                result.get('timing_analysis', {})))
            
            return {'timing_analysis': result}
        
        else:
            # Analyze existing collected data
            if 'usb_listen' in collected_results:
                listen_data = collected_results['usb_listen']
                if listen_data.get('timestamps'):
                    ts_list = [t[0] / 1e9 for t in listen_data['timestamps']]  # Convert ns to s
                    analysis = timing_analyzer.analyze_packet_timing(ts_list)
                    print("\n" + timing_analyzer.format_timing_report(analysis))
                    return {'timing_analysis': analysis}
            
            print("\033[93mNo existing data to analyze. Run USB listen (option 5) first.\033[0m")
            return None

    except Exception as e:
        print(f"\033[91m[ERROR] Timing analysis failed: {e}\033[0m")
        import traceback
        traceback.print_exc()
        return None


# ---------------------------------------------
# Menu Option 13: Full Automatic Diagnostic
# ---------------------------------------------
def run_full_diagnostic():
    """Run all safe diagnostic scans automatically."""
    global collected_results
    
    print('\n\033[93m' + '=' * 50)
    print('  FULL AUTOMATIC DIAGNOSTIC')
    print('  Running all passive scans...')
    print('=' * 50 + '\033[0m')
    
    start_time = time.time()
    
    # Step 1: USB Scan
    print("\n\033[96m[STEP 1/6] USB Device Scan\033[0m")
    r = run_usb_scan()
    if r:
        collected_results.update(r)
    
    # Step 2: COM Port Scan
    print("\n\033[96m[STEP 2/6] COM Port Scan\033[0m")
    r = run_com_scan()
    if r:
        collected_results.update(r)
    
    # Step 3: USB Endpoints
    print("\n\033[96m[STEP 3/6] USB Endpoints\033[0m")
    r = run_usb_endpoints()
    if r:
        collected_results.update(r)
    
    # Step 4: Network Discovery
    print("\n\033[96m[STEP 4/6] Network Discovery\033[0m")
    r = run_network_discovery()
    if r:
        collected_results.update(r)
    
    # Step 5: Port Scan (on M6 candidates)
    print("\n\033[96m[STEP 5/6] TCP Port Scan\033[0m")
    net = collected_results.get('network', {})
    candidates = net.get('m6_candidates', [])
    if candidates:
        for candidate in candidates[:3]:  # Scan up to 3 candidates
            target_ip = candidate.get('ip', '')
            if target_ip:
                print(f"\n  Scanning candidate: {target_ip}")
                try:
                    results = port_scanner.scan_priority_ports(target_ip, 1.0) if port_scanner else []
                    if results:
                        collected_results['open_ports'] = results
                        
                        # Check HTTP if web port found
                        http_ports = [r['port'] for r in results 
                                     if r.get('is_open') and r['port'] in [80, 443, 8080, 8081, 8888]]
                        if http_ports and http_scanner:
                            print(f"\n  \033[93mScanning HTTP services...\033[0m")
                            http_result = http_scanner.scan_http_full(target_ip, http_ports)
                            collected_results['http_services'] = http_result
                            print(http_scanner.format_http_report(http_result))
                except Exception as e:
                    print(f"  Port scan error: {e}")
    else:
        gateway = net.get('gateway', '')
        if gateway and port_scanner:
            print(f"\n  No M6 candidates found. Scanning gateway: {gateway}")
            try:
                results = port_scanner.scan_priority_ports(gateway, 1.0)
                if results:
                    collected_results['open_ports'] = results
            except Exception as e:
                print(f"  Port scan error: {e}")
    
    # Step 6: Storage Scan (if removable drives found)
    print("\n\033[96m[STEP 6/6] Storage Scan\033[0m")
    if storage_scanner:
        drives = storage_scanner.find_removable_drives()
        removable = [d for d in drives if d.get('is_removable')]
        if removable:
            for drive in removable:
                print(f"\n  Scanning {drive['drive_letter']}...")
                try:
                    result = storage_scanner.scan_drive_full(drive['drive_letter'])
                    collected_results['storage_scan'] = result
                except Exception as e:
                    print(f"  Storage scan error: {e}")
        else:
            print("  No removable drives found.")
    
    elapsed = time.time() - start_time
    print(f"\n\033[92m{'='*50}")
    print(f"  FULL DIAGNOSTIC COMPLETE ({elapsed:.1f}s)")
    print(f"{'='*50}\033[0m")
    
    # Auto-generate report
    run_generate_report()


# ---------------------------------------------
# Menu Option 14: Generate Report
# ---------------------------------------------
def run_generate_report():
    """Generate diagnostic report from collected data."""
    if not report_generator:
        print("\033[91m[ERROR] report_generator module not available\033[0m")
        return

    print('\n\033[94m' + '=' * 50)
    print('  GENERATE REPORT')
    print('=' * 50 + '\033[0m')

    if not collected_results:
        print("\033[93mNo diagnostic data collected yet. Please run some scans first.\033[0m")
        return

    try:
        # Print summary to console
        report_generator.print_summary(collected_results)
        
        # Save full report
        report_dir = report_generator.generate_full_report(collected_results)
        print(f"\n\033[92mFull report saved to: {report_dir}\033[0m")
        
        # List generated files
        if os.path.exists(report_dir):
            files = os.listdir(report_dir)
            print(f"  Generated {len(files)} file(s):")
            for f in files:
                filepath = os.path.join(report_dir, f)
                size = os.path.getsize(filepath)
                print(f"    {f} ({size} bytes)")

    except Exception as e:
        print(f"\033[91m[ERROR] Report generation failed: {e}\033[0m")
        import traceback
        traceback.print_exc()


# ---------------------------------------------
# Main Entry Point
# ---------------------------------------------
def main():
    """Main entry point."""
    global collected_results
    
    # Enable ANSI colors on Windows
    try:
        os.system('color')
    except Exception:
        pass
    
    # Enable VT100 on Windows 10+
    try:
        import ctypes
        kernel32 = ctypes.windll.kernel32
        kernel32.SetConsoleMode(kernel32.GetStdHandle(-11), 7)
    except Exception:
        pass
    
    print_banner()
    print('\033[91m+----------------------------------------------------------+')
    print('|  SAFETY: All operations are READ-ONLY.                   |')
    print('|  No data will be written to the device.                  |')
    print('|  No firmware, config, reset, or format operations.       |')
    print('+----------------------------------------------------------+\033[0m')
    
    menu_actions = {
        '1':  run_usb_scan,
        '2':  run_m6_detail,
        '3':  run_com_scan,
        '4':  run_usb_endpoints,
        '5':  run_usb_listen,
        '6':  run_storage_scan,
        '7':  run_network_discovery,
        '8':  run_port_scan,
        '9':  run_udp_listen,
        '10': run_packet_capture,
        '11': run_gnss_stream,
        '12': run_timing_analysis,
        '13': run_full_diagnostic,
        '14': run_generate_report,
    }
    
    while True:
        print_menu()
        try:
            choice = input('\n\033[96mSelect option [0-14]: \033[0m').strip()
        except (KeyboardInterrupt, EOFError):
            print('\n\033[93mExiting gracefully...\033[0m')
            break
        
        if choice == '0':
            print('\033[93mExiting...\033[0m')
            break
        
        action = menu_actions.get(choice)
        if action:
            try:
                result = action()
                if result and isinstance(result, dict):
                    collected_results.update(result)
            except KeyboardInterrupt:
                print('\n\033[93mOperation cancelled.\033[0m')
            except Exception as e:
                print(f'\033[91mUnexpected error: {e}\033[0m')
                import traceback
                traceback.print_exc()
        else:
            print('\033[91mInvalid option. Please enter 0-14.\033[0m')
        
        try:
            input('\n\033[90mPress Enter to continue...\033[0m')
        except (KeyboardInterrupt, EOFError):
            print('\n\033[93mExiting gracefully...\033[0m')
            break


if __name__ == '__main__':
    main()
