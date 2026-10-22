"""Starts a real Paper or Folia server with the plugin and has a player with the mod play on it.

    plugin-smoke.py <paper|folia> <minecraft version> <plugin jar>

The player (plugin-bot.js, a mineflayer bot; NODE_PATH must reach mineflayer) says hello, throws a stack
and dies. Fails unless the plugin sent the thrown stack's despawn clock marked as the player's own, a
death-loot timer, not too many of them, counted the player in /despawnindicator, and logged no exception.
The server jar comes from PaperMC's download API; a download that fails on the way is tried again.
"""
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import urllib.request

project, mc, plugin = sys.argv[1], sys.argv[2], os.path.realpath(sys.argv[3])
here = os.path.dirname(os.path.realpath(__file__))
work = os.path.join(os.environ.get('RUNNER_TEMP', '/tmp'), f'server-{project}-{mc}')
port = '25565'
failures = []


def fail(message):
    print(f'::error title=Plugin on {project} {mc}::{message}')
    failures.append(message)


def get(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'despawn-indicator-ci'})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(request, timeout=300) as response:
                return response.read()
        except OSError as e:
            if getattr(e, 'code', 500) < 500 and getattr(e, 'code', 500) != 429 or attempt == 3:
                raise
            print(f'::warning title=Network failure ({project} {mc})::{url}: {e}')
            time.sleep(20)


shutil.rmtree(work, ignore_errors=True)
os.makedirs(os.path.join(work, 'plugins'))
build = json.loads(get(f'https://fill.papermc.io/v3/projects/{project}/versions/{mc}/builds/latest'))
download = build['downloads']['server:default']
server = get(download['url'])
if hashlib.sha256(server).hexdigest() != download['checksums']['sha256']:
    sys.exit(f'{download["name"]}: wrong checksum')
print(f'== {download["name"]}')
open(os.path.join(work, 'server.jar'), 'wb').write(server)
shutil.copy(plugin, os.path.join(work, 'plugins'))
open(os.path.join(work, 'eula.txt'), 'w').write('eula=true\n')
flat = 'flat' if mc.startswith(('1.16', '1.17')) else 'minecraft:flat'
open(os.path.join(work, 'server.properties'), 'w').write(
    f'online-mode=false\nserver-port={port}\nlevel-type={flat}\nspawn-protection=0\nview-distance=4\n'
    'enforce-secure-profile=false\ngenerate-structures=false\nspawn-monsters=false\nspawn-animals=false\n')

# Paper 1.16.5 asks for Java 16 at most; it runs on 17
java = ['java', '-Xmx2G', '-DPaper.IgnoreJavaVersion=true', '-jar', 'server.jar', '--nogui']
process = subprocess.Popen(java, cwd=work, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True)
log = []


def read():
    for line in process.stdout:
        log.append(line)


threading.Thread(target=read, daemon=True).start()


def wait(pattern, seconds):
    end = time.time() + seconds
    while time.time() < end:
        if any(re.search(pattern, line) for line in log):
            return True
        if process.poll() is not None:
            return False
        time.sleep(0.5)
    return False


def console(command):
    process.stdin.write(command + '\n')
    process.stdin.flush()


result = None
if not wait(r'Done \(', 400):
    fail('the server did not start')
else:
    bot = subprocess.Popen(['node', os.path.join(here, 'plugin-bot.js'), port, mc], stdout=subprocess.PIPE,
                           stderr=subprocess.STDOUT, text=True)
    if wait(r'smoke joined the game', 60):
        console('op smoke')
    said = []
    for line in bot.stdout:
        said.append(line)
        if line.startswith('READY'):
            console('despawnindicator')
    bot.wait(60)
    found = [line for line in said if line.startswith('RESULT ')]
    if found:
        result = json.loads(found[0][len('RESULT '):])
    else:
        fail('the bot printed no result:\n' + ''.join(said[-40:]))
    time.sleep(1)
if process.poll() is None:
    console('stop')
    try:
        process.wait(120)
    except subprocess.TimeoutExpired:
        process.kill()
text = ''.join(log)
open(os.path.join(work, 'server.log'), 'w').write(text)

if result is not None:
    print('== Sent to the player: ' + json.dumps(result)[:3000])
    for error in result['errors']:
        fail('bot: ' + error)
    clocks = [c for message in result['clocks'] for c in message]
    # The thrown stack: the player's own (MINE = 4), counting down from five minutes
    if not any(c['flags'] & 4 and c.get('lifespan') == 6000 and 5000 < c['remaining'] <= 6000 for c in clocks):
        fail('no despawn clock for the thrown stack')
    if not result['loot'] or result['loot'][0]['present']:
        fail('no "no death loot" answer to the hello')
    deaths = [m for m in result['loot'] if m['present']]
    if not any(m['items'] >= 1 and m['dimension'] == 'minecraft:overworld' and 5000 < m['remaining'] <= 6000
               for m in deaths):
        fail('no death-loot timer')
    # The client counts down by itself: the timer is sent again only when it would be wrong
    if len(deaths) > 6:
        fail(f'the death-loot timer was sent {len(deaths)} times in a few seconds')
if result is not None and not re.search(r'Players with the mod: 1 of 1', text):
    fail('/despawnindicator did not count the player')
for pattern in (r'while enabling DespawnIndicator', r'kasper\.despawnindicator', r'\[DespawnIndicator\].*(Exception|Error)',
                r'Could not pass event \S+ to DespawnIndicator'):
    for line in log:
        if re.search(pattern, line):
            fail('the server log shows: ' + line.strip()[:300])
            break
if failures:
    sys.exit(1)
print(f'== The plugin works on {project} {mc}')
