import random
import string
from datetime import datetime, timedelta

def generate_random_ip():
    return '.'.join(str(random.randint(0, 255)) for _ in range(4))

def generate_random_jitter():
    return random.randint(0, 50)

def generate_random_request():
    methods = ['GET', 'POST', 'PUT', 'DELETE']
    resources = ['/page1', '/page2', '/page3', '/image.jpg', '/script.js']
    http_versions = ['HTTP/1.0', 'HTTP/1.1', 'HTTP/2.0']
    method = random.choice(methods)
    resource = random.choice(resources)
    http_version = random.choice(http_versions)
    return f'"{method} {resource} {http_version}"'

def generate_random_status():
    return random.choice([200, 301, 404, 500])

def generate_random_bytes():
    return random.randint(100, 10000)

def generate_log_entry(dt):
    ip = generate_random_ip()
    request = generate_random_request()
    status = generate_random_status()
    bytes_sent = generate_random_bytes()
    referer = '-'
    user_agent = '"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36"'
    return f'{ip} - - [{dt.strftime("%d/%b/%Y:%H:%M:%S")} +0000] {request} {status} {bytes_sent} {referer} {user_agent}\n'

# Define the output file path
output_file_path = 'access.log'

# Define the start datetime
timestamp = datetime(2024, 2, 10, 12, 0, 0)  # Example start datetime, adjust as needed

# Define the number of log entries to generate (adjust as needed)
num_entries = 10000000  # Change this to generate more or fewer entries

# Generate and write log entries to file
with open(output_file_path, 'w') as f:
    for _ in range(num_entries):
        timestamp = timestamp + timedelta(milliseconds=generate_random_jitter())
        log_entry = generate_log_entry(timestamp)
        f.write(log_entry)

print(f"Log file generated successfully: {output_file_path}")
