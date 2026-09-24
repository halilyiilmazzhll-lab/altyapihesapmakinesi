# timing_analyzer.py
# Meridian M6 GNSS Timing Analyzer
# WARNING: This script is for passive listening ONLY. NEVER send data.
# Note: USB and network timing measurements cannot confirm physical PPS output.

import statistics
import time
try:
    import serial
except ImportError:
    serial = None

def analyze_packet_timing(timestamps: list[float]) -> dict:
    """Analyze timing of received packets."""
    if len(timestamps) < 2:
        return {
            "count": len(timestamps),
            "duration": 0.0,
            "intervals": [],
            "mean_interval_ms": 0.0,
            "min_interval_ms": 0.0,
            "max_interval_ms": 0.0,
            "std_dev_ms": 0.0,
            "jitter_ms": 0.0,
            "is_periodic": False,
            "estimated_frequency_hz": 0.0,
            "is_potential_pps": False
        }
        
    intervals = [(timestamps[i] - timestamps[i-1]) * 1000 for i in range(1, len(timestamps))]
    
    mean_int = statistics.mean(intervals)
    std_dev = statistics.stdev(intervals) if len(intervals) > 1 else 0.0
    min_int = min(intervals)
    max_int = max(intervals)
    
    is_periodic = std_dev < (mean_int * 0.1) # Less than 10% deviation
    est_freq = 1000.0 / mean_int if mean_int > 0 else 0.0
    
    # Potential PPS: ~1000ms mean, tight deviation
    is_potential_pps = is_periodic and (950 < mean_int < 1050) and (std_dev < 10.0)
    
    return {
        "count": len(timestamps),
        "duration": timestamps[-1] - timestamps[0],
        "intervals": intervals,
        "mean_interval_ms": mean_int,
        "min_interval_ms": min_int,
        "max_interval_ms": max_int,
        "std_dev_ms": std_dev,
        "jitter_ms": max_int - min_int,
        "is_periodic": is_periodic,
        "estimated_frequency_hz": est_freq,
        "is_potential_pps": is_potential_pps
    }

def monitor_serial_timing(port: str, baud_rate: int = 115200, duration: float = 60.0) -> dict:
    """Monitor serial port and record timestamps of every data arrival. READ-ONLY."""
    if serial is None:
        raise ImportError("pyserial is required for monitor_serial_timing")
        
    raw_timestamps = []
    data_chunks = []
    
    try:
        # Open in read-only mode, with timeout
        with serial.Serial(port, baud_rate, timeout=0.1) as ser:
            print(f"Listening on {port} for {duration} seconds (Passive)...")
            start_time = time.perf_counter()
            while (time.perf_counter() - start_time) < duration:
                if ser.in_waiting > 0:
                    chunk = ser.read(ser.in_waiting)
                    ts = time.perf_counter()
                    raw_timestamps.append(ts)
                    data_chunks.append({"ts": ts, "data": chunk})
                time.sleep(0.001)
    except Exception as e:
        print(f"Error accessing serial port: {e}")
        
    timing_analysis = analyze_packet_timing(raw_timestamps)
    
    return {
        "raw_timestamps": raw_timestamps,
        "timing_analysis": timing_analysis,
        "data_chunks": data_chunks
    }

def search_timing_indicators_usb(usb_info: dict) -> list[dict]:
    """Search USB descriptor info for timing-related indicators."""
    keywords = ["timing", "pps", "1pps", "timepulse", "interrupt", "synchronization", "time", "pulse", "clock"]
    results = []
    
    # Search dict recursively (simplified for strings)
    def search_dict(d, path=""):
        for k, v in d.items():
            curr_path = f"{path}.{k}" if path else k
            if isinstance(v, str):
                v_lower = v.lower()
                for kw in keywords:
                    if kw in v_lower:
                        results.append({
                            "source": curr_path,
                            "indicator": kw,
                            "description": v
                        })
            elif isinstance(v, dict):
                search_dict(v, curr_path)
            elif isinstance(v, list):
                for i, item in enumerate(v):
                    if isinstance(item, dict):
                        search_dict(item, f"{curr_path}[{i}]")
                        
    search_dict(usb_info)
    return results

def search_timing_in_stream(data: bytes) -> list[dict]:
    """Search data stream for timing-related content."""
    results = []
    
    # ZDA Check
    zda_idx = data.find(b'$GPZDA')
    if zda_idx == -1: zda_idx = data.find(b'$GNZDA')
    if zda_idx != -1:
        end_idx = data.find(b'\r\n', zda_idx)
        if end_idx != -1:
            results.append({
                "type": "NMEA_ZDA",
                "content": data[zda_idx:end_idx].decode('ascii', errors='ignore'),
                "offset": zda_idx,
                "description": "NMEA Time and Date"
            })
            
    # UBX TIM Check
    offset = 0
    while offset < len(data) - 7:
        if data[offset] == 0xB5 and data[offset+1] == 0x62 and data[offset+2] == 0x0D:
            results.append({
                "type": "UBX_TIM",
                "content": f"Class 0x0D, ID 0x{data[offset+3]:02x}",
                "offset": offset,
                "description": "UBX Timing Message"
            })
        offset += 1
        
    return results

def analyze_interrupt_endpoint(timestamps: list[float], data_sizes: list[int]) -> dict:
    """Specifically analyze USB interrupt endpoint data."""
    timing = analyze_packet_timing(timestamps)
    
    is_consistent_size = False
    if data_sizes:
        # Check if > 90% of packets have same size
        most_common = statistics.mode(data_sizes)
        count_mode = data_sizes.count(most_common)
        if count_mode / len(data_sizes) > 0.9:
            is_consistent_size = True
            
    assessment = "Unlikely to be PPS."
    if timing.get("is_potential_pps") and is_consistent_size:
        assessment = "High probability of PPS. 1Hz rate with consistent payload size."
    elif timing.get("is_potential_pps"):
        assessment = "Moderate probability of PPS. 1Hz rate, but payload varies."
        
    return {
        "timing_analysis": timing,
        "is_consistent_size": is_consistent_size,
        "packet_size_stats": {
            "min": min(data_sizes) if data_sizes else 0,
            "max": max(data_sizes) if data_sizes else 0,
            "mean": statistics.mean(data_sizes) if data_sizes else 0
        },
        "assessment": assessment
    }

def format_timing_report(analysis: dict) -> str:
    """Format human-readable timing analysis report."""
    report = ["--- Timing Analysis Report ---"]
    report.append("Note: USB and network timing measurements cannot confirm physical PPS output.")
    report.append("They can only indicate potential timing events that warrant further")
    report.append("investigation with an oscilloscope.\n")
    
    t = analysis.get("timing_analysis", analysis) # Handle nested or direct
    
    report.append(f"Packets analyzed: {t.get('count', 0)}")
    if t.get("count", 0) > 1:
        report.append(f"Mean interval: {t.get('mean_interval_ms', 0):.2f} ms")
        report.append(f"Std Deviation: {t.get('std_dev_ms', 0):.2f} ms")
        report.append(f"Jitter: {t.get('jitter_ms', 0):.2f} ms")
        report.append(f"Est Frequency: {t.get('estimated_frequency_hz', 0):.2f} Hz")
        report.append(f"Periodic: {t.get('is_periodic', False)}")
        report.append(f"Potential PPS: {t.get('is_potential_pps', False)}")
        
    if "assessment" in analysis:
        report.append(f"\nAssessment: {analysis['assessment']}")
        
    return "\n".join(report)
