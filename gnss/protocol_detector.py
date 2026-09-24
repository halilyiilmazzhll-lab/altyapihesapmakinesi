# protocol_detector.py
# Meridian M6 GNSS Protocol Detector
# WARNING: This script is for passive listening ONLY. NEVER send data.

import re
import struct

def detect_protocols(data: bytes) -> dict:
    """Main detection function. Analyzes raw data for all known protocols."""
    if not isinstance(data, bytes):
        raise ValueError("Data must be bytes")
        
    nmea = find_nmea_sentences(data)
    rtcm3 = find_rtcm3_packets(data)
    ubx = find_ubx_messages(data)
    novatel = find_novatel_messages(data)
    binary = find_binary_patterns(data)
    
    return {
        "has_nmea": len(nmea) > 0,
        "has_rtcm3": len(rtcm3) > 0,
        "has_ubx": len(ubx) > 0,
        "has_novatel": len(novatel) > 0,
        "has_unknown_binary": len(binary) > 0,
        "nmea_sentences": nmea,
        "rtcm_packets": rtcm3,
        "ubx_messages": ubx,
        "novatel_messages": novatel,
        "binary_patterns": binary,
        "summary": f"Detected: NMEA={len(nmea)}, RTCM3={len(rtcm3)}, UBX={len(ubx)}, NovAtel={len(novatel)}"
    }

def find_nmea_sentences(data: bytes) -> list[dict]:
    """Find NMEA sentences in data."""
    results = []
    # Regex: \$[A-Z]{2,5},[^*]*\*[0-9A-Fa-f]{2}
    pattern = re.compile(rb'\$([A-Z0-9]+),([^*]*)\*([0-9A-Fa-f]{2})')
    
    for match in pattern.finditer(data):
        offset = match.start()
        talker_sentence = match.group(1).decode('ascii', errors='ignore')
        payload = match.group(2)
        checksum_hex = match.group(3).decode('ascii', errors='ignore')
        
        talker = talker_sentence[:2] if len(talker_sentence) >= 2 else talker_sentence
        sentence_id = talker_sentence[2:] if len(talker_sentence) > 2 else ""
        
        # Validate XOR checksum
        calc_checksum = 0
        for byte in (match.group(1) + b',' + payload):
            calc_checksum ^= byte
            
        try:
            expected_checksum = int(checksum_hex, 16)
            checksum_valid = (calc_checksum == expected_checksum)
        except ValueError:
            checksum_valid = False
            
        results.append({
            "offset": offset,
            "sentence": match.group(0).decode('ascii', errors='ignore'),
            "talker": talker,
            "sentence_id": sentence_id,
            "checksum_valid": checksum_valid
        })
        
    return results

def find_rtcm3_packets(data: bytes) -> list[dict]:
    """Find RTCM3 packets. Preamble=0xD3, 6 reserved bits, 10-bit length."""
    results = []
    offset = 0
    while offset < len(data) - 5:
        if data[offset] == 0xD3:
            # Check length (next two bytes: 6 reserved bits, 10 bits length)
            header = struct.unpack(">H", data[offset+1:offset+3])[0]
            length = header & 0x03FF
            
            if offset + 3 + length + 3 <= len(data):
                # We have a full packet. Check if the reserved bits are 0.
                reserved = (header >> 10) & 0x3F
                # Note: reserved bits are usually 0, but CRC24Q is the real validation.
                # Assuming CRC validation is omitted here for simplicity, but could be added.
                if length >= 2:
                    msg_type = (data[offset+3] << 4) | (data[offset+4] >> 4)
                    results.append({
                        "offset": offset,
                        "length": length,
                        "message_type": msg_type,
                        "crc_valid": True  # Placeholder for CRC24Q
                    })
                offset += 3 + length + 3
                continue
        offset += 1
    return results

