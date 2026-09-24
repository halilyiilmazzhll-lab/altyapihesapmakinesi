import re
import urllib.parse
try:
    import requests
    REQUESTS_AVAILABLE = True
except ImportError:
    REQUESTS_AVAILABLE = False
    print("Requests library not found. (Requests kütüphanesi bulunamadı)")

COMMON_PATHS = ['/', '/status', '/info', '/device', '/gnss', '/gps', '/nmea',
                '/rtcm', '/stream', '/api', '/config', '/api/status', 
                '/api/gnss', '/api/device', '/api/info', '/index.html',
                '/api/v1/status', '/api/v1/gnss']

JS_KEYWORDS = ['WebSocket', 'ws://', 'wss://', 'nmea', 'rtcm', 'gps', 'gnss',
               'serial', 'tcp', 'udp', 'socket', 'pps', 'timepulse',
               'stream', 'ntrip']

def safe_get(url: str, timeout: float = 5.0) -> 'requests.Response':
    """Wrapper function that only does GET.
    SAFETY: NEVER POST/PUT/DELETE/PATCH.
    """
    if not REQUESTS_AVAILABLE:
        raise ImportError("requests library is required for HTTP scanning")
    # This ensures only requests.get is used.
    return requests.get(url, timeout=timeout, verify=False)

def scan_http_service(ip: str, port: int, use_https: bool = False, 
                      timeout: float = 5.0) -> dict:
    """Scan an HTTP service. GET requests only. NEVER POST/PUT/DELETE.
    Returns: is_http, server_header, title, paths_found (list of 
    {path, status_code, content_type, content_preview})"""
    scheme = "https" if use_https else "http"
    base_url = f"{scheme}://{ip}:{port}"
    
    result = {
        'is_http': False,
        'server_header': '',
        'title': '',
        'paths_found': [],
        'html_content': ''
    }
    
    if not REQUESTS_AVAILABLE:
        return result
        
    import urllib3
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
    
    try:
        # Initial check on root
        resp = safe_get(f"{base_url}/", timeout=timeout)
        result['is_http'] = True
        result['server_header'] = resp.headers.get('Server', '')
        result['html_content'] = resp.text
        
        # Extract title
        title_match = re.search(r'<title>(.*?)</title>', resp.text, re.IGNORECASE)
        if title_match:
            result['title'] = title_match.group(1).strip()
            
        # Scan common paths
        for path in COMMON_PATHS:
            try:
                p_resp = safe_get(f"{base_url}{path}", timeout=timeout)
                if p_resp.status_code != 404:
                    result['paths_found'].append({
                        'path': path,
                        'status_code': p_resp.status_code,
                        'content_type': p_resp.headers.get('Content-Type', ''),
                        'content_preview': p_resp.text[:100].replace('\n', ' ')
                    })
            except requests.RequestException:
                pass
                
    except requests.RequestException:
        pass
        
    return result

def find_javascript_files(html_content: str, base_url: str) -> list[str]:
    """Extract JavaScript file URLs from HTML.
    Look for <script src="..."> tags.
    Returns list of full URLs."""
    js_urls = []
    # regex to find src attributes in script tags
    matches = re.finditer(r'<script[^>]+src=["\']([^"\']+)["\']', html_content, re.IGNORECASE)
    for match in matches:
        src = match.group(1)
        full_url = urllib.parse.urljoin(base_url, src)
        js_urls.append(full_url)
    return js_urls

