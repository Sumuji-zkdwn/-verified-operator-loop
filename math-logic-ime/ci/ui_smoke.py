#!/usr/bin/env python3
"""Exercise the installed IME through Android's UI, not just the layout tables."""
import os, re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path

out=Path('math-logic-ime/build/ui')
out.mkdir(parents=True, exist_ok=True)
def adb(*args, check=True):
    result=subprocess.run(['adb',*args],stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    if result.returncode and check:
        print('ADB failed:',args,'stdout:',result.stdout.decode(errors='replace'),'stderr:',result.stderr.decode(errors='replace'),flush=True)
        raise RuntimeError('adb command failed')
    return result.stdout

def dump():
    adb('shell','uiautomator','dump','--compressed','/sdcard/math-ime-ui.xml')
    xml=adb('exec-out','cat','/sdcard/math-ime-ui.xml')
    (out/'window.xml').write_bytes(xml)
    return ET.fromstring(xml)

def find(root, predicate):
    for node in root.iter('node'):
        if predicate(node): return node
    raise AssertionError('UI element not found; see window.xml')

def center(node):
    b=list(map(int,re.findall(r'\d+',node.attrib['bounds'])))
    return (b[0]+b[2])//2, (b[1]+b[3])//2

def tap(node):
    x,y=center(node); adb('shell','input','tap',str(x),str(y));time.sleep(.7)

def description(root, text):
    return find(root,lambda n:text in n.attrib.get('content-desc',''))

def edit_text():
    root=dump()
    node=find(root,lambda n:n.attrib.get('class')=='android.widget.EditText')
    return node.attrib.get('text','')

adb('install','-r','math-logic-ime/build/math-flick.apk')
print('installed:', adb('shell','pm','list','packages','jp.example.mathime').decode(), flush=True)
ime_id=None
for attempt in range(30):
    methods=adb('shell','ime','list','-a','-s').decode()
    ime_id=next((line.strip() for line in methods.splitlines() if line.startswith('jp.example.mathime/')),None)
    if ime_id: break
    time.sleep(1)
if not ime_id:
    logs=adb('logcat','-d','-t','4000').decode(errors='replace')
    print('input methods:',methods,flush=True)
    print('IME log:', '\n'.join(line for line in logs.splitlines() if 'InputMethod' in line or 'mathime' in line),flush=True)
    raise AssertionError('IME service did not register')
print('registered:',ime_id,flush=True)
adb('shell','ime','enable',ime_id)
adb('shell','ime','set',ime_id)
adb('shell','am','start','-n','jp.example.mathime/.MainActivity')
time.sleep(3)
root=dump()
tap(find(root,lambda n:n.attrib.get('class')=='android.widget.EditText'))
time.sleep(2)
# The platform UI dumper exposes the focused app window, not the separate IME
# window. Capture a screenshot and use screen/density-derived IME coordinates.
(out/'keyboard-before.png').write_bytes(adb('exec-out','screencap','-p'))
print('window information:',adb('shell','dumpsys','window','windows').decode(errors='replace')[-18000:],flush=True)
size=adb('shell','wm','size').decode(); density=adb('shell','wm','density').decode()
w,h=map(int,re.search(r'(\d+)x(\d+)',size).groups())
dpi=int(re.search(r'(\d+)',density).group(1)); scale=dpi/160
bottom=h-round(24*scale)
top=bottom-round(398*scale)
def point(x,y): adb('shell','input','tap',str(round(x)),str(round(y)));time.sleep(.8)
point(w*1.5/6, top+52*scale)  # 論理 tab
x=w*0.135; y=top+110*scale     # first symbol key
point(x,y)
adb('shell','input','swipe',str(round(x)),str(round(y)),str(round(x-36*scale)),str(round(y)),'180');time.sleep(.7)
(out/'keyboard.png').write_bytes(adb('exec-out','screencap','-p'))
adb('shell','input','keyevent','4');time.sleep(1)
actual=edit_text();print('typed:',repr(actual),flush=True)
assert actual=='∀∃', 'tap/flick output: '+repr(actual)
print('PASS: installed and enabled IME; Unicode tap and left flick')
