#!/usr/bin/env python3

import requests
import os
import time
import json


def get_env(varname):
    value = os.getenv(varname)
    if not value:
        raise Exception(f'Undefined environment variable "{varname}"')
    return value


def config():
    host = get_env('KEYCLOAK_HOST')
    port = get_env('KEYCLOAK_PORT')
    username = get_env('KEYCLOAK_USER')
    password = get_env('KEYCLOAK_PASS')
    realm = get_env('KEYCLOAK_REALM')
    return {'user': username,
            'pass': password,
            'baseUrl': f"http://{host}:{port}",
            'realm': realm,
            'client': 'futurama-web'}


def wait_keycloak(params, retry_count=100, retry_delay_in_sec=0.500):
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
    url = f"{cfg['baseUrl']}{path}"
    try:
        response = requests.post(url, json=data, headers=headers)
        response.raise_for_status()  # raise an exception if the request was unsuccessful
    except Exception as e:
        print(f"POST {url}")
        print(f"headers: {headers}")
        print(f"BODY: {data}")
        raise e



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


def client_exists(cfg, realm, clientId):
    response = get(cfg, f'/auth/admin/realms/{realm}/clients/{realm + ":" + client}')
    print(f"Client {clientId} exists? {response.status_code}")
    return response.status_code == requests.codes.ok

if __name__ == '__main__':
    cfg = config()
    access_token = wait_keycloak(cfg)
    cfg = {**cfg, **{'access_token': access_token}}
    # print(access_token)

    realm = cfg['realm']
    client = cfg['client']

    print("====================================")
    print("    REALM")
    print("====================================")
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
    print()

    print("====================================")
    print("    CLIENT")
    print("====================================")
    with open('client.json') as json_file:
        data = json.load(json_file)
    data['id'] = realm + ":" + client
    data['clientId'] = client
    if not client_exists(cfg, realm, client):
        post(cfg, f'/auth/admin/realms/{realm}/clients', data)
        print(f'Client "{client}" created')
    else:
        put(cfg,  f'/auth/admin/realms/{realm}/clients/{realm + ":" + client}', data)
        print(f'Client "{client}" updated')


