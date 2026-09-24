import subprocess
import json
import re
import sys
from typing import Optional

try:
    import serial.tools.list_ports
    HAS_PYSERIAL = True
except ImportError:
    HAS_PYSERIAL = False
    print("Warning: pyserial is not installed. COM port discovery will be limited.")

try:
    import wmi
    HAS_WMI = True
except ImportError:
    HAS_WMI = False
    print("Warning: wmi module not installed. Falling back to PowerShell for WMI queries.")

def run_powershell(command: str) -> str:
    """Run a PowerShell command and return its output."""
    try:
        result = subprocess.run(
            ["powershell", "-Command", command],
            capture_output=True,
            text=True,
            check=False
        )
        return result.stdout.strip()
    except Exception as e:
        print(f"PowerShell error: {e}")
        return ""

def discover_usb_devices() -> list[dict]:
    """Discover all USB devices using WMI and PowerShell.
    Returns list of dicts with: vid, pid, manufacturer, product, serial, 
    device_class, subclass, protocol, device_id, is_composite, status"""
    devices = []
    
    # Try using WMI module first
    if HAS_WMI:
        try:
            c = wmi.WMI()
            for device in c.Win32_PnPEntity():
                if device.DeviceID and 'USB' in device.DeviceID:
                    vid_pid = extract_vid_pid(device.DeviceID)
                    devices.append({
                        'vid': vid_pid[0],
                        'pid': vid_pid[1],
                        'manufacturer': device.Manufacturer or 'Unknown',
                        'product': device.Name or 'Unknown',
                        'serial': extract_serial(device.DeviceID),
                        'device_class': device.PNPClass or 'Unknown',
                        'subclass': '',
                        'protocol': '',
                        'device_id': device.DeviceID,
                        'is_composite': 'Composite' in str(device.Name),
                        'status': device.Status or 'Unknown'
                    })
            return devices
        except Exception as e:
            print(f"WMI error: {e}. Falling back to PowerShell.")
            
    # Fallback to PowerShell
    try:
        ps_cmd = "Get-PnpDevice -Class USB | Select-Object Status, Class, FriendlyName, InstanceId, Manufacturer | ConvertTo-Json -Compress"
        output = run_powershell(ps_cmd)
        if output:
            data = json.loads(output)
            if not isinstance(data, list):
                data = [data]
            for item in data:
                inst_id = item.get('InstanceId', '')
                vid_pid = extract_vid_pid(inst_id)
                devices.append({
                    'vid': vid_pid[0],
                    'pid': vid_pid[1],
                    'manufacturer': item.get('Manufacturer', 'Unknown'),
                    'product': item.get('FriendlyName', 'Unknown'),
                    'serial': extract_serial(inst_id),
                    'device_class': item.get('Class', 'Unknown'),
                    'subclass': '',
                    'protocol': '',
                    'device_id': inst_id,
                    'is_composite': 'Composite' in str(item.get('FriendlyName', '')),
                    'status': item.get('Status', 'Unknown')
                })
    except Exception as e:
        print(f"Failed to discover USB devices via PowerShell: {e}")
        
    return devices

def extract_vid_pid(device_id: str) -> tuple[str, str]:
    """Parse VID/PID from hardware IDs using regex: VID_([0-9A-F]{4})&PID_([0-9A-F]{4})"""
    match = re.search(r'VID_([0-9A-F]{4})&PID_([0-9A-F]{4})', device_id, re.IGNORECASE)
    if match:
        return match.group(1), match.group(2)
    return "", ""

def extract_serial(device_id: str) -> str:
    """Extract serial number from device ID if present."""
    parts = device_id.split('\\')
    if len(parts) > 2 and not '&' in parts[-1]:
        return parts[-1]
    return ""

def discover_com_ports() -> list[dict]:
    """Discover all COM ports using pyserial.
    Returns list of dicts with: port, description, hwid, vid, pid, 
    serial_number, manufacturer, product, interface"""
    ports = []
    if not HAS_PYSERIAL:
        return ports
        
    try:
        com_ports = serial.tools.list_ports.comports()
        for port in com_ports:
            ports.append({
                'port': port.device,
                'description': port.description,
                'hwid': port.hwid,
                'vid': f"{port.vid:04X}" if port.vid else "",
                'pid': f"{port.pid:04X}" if port.pid else "",
                'serial_number': port.serial_number or "",
                'manufacturer': port.manufacturer or "",
                'product': port.product or "",
                'interface': port.interface or ""
            })
    except Exception as e:
        print(f"Error discovering COM ports: {e}")
    return ports

