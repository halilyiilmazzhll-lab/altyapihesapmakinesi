import json
import os
from datetime import datetime
from typing import Union

def create_report_dir() -> str:
    """Create reports/m6_diagnostic_YYYYMMDD_HHMMSS/ directory.
    Returns the path."""
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    dir_path = os.path.join(os.getcwd(), 'reports', f'm6_diagnostic_{timestamp}')
    os.makedirs(dir_path, exist_ok=True)
    return dir_path

def save_json(data: Union[dict, list], filepath: str) -> None:
    """Save data as formatted JSON."""
    try:
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=4, ensure_ascii=False)
    except Exception as e:
        print(f"Error saving JSON to {filepath}: {e}")

def generate_summary(results: dict) -> str:
    """Generate summary.txt content."""
    
    # Extract data safely
    usb_devices = results.get('usb_devices', [])
    serial_ports = results.get('serial_ports', [])
    network = results.get('network', {})
    open_ports = results.get('open_ports', [])
    nmea_data = results.get('nmea_data', {})
    timing = results.get('timing_analysis', {})
    
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    summary = f"""============================================
MERIDIAN M6 GNSS RECEIVER - DIAGNOSTIC REPORT
Date: {timestamp}
============================================

## USB
Mass Storage: {'YES' if any('Mass Storage' in str(d) for d in usb_devices) else 'NO'}
Serial: {'YES' if len(serial_ports) > 0 else 'NO'}
CDC ACM: {'YES' if any('CDC' in str(d) for d in usb_devices) else 'NO'}
Vendor Interface: {'YES' if any('Vendor' in str(d) for d in usb_devices) else 'NO'}
Interrupt Endpoint: {'YES' if any('Interrupt' in str(d) for d in usb_devices) else 'NO'}
VID/PID: {', '.join([f"{d.get('idVendor', 'unknown')}:{d.get('idProduct', 'unknown')}" for d in usb_devices if isinstance(d, dict)])}

## NETWORK
M6 IP: {network.get('ip', 'Not found')}
HTTP: {'80 (or 8080)' if any(p in open_ports for p in [80, 8080]) else 'Not found'}
TCP GNSS Stream: {'YES' if any(p in open_ports for p in [2101, 8000, 9000, 115200]) else 'Not found'}
UDP GNSS Stream: {'Not tested' if 'udp_ports' not in results else 'Tested'}
WebSocket: {'Not tested' if 'websocket' not in network else 'Found'}
NTRIP: {'YES' if 2101 in open_ports else 'NO'}

## GNSS
NMEA detected: {'YES' if nmea_data else 'NO'}
RTCM detected: {'YES' if results.get('rtcm_detected') else 'NO'}
UBX detected: {'YES' if results.get('ubx_detected') else 'NO'}
Update rate: {nmea_data.get('update_rate', 'Unknown')} Hz

## TIMING
PPS physical output detected: NOT TESTABLE VIA USB/NETWORK
PPS-like USB event: {'YES' if timing.get('usb_pps') else 'NO'} 
PPS-like network event: {'YES' if timing.get('network_pps') else 'NO'}
UTC timing messages: {'YES' if timing.get('utc_messages') else 'NO'}
Observed jitter: {timing.get('jitter_ms', 'Unknown')} ms

## CONCLUSION
Data paths found (in order of priority):
"""
    priority = 1
    if len(serial_ports) > 0:
        summary += f"{priority}. Serial Port(s): {', '.join([p.get('device', str(p)) if isinstance(p, dict) else str(p) for p in serial_ports])}\n"
        priority += 1
    if any(p in open_ports for p in [2101, 8000, 9000]):
        summary += f"{priority}. Network TCP Stream on ports: {[p for p in open_ports if p in [2101, 8000, 9000]]}\n"
        priority += 1
    if 'udp_ports' in results:
        summary += f"{priority}. UDP Broadcast/Stream detected\n"
        priority += 1
        
    if priority == 1:
        summary += "No clear data paths discovered.\n"
        
    return summary

def generate_full_report(results: dict) -> str:
    """Generate all report files and return the report directory path."""
    dir_path = create_report_dir()
    
    # Save raw results
    if 'usb_devices' in results:
        save_json(results['usb_devices'], os.path.join(dir_path, 'usb_devices.json'))
    if 'serial_ports' in results:
        save_json(results['serial_ports'], os.path.join(dir_path, 'serial_ports.json'))
    if 'network' in results:
        save_json(results['network'], os.path.join(dir_path, 'network.json'))
    if 'open_ports' in results:
        save_json(results['open_ports'], os.path.join(dir_path, 'open_ports.json'))
    if 'nmea_data' in results:
        save_json(results['nmea_data'], os.path.join(dir_path, 'streams.json'))
    if 'timing_analysis' in results:
        save_json(results['timing_analysis'], os.path.join(dir_path, 'timing_analysis.json'))
        
    summary_text = generate_summary(results)
    try:
        with open(os.path.join(dir_path, 'summary.txt'), 'w', encoding='utf-8') as f:
            f.write(summary_text)
    except Exception as e:
        print(f"Error saving summary.txt: {e}")
        
    return dir_path

def print_summary(results: dict) -> None:
    """Print summary to console."""
    summary_text = generate_summary(results)
    print("\n" + summary_text)

if __name__ == "__main__":
    pass