def download_and_analyze_js(js_url: str, timeout: float = 5.0) -> dict:
    """Download a JS file (GET only) and search for keywords.
    Returns: url, size, keywords_found (list of {keyword, context_line}),
    websocket_endpoints (list of ws:// or wss:// URLs found)"""
    result = {
        'url': js_url,
        'size': 0,
        'keywords_found': [],
        'websocket_endpoints': [],
        'content': ''
    }
    
    if not REQUESTS_AVAILABLE:
        return result
        
    try:
        # SAFETY: safe_get is only performing GET request
        resp = safe_get(js_url, timeout=timeout)
        if resp.status_code == 200:
            content = resp.text
            result['size'] = len(content)
            result['content'] = content
            lines = content.split('\n')
            
            for keyword in JS_KEYWORDS:
                for line_idx, line in enumerate(lines):
                    if keyword.lower() in line.lower():
                        result['keywords_found'].append({
                            'keyword': keyword,
                            'context_line': line.strip()[:100]
                        })
                        
            ws_matches = re.findall(r'(ws[s]?://[^\s\'"]+)', content)
            result['websocket_endpoints'].extend(ws_matches)
    except requests.RequestException:
        pass
        
    return result

def scan_websocket_endpoints(html_content: str, js_contents: list[str]) -> list[str]:
    """Find WebSocket endpoint URLs in HTML and JavaScript."""
    endpoints = set()
    
    ws_pattern = r'(ws[s]?://[^\s\'"]+)'
    
    for match in re.findall(ws_pattern, html_content):
        endpoints.add(match)
        
    for content in js_contents:
        for match in re.findall(ws_pattern, content):
            endpoints.add(match)
            
    return list(endpoints)

def scan_http_full(ip: str, ports: list[int] = None) -> dict:
    """Complete HTTP scan across all given ports.
    Default ports: [80, 443, 8080, 8081, 8888, 3000, 5000].
    Returns: services (list of service dicts), js_analysis (list),
    websocket_endpoints (list)"""
    if ports is None:
        ports = [80, 443, 8080, 8081, 8888, 3000, 5000]
        
    results = {
        'services': [],
        'js_analysis': [],
        'websocket_endpoints': []
    }
    
    all_js_contents = []
    all_html_contents = ""
    
    for port in ports:
        use_https = (port == 443 or port == 8443)
        service_info = scan_http_service(ip, port, use_https)
        if service_info['is_http']:
            results['services'].append({
                'port': port,
                'title': service_info['title'],
                'server': service_info['server_header'],
                'paths_count': len(service_info['paths_found']),
                'paths': service_info['paths_found']
            })
            
            all_html_contents += service_info['html_content'] + "\n"
            
            base_url = f"{'https' if use_https else 'http'}://{ip}:{port}"
            js_files = find_javascript_files(service_info['html_content'], base_url)
            
            for js_url in js_files:
                js_info = download_and_analyze_js(js_url)
                if js_info['size'] > 0:
                    results['js_analysis'].append({
                        'url': js_url,
                        'keywords_count': len(js_info['keywords_found']),
                        'keywords': js_info['keywords_found']
                    })
                    results['websocket_endpoints'].extend(js_info['websocket_endpoints'])
                    all_js_contents.append(js_info['content'])
                    
    endpoints_from_regex = scan_websocket_endpoints(all_html_contents, all_js_contents)
    results['websocket_endpoints'] = list(set(results['websocket_endpoints'] + endpoints_from_regex))
    
    return results

def format_http_report(results: dict) -> str:
    """Format human-readable HTTP scan report."""
    report = "--- HTTP Scanner Report ---\n"
    report += f"Services Found: {len(results['services'])}\n"
    for s in results['services']:
        report += f"  Port {s['port']} - {s['title']} (Server: {s['server']})\n"
        for p in s['paths'][:3]:
            report += f"    - {p['path']} (HTTP {p['status_code']})\n"
            
    report += f"\nJavaScript Files Analyzed: {len(results['js_analysis'])}\n"
    for js in results['js_analysis']:
        if js['keywords_count'] > 0:
            report += f"  {js['url']}: {js['keywords_count']} interesting keywords found\n"
            
    report += f"\nWebSocket Endpoints Found: {len(results['websocket_endpoints'])}\n"
    for ws in results['websocket_endpoints']:
        report += f"  - {ws}\n"
        
    return report

if __name__ == "__main__":
    print("HTTP Scanner loaded in safe GET-only mode.")
