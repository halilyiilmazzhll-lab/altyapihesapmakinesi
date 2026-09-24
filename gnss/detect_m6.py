"""
USB Plug/Unplug Device Detection Script
Detects Meridian M6 by comparing USB state before and after unplugging.
"""
import sys
import os
import json
import time
import subprocess

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import usb_discovery


def get_usb_snapshot():
    """Take a snapshot of all USB devices and COM ports."""
    devices = usb_discovery.discover_usb_devices()
    com_ports = usb_discovery.discover_com_ports()
    
    # Create sets of device IDs for comparison
    device_ids = set()
    device_map = {}
    for d in devices:
        did = d.get('device_id', str(d))
        device_ids.add(did)
        device_map[did] = d
    
    com_set = set()
    com_map = {}
    for p in com_ports:
        if isinstance(p, dict):
            port_name = p.get('port', p.get('device', str(p)))
        else:
            port_name = str(p)
        com_set.add(port_name)
        com_map[port_name] = p
    
    return device_ids, device_map, com_set, com_map


def print_device(dev, indent="  "):
    """Print device info nicely."""
    if isinstance(dev, dict):
        name = dev.get('product', dev.get('name', dev.get('description', 'Unknown')))
        print(f"{indent}Name: {name}")
        if dev.get('vid'):
            print(f"{indent}VID: {dev['vid']}  PID: {dev.get('pid', 'N/A')}")
        if dev.get('manufacturer'):
            print(f"{indent}Manufacturer: {dev['manufacturer']}")
        if dev.get('serial'):
            print(f"{indent}Serial: {dev['serial']}")
        if dev.get('device_id'):
            print(f"{indent}Device ID: {dev['device_id']}")
        if dev.get('device_class'):
            print(f"{indent}Class: {dev['device_class']}")
        if dev.get('hwid'):
            print(f"{indent}HWID: {dev['hwid']}")
    else:
        print(f"{indent}{dev}")


