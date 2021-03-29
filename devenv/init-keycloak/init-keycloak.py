#!/usr/bin/env python3

import requests
import os
import time
import json


def config():
    host = os.getenv('KEYCLOAK_HOST')
    port = os.getenv('KEYCLOAK_PORT')
    return {'user': os.getenv('KEYCLOAK_USER'),
            'pass': os.getenv('KEYCLOAK_PASS'),
            'baseUrl': f"http://{host}:{port}",
            'realm': os.getenv('KEYCLOAK_REALM')}


def wait_keycloak(params, retry_count=10, retry_delay_in_sec=0.500):
    creds = {'grant_type': 'password',
             'client_id': 'admin-cli',
             'username': params['user'],
             'password': params['pass']}

    count = 0
    while count <= retry_count:
        count += 1
        try:
            print(
                'Connecting to the Keycloak ... {0}/{1} - {2}'.format(count, retry_count, params['baseUrl']))
            response = requests.post(
                f"{params['baseUrl']}/auth/realms/master/protocol/openid-connect/token", data=creds)
            response.raise_for_status()  # raise an exception if the request was unsuccessful

            # print(response.text)
            json = response.json()
            return json['access_token']
        except (Exception) as error:
            if count > retry_count:
                raise error
            print('.')
        time.sleep(retry_delay_in_sec)
    raise Exception('Unable to reach keycloak in time or with 200 status code')


def post(cfg, path, data):
    headers = {'Authorization': 'Bearer ' + cfg['access_token'],
               'Accept': 'application/json',
               'Content-Type': 'application/json'}
    response = requests.post(
        f"{cfg['baseUrl']}{path}", json=data, headers=headers)
    response.raise_for_status()  # raise an exception if the request was unsuccessful


def put(cfg, path, data):
    headers = {'Authorization': 'Bearer ' + cfg['access_token'],
               'Accept': 'application/json',
               'Content-Type': 'application/json'}
    response = requests.put(
        f"{cfg['baseUrl']}{path}", json=data, headers=headers)
    response.raise_for_status()  # raise an exception if the request was unsuccessful


def get(cfg, path):
    headers = {'Authorization': 'Bearer ' + cfg['access_token'],
               'Accept': 'application/json',
               'Content-Type': 'application/json'}
    return requests.get(
        f"{cfg['baseUrl']}{path}", headers=headers)


def realm_exists(cfg, realm):
    response = get(cfg, f'/auth/admin/realms/{realm}')
    return response.status_code == requests.codes.ok


if __name__ == '__main__':
    cfg = config()
    access_token = wait_keycloak(cfg)
    cfg = {**cfg, **{'access_token': access_token}}
    # print(access_token)

    realm = cfg['realm']

    with open('realm.json') as json_file:
        data = json.load(json_file)
    data['id'] = realm
    data['realm'] = realm
    if not realm_exists(cfg, realm):
        post(cfg, f'/auth/admin/realms', data)
        print(f'Realm "{realm}" created')
    else:
        put(cfg, f'/auth/admin/realms/{realm}', data)
        print(f'Realm "{realm}" updated')