def get_usb_device_details(device_id: str) -> dict:
    """Get detailed info about a specific USB device by its Device Instance ID.
    Returns dict with all available properties from WMI."""
    details = {}
    if HAS_WMI:
        try:
            c = wmi.WMI()
            safe_id = device_id.replace('\\', '\\\\')
            items = c.Win32_PnPEntity(DeviceID=device_id)
            if items:
                device = items[0]
                for prop in device.properties:
                    details[prop] = getattr(device, prop)
                return details
        except Exception as e:
            print(f"WMI query failed: {e}")
            
    # Fallback to PowerShell
    try:
        ps_cmd = f"Get-PnpDeviceProperty -InstanceId '{device_id}' | Select-Object KeyName, Data | ConvertTo-Json -Compress"
        output = run_powershell(ps_cmd)
        if output:
            data = json.loads(output)
            if not isinstance(data, list):
                data = [data]
            for item in data:
                if item.get('KeyName'):
                    details[item['KeyName']] = item.get('Data')
    except Exception as e:
        print(f"PowerShell details query failed: {e}")
        
    return details

def list_usb_interfaces_powershell() -> list[dict]:
    """List all USB interfaces and their types using PowerShell/WMI."""
    interfaces = []
    devices = discover_usb_devices()
    
    for dev in devices:
        cname = str(dev.get('device_class', '')).lower()
        product = str(dev.get('product', '')).lower()
        
        type_str = "Unknown"
        if "cdc" in cname or "serial" in cname or "cdc" in product:
            type_str = "CDC ACM / Serial"
        elif "disk" in cname or "mass storage" in cname:
            type_str = "Mass Storage"
        elif "image" in cname or "mtp" in product or "ptp" in product:
            type_str = "MTP/PTP"
        elif "net" in cname or "rndis" in product:
            type_str = "RNDIS"
        elif "hid" in cname:
            type_str = "HID"
        elif "vendor" in cname:
            type_str = "Vendor-specific"
            
        interfaces.append({
            'device_id': dev.get('device_id'),
            'friendly_name': dev.get('product'),
            'class': dev.get('device_class'),
            'detected_type': type_str
        })
        
    return interfaces

def check_device_types() -> dict:
    """Check what types of USB devices are present.
    Returns dict with booleans: has_serial, has_mass_storage, has_mtp, 
    has_rndis, has_hid, has_cdc_acm, has_winusb, has_vendor_specific."""
    types = {
        'has_serial': len(discover_com_ports()) > 0,
        'has_mass_storage': False,
        'has_mtp': False,
        'has_rndis': False,
        'has_hid': False,
        'has_cdc_acm': False,
        'has_winusb': False,
        'has_vendor_specific': False
    }
    
    interfaces = list_usb_interfaces_powershell()
    for iface in interfaces:
        t = iface['detected_type']
        if "CDC" in t or "Serial" in t:
            types['has_cdc_acm'] = True
            types['has_serial'] = True
        elif "Mass Storage" in t:
            types['has_mass_storage'] = True
        elif "MTP" in t:
            types['has_mtp'] = True
        elif "RNDIS" in t:
            types['has_rndis'] = True
        elif "HID" in t:
            types['has_hid'] = True
        elif "Vendor" in t:
            types['has_vendor_specific'] = True
            
    return types

def find_meridian_device() -> Optional[dict]:
    """Try to identify the Meridian M6 among connected USB devices.
    Look for 'Meridian' or 'M6' in product/manufacturer strings."""
    devices = discover_usb_devices()
    for dev in devices:
        prod = str(dev.get('product', '')).lower()
        manuf = str(dev.get('manufacturer', '')).lower()
        if 'meridian' in prod or 'meridian' in manuf or 'm6' in prod:
            return dev
    return None

def format_usb_report(devices: list[dict], com_ports: list[dict], 
                       interfaces: list[dict], device_types: dict) -> str:
    """Format a human-readable report of USB discovery results."""
    lines = ["=== USB Discovery Report ==="]
    
    lines.append("\n-- Device Types Present --")
    for k, v in device_types.items():
        if v:
            lines.append(f" - {k.replace('has_', '').upper()}")
            
    lines.append("\n-- COM Ports --")
    if not com_ports:
        lines.append(" No COM ports found.")
    for p in com_ports:
        lines.append(f" - {p['port']}: {p['description']} (VID:{p['vid']} PID:{p['pid']})")
        
    lines.append("\n-- USB Devices --")
    if not devices:
        lines.append(" No USB devices found.")
    for d in devices:
        lines.append(f" - {d['product']} [{d['manufacturer']}]")
        lines.append(f"   VID: {d['vid']} PID: {d['pid']} Serial: {d['serial']}")
        lines.append(f"   Status: {d['status']} Class: {d['device_class']}")
        
    return "\n".join(lines)
