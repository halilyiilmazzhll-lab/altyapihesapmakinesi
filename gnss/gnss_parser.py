# gnss_parser.py
# Meridian M6 GNSS NMEA Parser
# WARNING: This script is for passive listening ONLY.

import time

def validate_nmea_checksum(sentence: str) -> bool:
    """Validate NMEA XOR checksum."""
    if not sentence.startswith('$') or '*' not in sentence:
        return False
    
    content, checksum_hex = sentence[1:].rsplit('*', 1)
    checksum_hex = checksum_hex.strip()
    
    if len(checksum_hex) != 2:
        return False
        
    calc = 0
    for char in content:
        calc ^= ord(char)
        
    try:
        return calc == int(checksum_hex, 16)
    except ValueError:
        return False

def parse_nmea(sentence: str) -> dict | None:
    """Parse any NMEA sentence."""
    if not validate_nmea_checksum(sentence):
        return None
        
    content = sentence[1:sentence.rfind('*')]
    fields = content.split(',')
    
    if not fields:
        return None
        
    talker_sentence = fields[0]
    sentence_id = talker_sentence[2:] if len(talker_sentence) > 2 else talker_sentence
    
    result = {"sentence_id": sentence_id, "talker": talker_sentence[:2]}
    
    parsers = {
        "GGA": parse_gga,
        "RMC": parse_rmc,
        "GSA": parse_gsa,
        "GSV": parse_gsv,
        "GST": parse_gst,
        "ZDA": parse_zda
    }
    
    if sentence_id in parsers:
        try:
            parsed_data = parsers[sentence_id](fields[1:])
            result.update(parsed_data)
        except Exception:
            pass # Ignore malformed fields gracefully
            
    return result

def convert_nmea_coord(value: str, direction: str) -> float | None:
    """Convert NMEA coordinate (DDDMM.MMMM) to decimal degrees."""
    if not value or not direction:
        return None
    try:
        if '.' in value:
            deg_len = value.index('.') - 2
        else:
            deg_len = len(value) - 2
            
        degrees = float(value[:deg_len])
        minutes = float(value[deg_len:])
        decimal = degrees + (minutes / 60.0)
        
        if direction in ['S', 'W']:
            decimal = -decimal
        return decimal
    except ValueError:
        return None

def parse_gga(fields: list[str]) -> dict:
    """Parse GGA sentence."""
    return {
        "utc": fields[0] if len(fields) > 0 else "",
        "lat": convert_nmea_coord(fields[1], fields[2]) if len(fields) > 2 else None,
        "lon": convert_nmea_coord(fields[3], fields[4]) if len(fields) > 4 else None,
        "fix_quality": int(fields[5]) if len(fields) > 5 and fields[5] else 0,
        "num_satellites": int(fields[6]) if len(fields) > 6 and fields[6] else 0,
        "hdop": float(fields[7]) if len(fields) > 7 and fields[7] else 0.0,
        "altitude": float(fields[8]) if len(fields) > 8 and fields[8] else 0.0,
        "altitude_unit": fields[9] if len(fields) > 9 else "",
        "geoid_sep": float(fields[10]) if len(fields) > 10 and fields[10] else 0.0,
        "diff_age": float(fields[12]) if len(fields) > 12 and fields[12] else None,
        "diff_station": fields[13] if len(fields) > 13 else ""
    }

def parse_rmc(fields: list[str]) -> dict:
    """Parse RMC sentence."""
    return {
        "utc": fields[0] if len(fields) > 0 else "",
        "status": fields[1] if len(fields) > 1 else "",
        "lat": convert_nmea_coord(fields[2], fields[3]) if len(fields) > 3 else None,
        "lon": convert_nmea_coord(fields[4], fields[5]) if len(fields) > 5 else None,
        "speed_knots": float(fields[6]) if len(fields) > 6 and fields[6] else 0.0,
        "course": float(fields[7]) if len(fields) > 7 and fields[7] else 0.0,
        "date": fields[8] if len(fields) > 8 else "",
        "magnetic_var": float(fields[9]) if len(fields) > 9 and fields[9] else 0.0,
        "mode": fields[11] if len(fields) > 11 else ""
    }

def parse_gsa(fields: list[str]) -> dict:
    """Parse GSA sentence."""
    sats = [s for s in fields[2:14] if s] if len(fields) > 13 else []
    return {
        "mode": fields[0] if len(fields) > 0 else "",
        "fix_type": int(fields[1]) if len(fields) > 1 and fields[1] else 1,
        "satellite_ids": sats,
        "pdop": float(fields[14]) if len(fields) > 14 and fields[14] else 0.0,
        "hdop": float(fields[15]) if len(fields) > 15 and fields[15] else 0.0,
        "vdop": float(fields[16]) if len(fields) > 16 and fields[16] else 0.0
    }

