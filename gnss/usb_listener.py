import time
import re
import string
from typing import Any

try:
    import serial
    HAS_PYSERIAL = True
except ImportError:
    HAS_PYSERIAL = False
    print("Warning: pyserial is not installed. Serial communication will not work.")

BAUD_RATES = [9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600]

NMEA_PATTERNS = ['$GPGGA', '$GNGGA', '$GPRMC', '$GNRMC', '$GPGSA', '$GNGSA', 
                 '$GSV', '$GST', '$ZDA']
RTCM3_PREAMBLE = 0xD3
UBX_SYNC = bytes([0xB5, 0x62])
KEYWORDS = ['PPS', '1PPS', 'TIMEPULSE', 'TIME', 'GPS TIME', 'UTC', 'NMEA', 'RTCM', 'GNSS']

def listen_serial(port: str, baud_rate: int = 115200, duration: float = 10.0) -> dict:
    """Listen on a serial port for the specified duration. READ-ONLY.
    Returns dict with raw_data, hex_dump, ascii_dump, timestamps, nmea_found, 
    rtcm_found, ubx_found, keywords_found"""
    result: dict[str, Any] = {
        'raw_data': b'',
        'hex_dump': '',
        'ascii_dump': '',
        'timestamps': [],
        'nmea_found': [],
        'rtcm_found': False,
        'ubx_found': False,
        'keywords_found': []
    }
    
    if not HAS_PYSERIAL:
        print("Error: pyserial not installed.")
        return result
        
    try:
        # SAFETY: The serial port must be opened with write=False (by not writing anything).
        # We only use ser.read(). Never call ser.write()!
        ser = serial.Serial(port, baud_rate, timeout=0.1)
        
        start_time = time.time()
        while (time.time() - start_time) < duration:
            if ser.in_waiting > 0:
                chunk = ser.read(ser.in_waiting)
                if chunk:
                    # Record high-resolution timestamp
                    ts = time.perf_counter_ns()
                    result['timestamps'].append((ts, len(chunk)))
                    result['raw_data'] += chunk
            time.sleep(0.01)
            
        ser.close()
        
        if result['raw_data']:
            analysis = analyze_raw_data(result['raw_data'])
            result['nmea_found'] = analysis['nmea_sentences']
            result['rtcm_found'] = len(analysis['rtcm_packets']) > 0
            result['ubx_found'] = len(analysis['ubx_messages']) > 0
            result['keywords_found'] = analysis['keywords_found']
            
            result['hex_dump'] = analysis['hex_preview']
            result['ascii_dump'] = analysis['ascii_content']
            
    except Exception as e:
        print(f"Error listening on {port}: {e}")
        
    return result

def scan_all_bauds(port: str, duration_per_baud: float = 3.0) -> dict:
    """Try all baud rates and report which ones produce valid data."""
    results = {}
    for baud in BAUD_RATES:
        print(f"Testing {port} at {baud} baud (Passive Read-Only)...")
        res = listen_serial(port, baud, duration_per_baud)
        
        has_data = len(res['raw_data']) > 0
        results[baud] = {
            'has_data': has_data,
            'nmea': len(res['nmea_found']) > 0,
            'rtcm': res['rtcm_found'],
            'ubx': res['ubx_found'],
            'sample': res['ascii_dump'][:100] if has_data else ''
        }
    return results

def analyze_raw_data(data: bytes) -> dict:
    """Analyze raw bytes for GNSS protocol signatures."""
    result = {
        'nmea_sentences': [],
        'rtcm_packets': [],
        'ubx_messages': [],
        'keywords_found': [],
        'ascii_content': '',
        'hex_preview': ''
    }
    
    if not data:
        return result
        
    # Extract ASCII content safely for NMEA and keyword search
    printable = set(bytes(string.printable, 'ascii'))
    ascii_chars = [chr(b) if b in printable else '.' for b in data]
    result['ascii_content'] = ''.join(ascii_chars)
    
    # NMEA Search
    for pattern in NMEA_PATTERNS:
        if pattern in result['ascii_content']:
            result['nmea_sentences'].append(pattern)
            
    # Keywords Search
    upper_ascii = result['ascii_content'].upper()
    for kw in KEYWORDS:
        if kw in upper_ascii:
            result['keywords_found'].append(kw)
            
    # RTCM3 Search
    for i in range(len(data) - 3):
        if data[i] == RTCM3_PREAMBLE:
            result['rtcm_packets'].append(i)
            
    # UBX Search
    for i in range(len(data) - 2):
        if data[i:i+2] == UBX_SYNC:
            result['ubx_messages'].append(i)
            
    # Generate Hex preview
    result['hex_preview'] = format_hex_ascii_dump(data[:512])
    return result

def format_hex_ascii_dump(data: bytes, bytes_per_line: int = 16) -> str:
    """Format data as side-by-side HEX + ASCII dump with offsets."""
    lines = []
    for i in range(0, len(data), bytes_per_line):
        chunk = data[i:i+bytes_per_line]
        hex_str = ' '.join(f'{b:02X}' for b in chunk)
        # Pad hex_str if chunk is less than bytes_per_line
        hex_padding = ' ' * (3 * (bytes_per_line - len(chunk)))
        
        ascii_str = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in chunk)
        
        lines.append(f"{i:04X}  {hex_str}{hex_padding}  {ascii_str}")
    return '\n'.join(lines)

def save_raw_log(data: bytes, timestamps: list, filepath: str) -> None:
    """Save raw captured data with timestamps to a log file."""
    try:
        # Passive logging. Safe file write on local system for diagnostic output.
        with open(filepath, 'wb') as f:
            f.write(b'=== RAW GNSS CAPTURE LOG ===\n')
            f.write(data)
        print(f"Log saved successfully to {filepath}")
    except Exception as e:
        print(f"Failed to save log to {filepath}: {e}")
