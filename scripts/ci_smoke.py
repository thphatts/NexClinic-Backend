#!/usr/bin/env python3
"""Black-box assertions for an isolated CI API; never run against production."""
import json
import sys
import urllib.error
import urllib.request
import uuid

base = sys.argv[1].rstrip('/')

def call(path, payload=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request(base + path, headers=headers,
        data=None if payload is None else json.dumps(payload).encode())
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        body = error.read()
        return error.code, json.loads(body) if body else {}

status, health = call('/actuator/health')
assert status == 200 and health['status'] == 'UP', (status, health)
status, home = call('/')
assert status == 200 and home['status'] == 'UP', (status, home)
status, _ = call('/api/v1/patients')
assert status in (401, 403), 'Protected patient endpoint exposed'
status, doctors = call('/api/v1/doctors?page_no=1&page_size=1')
assert status == 200 and doctors['code'] == 200, (status, doctors)

suffix = uuid.uuid4().hex[:12]
username = 'ci_' + suffix
password = uuid.uuid4().hex
status, registration = call('/api/v1/auth/register', {
    'name': 'CI Synthetic Patient', 'username': username,
    'email': username + '@example.invalid', 'password': password,
    'phone': '09' + str(uuid.uuid4().int % 100000000).zfill(8),
    'citizenId': str(uuid.uuid4().int % 1000000000000).zfill(12),
    'dob': '1990-01-01', 'gender': 'MALE',
})
assert status == 201 and registration['result']['token'], (status, registration.get('message'))
status, login = call('/api/v1/auth/login', {'username': username, 'password': password})
assert status == 200 and login['result']['token'], (status, login.get('message'))
status, patient = call('/api/v1/patients/me', token=login['result']['token'])
assert status == 200 and patient['result']['id'], (status, patient.get('message'))
print('PASS health, root, protected route, public doctors, registration, login, patient profile')
