#!/usr/bin/env python3

import os
import time
import subprocess


# source .env
def load_env():
    env = {}
    with open(".env") as fp:
        for line in fp.readlines():
            try:
                k, v = line.strip().split('=', 1)
            except:
                continue  # bad line format, skip it
            env[k] = v
    return env


def tab(indent):
    return "  " * indent


def list_deployment_dir(env) -> str:
    process = subprocess.run(['docker', 'exec',
                              '-it',  f'{env["COMPOSE_PROJECT_NAME"]}_keycloak_1',
                              'ls',
                              '/opt/jboss/keycloak/standalone/deployments'],
                             stdout=subprocess.PIPE,
                             universal_newlines=True)
    if process.returncode != 0:
        raise Exception(f"Failed to list deployment content, got: {process}")
    return process.stdout


def is_ear_already_deployed(env):
    r = list_deployment_dir(env)
    if r.find("keycloak-remote-user.ear") >= 0:
        return True
    return False


def wait_until_undeployed(env, indent, retry_count=100, retry_delay_in_sec=0.500):
    count = 0
    while count <= retry_count:
        count += 1
        if list_deployment_dir(env).find("keycloak-remote-user.ear.undeployed") >= 0:
            print(f"{tab(indent)}undeployed file detected {count}/{retry_count}")
            return True
        print(f"{tab(indent)}undeployed file still missing {count}/{retry_count}")
        time.sleep(retry_delay_in_sec)
    raise Exception('Undeploy not achieved in time')

def wait_until_deployed(env, indent, retry_count=100, retry_delay_in_sec=0.500):
    count = 0
    while count <= retry_count:
        count += 1
        if list_deployment_dir(env).find("keycloak-remote-user.ear.deployed") >= 0:
            print(f"{tab(indent)}deployed file detected {count}/{retry_count}")
            return True
        print(f"{tab(indent)}deployed file still missing {count}/{retry_count}")
        time.sleep(retry_delay_in_sec)
    raise Exception('Deploy not detected in time')


def remove_existing_ear(env, indent):
    if not is_ear_already_deployed(env):
        print(f"{tab(indent)}EAR already present? No")
        return None
    print(f"{tab(indent)}EAR already present? Yes")
    print(f"{tab(indent+1)}Removing previous EAR")
    process = subprocess.run(['docker', 'exec',
                              '-it',  f'{env["COMPOSE_PROJECT_NAME"]}_keycloak_1',
                              'rm',
                              '/opt/jboss/keycloak/standalone/deployments/keycloak-remote-user.ear'],
                             stdout=subprocess.PIPE,
                             universal_newlines=True)
    if process.returncode != 0:
        raise Exception(f"Failed to remove EAR, got: {process}")
    wait_until_undeployed(env, indent+1)


def deploy_ear(env, indent):
    print(f"{tab(indent)}Install EAR in keycloak")
    # Note: '/plugin/' folder is a volume @see docker-compose.yml
    process = subprocess.run(['docker', 'exec',
                              '-it',  f'{env["COMPOSE_PROJECT_NAME"]}_keycloak_1',
                              'cp',
                              '/plugins/ear/target/keycloak-remote-user.ear',
                              '/opt/jboss/keycloak/standalone/deployments'],
                             stdout=subprocess.PIPE,
                             universal_newlines=True)
    if process.returncode != 0:
        raise Exception(f"Failed to install EAR, got: {process}")
    print(f"{tab(indent + 1)}EAR installed")
    wait_until_deployed(env, indent+1)


def init_keycloak(env, indent):
    print(f"{tab(indent)}Configure keycloak")
    process = subprocess.run(['docker-compose', '-f', 'docker-compose.initk.yml', 'up'],
                             stdout=subprocess.PIPE,
                             universal_newlines=True)
    if process.returncode != 0:
        raise Exception(f"Failed to configure keycloak, got: {process}")
    print(f"{tab(indent + 1)}Keycloak configured")


if __name__ == '__main__':
    env = load_env()
    remove_existing_ear(env, 0)
    deploy_ear(env, 0)
    init_keycloak(env, 0)