def main():
    print("=" * 60)
    print("  MERIDIAN M6 USB PLUG/UNPLUG DETECTION")
    print("  Cihazi tak-cikar yaparak tespit eder")
    print("=" * 60)
    
    # Step 1: Snapshot with device PLUGGED IN
    print("\n[ADIM 1] Meridian M6 USB'ye TAKILI olsun.")
    input("M6 takili ise ENTER'a basin... ")
    
    print("\nUSB cihazlari taraniyor (TAKILI durumda)...")
    plugged_ids, plugged_map, plugged_coms, plugged_com_map = get_usb_snapshot()
    print(f"  {len(plugged_ids)} USB cihazi, {len(plugged_coms)} COM portu bulundu.\n")
    
    # Step 2: Ask user to UNPLUG
    print("[ADIM 2] Simdi Meridian M6'yi USB'den CIKARIN.")
    input("M6'yi cikardiktan sonra ENTER'a basin... ")
    
    # Wait a moment for Windows to detect removal
    print("\nWindows'un cihazi algimasi bekleniyor (3 saniye)...")
    time.sleep(3)
    
    print("USB cihazlari taraniyor (CIKARILMIS durumda)...")
    unplugged_ids, unplugged_map, unplugged_coms, unplugged_com_map = get_usb_snapshot()
    print(f"  {len(unplugged_ids)} USB cihazi, {len(unplugged_coms)} COM portu bulundu.\n")
    
    # Step 3: Compare
    print("=" * 60)
    print("  KARSILASTIRMA SONUCU")
    print("=" * 60)
    
    # Devices that disappeared = M6 devices
    disappeared_ids = plugged_ids - unplugged_ids
    appeared_ids = unplugged_ids - plugged_ids  # shouldn't happen, but check
    
    disappeared_coms = plugged_coms - unplugged_coms
    appeared_coms = unplugged_coms - plugged_coms
    
    if disappeared_ids:
        print(f"\n>>> CIKARILAN CIHAZLAR ({len(disappeared_ids)} adet) - BUNLAR M6! <<<\n")
        
        m6_devices = []
        vid_pid_set = set()
        
        for did in sorted(disappeared_ids):
            dev = plugged_map.get(did, {'device_id': did})
            print(f"  ---")
            print_device(dev)
            m6_devices.append(dev)
            
            # Extract VID/PID
            vid = dev.get('vid', '')
            pid = dev.get('pid', '')
            if vid and pid:
                vid_pid_set.add((vid, pid))
            print()
        
        # Summary of unique VID/PID pairs
        if vid_pid_set:
            print("\n" + "=" * 60)
            print("  M6 USB TANITICI BILGILERI (VID:PID)")
            print("=" * 60)
            for vid, pid in sorted(vid_pid_set):
                print(f"\n  VID: {vid}  (0x{vid})")
                print(f"  PID: {pid}  (0x{pid})")
                
                # Find all interfaces with this VID:PID
                related = [d for d in m6_devices 
                          if d.get('vid') == vid and d.get('pid') == pid]
                if related:
                    print(f"  Iliskili arayuzler ({len(related)}):")
                    for r in related:
                        name = r.get('product', r.get('name', r.get('description', '?')))
                        print(f"    - {name}")
                        if r.get('device_id'):
                            did = r['device_id']
                            # Extract interface number if present
                            if 'MI_' in did:
                                mi = did.split('MI_')[1][:2]
                                print(f"      Interface: MI_{mi}")
        
        # Categorize device types
        print("\n" + "=" * 60)
        print("  M6 CIHAZ TIPI ANALIZI")
        print("=" * 60)
        
        categories = {
            'ADB': [],
            'Mass Storage': [],
            'Serial/CDC': [],
            'HID': [],
            'Composite': [],
            'RNDIS/Network': [],
            'WinUSB/Vendor': [],
            'Storage Volume': [],
            'WPD (MTP/PTP)': [],
            'Other': []
        }
        
        for dev in m6_devices:
            did = str(dev.get('device_id', ''))
            name = str(dev.get('product', dev.get('name', dev.get('description', ''))))
            manufacturer = str(dev.get('manufacturer', ''))
            all_text = (did + name + manufacturer).upper()
            
            categorized = False
            
            if 'ADB' in all_text:
                categories['ADB'].append(dev)
                categorized = True
            if 'MASS STORAGE' in all_text or 'USBSTOR' in did or 'FILE-STOR' in all_text:
                categories['Mass Storage'].append(dev)
                categorized = True
            if 'CDC' in all_text or 'SERIAL' in all_text or 'COM' in all_text or 'MODEM' in all_text:
                categories['Serial/CDC'].append(dev)
                categorized = True
            if 'HID' in all_text:
                categories['HID'].append(dev)
                categorized = True
            if 'COMPOSITE' in all_text or 'BILESIK' in all_text:
                categories['Composite'].append(dev)
                categorized = True
            if 'RNDIS' in all_text or 'ETHERNET' in all_text or 'NET' in all_text:
                categories['RNDIS/Network'].append(dev)
                categorized = True
            if 'WINUSB' in all_text or 'VENDOR' in all_text:
                categories['WinUSB/Vendor'].append(dev)
                categorized = True
            if 'STORAGE\\VOLUME' in did:
                categories['Storage Volume'].append(dev)
                categorized = True
            if 'WPDBUSENUM' in did or 'MTP' in all_text or 'PTP' in all_text:
                categories['WPD (MTP/PTP)'].append(dev)
                categorized = True
            
            if not categorized:
                categories['Other'].append(dev)
        
        for cat_name, devs in categories.items():
            if devs:
                print(f"\n  [{cat_name}] - {len(devs)} cihaz")
                for d in devs:
                    name = d.get('product', d.get('name', d.get('description', '?')))
                    print(f"    * {name}")
                    if d.get('device_id'):
                        print(f"      {d['device_id']}")
        
        print("\n" + "=" * 60)
        print("  OZET")
        print("=" * 60)
        print(f"  Toplam kaybolan USB cihazi: {len(disappeared_ids)}")
        print(f"  Benzersiz VID:PID: {len(vid_pid_set)}")
        for cat_name, devs in categories.items():
            if devs:
                print(f"  {cat_name}: EVET ({len(devs)})")
        
    else:
        print("\n  Hicbir USB cihazi kaybolmadi!")
        print("  M6 USB'den cikarilmamis olabilir veya")
        print("  cihaz farkli bir arayuz kullaniyor olabilir.")
    
    if disappeared_coms:
        print(f"\n  Kaybolan COM portlari ({len(disappeared_coms)}):")
        for com in sorted(disappeared_coms):
            port_info = plugged_com_map.get(com, com)
            print(f"    {com}")
            if isinstance(port_info, dict):
                print_device(port_info, "      ")
    
    if appeared_ids:
        print(f"\n  [NOT] Cikarma sonrasi yeni gorunen cihazlar ({len(appeared_ids)}):")
        for did in sorted(appeared_ids):
            dev = unplugged_map.get(did, {'device_id': did})
            print_device(dev, "    ")
    
    # Step 4: Optionally plug back in to verify
    print("\n" + "=" * 60)
    verify = input("\nM6'yi tekrar takip dogrulama yapmak ister misiniz? (e/h): ").strip().lower()
    
    if verify in ('e', 'y', 'evet', 'yes'):
        print("\n[ADIM 3] Meridian M6'yi tekrar TAKIN.")
        input("M6'yi taktiktan sonra ENTER'a basin... ")
        
        print("\nWindows'un cihazi algimasi bekleniyor (5 saniye)...")
        time.sleep(5)
        
        print("USB cihazlari taraniyor (TEKRAR TAKILI)...")
        replug_ids, replug_map, replug_coms, replug_com_map = get_usb_snapshot()
        
        # Devices that reappeared
        reappeared = replug_ids - unplugged_ids
        
        if reappeared:
            print(f"\n>>> TEKRAR GORUNEN CIHAZLAR ({len(reappeared)}) - DOGRULANDI! <<<\n")
            for did in sorted(reappeared):
                dev = replug_map.get(did, {'device_id': did})
                print(f"  ---")
                print_device(dev)
                print()
            
            # Cross-reference with disappeared
            confirmed = reappeared & disappeared_ids
            print(f"\n  Dogrulanmis M6 cihazlari: {len(confirmed)}/{len(disappeared_ids)}")
        
        reappeared_coms = replug_coms - unplugged_coms
        if reappeared_coms:
            print(f"\n  Tekrar gorunen COM portlari: {sorted(reappeared_coms)}")
    
    # Save results
    results = {
        'timestamp': time.strftime('%Y-%m-%d %H:%M:%S'),
        'disappeared_count': len(disappeared_ids),
        'disappeared_devices': [plugged_map.get(did, {'device_id': did}) for did in sorted(disappeared_ids)],
        'disappeared_com_ports': sorted(disappeared_coms),
        'vid_pid_pairs': [{'vid': v, 'pid': p} for v, p in sorted(vid_pid_set)] if 'vid_pid_set' in dir() else [],
    }
    
    results_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 
                                'reports', 'm6_usb_detection.json')
    os.makedirs(os.path.dirname(results_path), exist_ok=True)
    with open(results_path, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False, default=str)
    print(f"\nSonuclar kaydedildi: {results_path}")
    
    print("\n" + "=" * 60)
    print("  TAMAMLANDI")
    print("=" * 60)


if __name__ == '__main__':
    main()
