#!/usr/bin/env python3
# {{ ansible_managed }}

import requests
import os
import time


def config():
    host = os.getenv('KEYCLOAK_HOST')
    port = os.getenv('KEYCLOAK_PORT')
    return {'user': os.getenv('KEYCLOAK_USER'),
            'pass': os.getenv('KEYCLOAK_PASS'),
            'baseUrl': f"http://{host}:{port}"}


def wait_keycloak(params, retry_count=100, retry_delay_in_sec=0.500):
    creds = {'grant_type': 'password',
             'client_id': 'admin-cli',
             'username': params['user'],
             'password': params['pass']}

    count = 1
    while count <= retry_count:
        count += 1
        try:
            print(
                'Connecting to the Keycloak ... {0}/{1}'.format(count, retry_count))
            res = requests.post(
                f"{params['baseUrl']}/auth/realms/master/protocol/openid-connect/token", data=creds)
            print(res)
            return True
        except (Exception) as error:
            if count > retry_count:
                raise error
            print('.')
        time.sleep(retry_delay_in_sec)


if __name__ == '__main__':
    cfg = config()
    wait_keycloak(cfg)
