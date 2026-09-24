# Meridian M6 GNSS Receiver - Passive Diagnostic Tool

**Version: v1.0.0**

## Description

This tool passively discovers and analyzes all data interfaces (USB, Wi-Fi, serial, network) of the Meridian M6 GNSS receiver **without making any modifications to the device**.

The goal is to determine what data interfaces the M6 exposes: NMEA, RTCM, UBX, raw GNSS data, timing/PPS signals, or any other accessible data output.

## ⚠ SAFETY NOTICE

**NO write operations are performed by this tool.**

- No firmware writing
- No config changes
- No device reset / factory reset
- No format operations
- No unknown binary commands sent
- All operations are **READ-ONLY** / passive listening

## Prerequisites

- **Python 3.10+** (Windows)
- **Windows 10/11**
- **Npcap** (optional, for packet capture) — Download from [npcap.com](https://npcap.com)

## Installation

```bash
cd c:\Users\hll\Desktop\egimhesabi\gnss
pip install -r requirements.txt
```

### Dependencies

| Package    | Purpose                          | Required |
|------------|----------------------------------|----------|
| pyserial   | COM port discovery & serial read | Yes      |
| psutil     | Disk and network interface info  | Yes      |
| requests   | HTTP GET requests                | Yes      |
| wmi        | Windows WMI queries              | Yes      |
| pywin32    | Windows API access               | Yes      |
| scapy      | Packet capture (needs Npcap)     | Optional |

## Usage

```bash
python main.py
```

This launches an interactive CLI menu with 14 diagnostic options.

## Menu

| #  | Option                          | Description                                            |
|----|---------------------------------|--------------------------------------------------------|
| 1  | USB cihazlarini tara            | Scan all USB devices (VID/PID, class, manufacturer)    |
| 2  | M6 detayli incele               | Find and examine potential Meridian M6 USB device       |
| 3  | COM portlarini tara             | Scan all serial COM ports                              |
| 4  | USB endpoint'lerini listele     | List USB interfaces and endpoint types                 |
| 5  | USB pasif veri dinle            | Passively listen on serial port (multi baud rate scan) |
| 6  | M6 depolamasini read-only tara | Scan removable storage for GNSS files                  |
| 7  | Wi-Fi/network kesfi            | Network discovery (IP, gateway, ARP, subnet sweep)     |
| 8  | TCP port taramasi               | TCP connect scan (priority or full 1-65535)            |
| 9  | UDP pasif dinleme               | Passive UDP listen (mDNS, SSDP, GNSS broadcasts)      |
| 10 | Network packet capture          | Passive packet capture with protocol detection         |
| 11 | GNSS/NMEA stream ara            | Search for GNSS data on serial and TCP ports           |
| 12 | Timing/PPS event analizi        | Analyze timing events, jitter, PPS-like patterns       |
| 13 | Tam otomatik teshis             | Run all scans automatically and generate report        |
| 14 | Rapor olustur                   | Generate diagnostic report from collected data         |
| 0  | Cikis                           | Exit the tool                                          |

## Reports

Diagnostic reports are saved in:

```
reports/m6_diagnostic_YYYYMMDD_HHMMSS/
├── summary.txt                  # Human-readable summary with conclusions
├── usb_devices.json             # USB device details
├── usb_interfaces.json          # USB interface/endpoint info
├── serial_ports.json            # COM port details
├── network.json                 # Network discovery results
├── open_ports.json              # TCP port scan results
├── streams.json                 # GNSS stream detection results
├── nmea.log                     # Raw NMEA data log
├── raw_usb.log                  # Raw USB data capture
├── network_capture_summary.json # Packet capture analysis
└── timing_analysis.json         # PPS/timing analysis results
```

## Project Structure

```
gnss/
├── main.py              # CLI menu and orchestrator
├── usb_discovery.py     # USB device/COM port discovery (WMI/PowerShell)
├── usb_listener.py      # Passive serial port listener
├── storage_scanner.py   # Read-only storage file scanner
├── network_discovery.py # Wi-Fi/network/ARP discovery
├── port_scanner.py      # TCP connect scanner with banner grabbing
├── udp_listener.py      # Passive UDP listener (mDNS, SSDP, GNSS)
├── packet_capture.py    # Network packet capture (scapy/raw socket)
├── http_scanner.py      # HTTP/WebSocket endpoint scanner
├── gnss_parser.py       # NMEA sentence parser with live display
├── protocol_detector.py # NMEA/RTCM3/UBX/NovAtel protocol detector
├── timing_analyzer.py   # PPS/timing event analyzer
├── report_generator.py  # Diagnostic report generator
├── requirements.txt     # Python dependencies
└── README.md            # This file
```

## License

MIT