def parse_gsv(fields: list[str]) -> dict:
    """Parse GSV sentence."""
    sats = []
    if len(fields) >= 3:
        for i in range(3, len(fields)-3, 4):
            if i+3 < len(fields) and fields[i]:
                sats.append({
                    "prn": fields[i],
                    "elevation": float(fields[i+1]) if fields[i+1] else 0.0,
                    "azimuth": float(fields[i+2]) if fields[i+2] else 0.0,
                    "snr": float(fields[i+3]) if fields[i+3] else 0.0
                })
    return {
        "total_messages": int(fields[0]) if len(fields) > 0 and fields[0] else 0,
        "message_number": int(fields[1]) if len(fields) > 1 and fields[1] else 0,
        "total_sats": int(fields[2]) if len(fields) > 2 and fields[2] else 0,
        "satellites": sats
    }

def parse_gst(fields: list[str]) -> dict:
    """Parse GST sentence."""
    return {
        "utc": fields[0] if len(fields) > 0 else "",
        "rms_residual": float(fields[1]) if len(fields) > 1 and fields[1] else 0.0,
        "semi_major_error": float(fields[2]) if len(fields) > 2 and fields[2] else 0.0,
        "semi_minor_error": float(fields[3]) if len(fields) > 3 and fields[3] else 0.0,
        "orientation": float(fields[4]) if len(fields) > 4 and fields[4] else 0.0,
        "lat_error": float(fields[5]) if len(fields) > 5 and fields[5] else 0.0,
        "lon_error": float(fields[6]) if len(fields) > 6 and fields[6] else 0.0,
        "alt_error": float(fields[7]) if len(fields) > 7 and fields[7] else 0.0
    }

def parse_zda(fields: list[str]) -> dict:
    """Parse ZDA sentence."""
    return {
        "utc": fields[0] if len(fields) > 0 else "",
        "day": int(fields[1]) if len(fields) > 1 and fields[1] else 0,
        "month": int(fields[2]) if len(fields) > 2 and fields[2] else 0,
        "year": int(fields[3]) if len(fields) > 3 and fields[3] else 0,
        "local_tz_hours": int(fields[4]) if len(fields) > 4 and fields[4] else 0,
        "local_tz_minutes": int(fields[5]) if len(fields) > 5 and fields[5] else 0
    }

class NMEAStream:
    """Live NMEA stream analyzer."""
    def __init__(self):
        self.message_counts = {}
        self.message_times = {}
        self.last_position = {}
        self.start_time = None
        self.buffer = ""
        
    def feed(self, data: str, timestamp: float = None):
        """Feed raw data. Extract and parse NMEA sentences."""
        if self.start_time is None:
            self.start_time = time.perf_counter()
            
        if timestamp is None:
            timestamp = time.perf_counter()
            
        self.buffer += data
        lines = self.buffer.split('\n')
        self.buffer = lines.pop()
        
        for line in lines:
            line = line.strip()
            if not line:
                continue
                
            parsed = parse_nmea(line)
            if parsed:
                sid = parsed["sentence_id"]
                self.message_counts[sid] = self.message_counts.get(sid, 0) + 1
                
                if sid not in self.message_times:
                    self.message_times[sid] = []
                self.message_times[sid].append(timestamp)
                
                # Keep last 100 timestamps for rate calculation
                if len(self.message_times[sid]) > 100:
                    self.message_times[sid] = self.message_times[sid][-100:]
                    
                if sid in ["GGA", "RMC"]:
                    if parsed.get("lat") is not None:
                        self.last_position = parsed
                        
    def get_message_rates(self) -> dict:
        """Calculate message rates in Hz for each sentence type."""
        rates = {}
        for sid, times in self.message_times.items():
            if len(times) >= 2:
                duration = times[-1] - times[0]
                if duration > 0:
                    rates[sid] = (len(times) - 1) / duration
                else:
                    rates[sid] = 0.0
            else:
                rates[sid] = 0.0
        return rates
        
    def get_current_status(self) -> str:
        """Get formatted current GNSS status string."""
        rates = self.get_message_rates()
        rates_str = ", ".join(f"{k}: {v:.1f}Hz" for k, v in rates.items())
        
        utc = self.last_position.get("utc", "N/A")
        lat = self.last_position.get("lat", "N/A")
        lon = self.last_position.get("lon", "N/A")
        alt = self.last_position.get("altitude", "N/A")
        fix = self.last_position.get("fix_quality", "N/A")
        
        if isinstance(lat, float): lat = f"{lat:.5f}"
        if isinstance(lon, float): lon = f"{lon:.5f}"
        if isinstance(alt, float): alt = f"{alt:.1f}m"
        
        return f"UTC:{utc} | Lat:{lat} Lon:{lon} Alt:{alt} | Fix:{fix} | Rates: [{rates_str}]"
        
    def get_statistics(self) -> dict:
        """Get overall statistics: total messages, rates, duration."""
        duration = 0.0
        if self.start_time is not None:
            duration = time.perf_counter() - self.start_time
            
        return {
            "total_messages": sum(self.message_counts.values()),
            "message_counts": self.message_counts,
            "rates_hz": self.get_message_rates(),
            "duration_seconds": duration
        }