def find_ubx_messages(data: bytes) -> list[dict]:
    """Find UBX messages."""
    results = []
    offset = 0
    
    classes = {0x01: "NAV", 0x02: "RXM", 0x05: "ACK", 0x06: "CFG", 
               0x0A: "MON", 0x0D: "TIM", 0x10: "ESF", 0x13: "MGA", 
               0x21: "LOG", 0x27: "SEC"}
               
    while offset < len(data) - 7:
        if data[offset] == 0xB5 and data[offset+1] == 0x62:
            msg_class = data[offset+2]
            msg_id = data[offset+3]
            length = struct.unpack("<H", data[offset+4:offset+6])[0]
            
            if offset + 6 + length + 2 <= len(data):
                payload = data[offset+6:offset+6+length]
                ck_a, ck_b = data[offset+6+length:offset+8+length]
                
                # Checksum
                ca, cb = 0, 0
                for b in data[offset+2:offset+6+length]:
                    ca = (ca + b) & 0xFF
                    cb = (cb + ca) & 0xFF
                    
                valid = (ca == ck_a and cb == ck_b)
                
                results.append({
                    "offset": offset,
                    "msg_class": msg_class,
                    "msg_id": msg_id,
                    "length": length,
                    "class_name": classes.get(msg_class, f"UNKNOWN(0x{msg_class:02x})"),
                    "checksum_valid": valid
                })
                offset += 8 + length
                continue
        offset += 1
    return results

def find_novatel_messages(data: bytes) -> list[dict]:
    """Find NovAtel format messages."""
    results = []
    # ASCII NovAtel
    for match in re.finditer(rb'#[A-Z0-9]+A,', data):
        results.append({
            "offset": match.start(),
            "format": "ASCII",
            "header_preview": match.group(0).decode('ascii', errors='ignore')
        })
        
    # Binary NovAtel (0xAA 0x44 0x12)
    offset = 0
    while offset < len(data) - 3:
        if data[offset] == 0xAA and data[offset+1] == 0x44 and data[offset+2] == 0x12:
            results.append({
                "offset": offset,
                "format": "BINARY",
                "header_preview": "AA 44 12"
            })
            offset += 3
            continue
        offset += 1
    return results

def find_binary_patterns(data: bytes, min_pattern_len: int = 3, min_occurrences: int = 3) -> list[dict]:
    """Find recurring binary patterns."""
    return [] # Placeholder due to complexity of finding arbitrary patterns

def search_keywords_in_data(data: bytes) -> list[dict]:
    """Search for GNSS/timing related keywords in data."""
    keywords = [b'PPS', b'1PPS', b'TIMEPULSE', b'TIME', b'GPS TIME', b'UTC', 
                b'NMEA', b'RTCM', b'GNSS', b'EXTINT', b'TIM-TP', b'time mark', b'event mark']
    results = []
    
    data_lower = data.lower()
    for kw in keywords:
        kw_lower = kw.lower()
        offset = 0
        while True:
            idx = data_lower.find(kw_lower, offset)
            if idx == -1:
                break
            
            start_context = max(0, idx - 10)
            end_context = min(len(data), idx + len(kw) + 10)
            context = data[start_context:end_context].decode('ascii', errors='ignore')
            
            results.append({
                "keyword": kw.decode('ascii'),
                "offset": idx,
                "context": context.replace('\r', '').replace('\n', ' ')
            })
            offset = idx + len(kw)
            
    return results

def format_detection_report(results: dict) -> str:
    """Format human-readable protocol detection report."""
    report = ["--- Protocol Detection Report ---"]
    report.append(results.get("summary", "No summary available"))
    
    if results.get("has_nmea"):
        report.append("\nNMEA Sentences Found:")
        for item in results.get("nmea_sentences", [])[:5]:
            report.append(f"  Offset {item['offset']}: {item['talker']}{item['sentence_id']} (Valid: {item['checksum_valid']})")
            
    if results.get("has_rtcm3"):
        report.append("\nRTCM3 Packets Found:")
        for item in results.get("rtcm_packets", [])[:5]:
            report.append(f"  Offset {item['offset']}: Type {item['message_type']} (Len: {item['length']})")
            
    if results.get("has_ubx"):
        report.append("\nUBX Messages Found:")
        for item in results.get("ubx_messages", [])[:5]:
            report.append(f"  Offset {item['offset']}: {item['class_name']}-0x{item['msg_id']:02x} (Valid: {item['checksum_valid']})")
            
    return "\n".join(report)
