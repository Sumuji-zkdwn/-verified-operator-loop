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
print('input methods:',adb('shell','ime','list','-a','-s').decode(),flush=True)
print('service query:',adb('shell','cmd','package','query-services','-a','android.view.InputMethod').decode(),flush=True)
adb('shell','ime','enable','jp.example.mathime/.MathInputService')
adb('shell','ime','set','jp.example.mathime/.MathInputService')
adb('shell','am','start','-n','jp.example.mathime/.MainActivity')
time.sleep(3)
root=dump()
tap(find(root,lambda n:n.attrib.get('class')=='android.widget.EditText'))
time.sleep(2)
root=dump()
tap(description(root,'論理 の記号'))
root=dump()
key=description(root,'タップ ∀、左 ∃、上 ∄')
tap(key)
root=dump(); key=description(root,'タップ ∀、左 ∃、上 ∄')
x,y=center(key)
adb('shell','input','swipe',str(x),str(y),str(x-100),str(y),'180');time.sleep(.7)
assert edit_text()=='∀∃', 'tap/flick output: '+repr(edit_text())
root=dump();tap(description(root,'文字と LaTeX の出力を切り替える'))
root=dump();tap(description(root,'タップ ∀、左 ∃、上 ∄'))
assert edit_text()=='∀∃\\forall ', 'LaTeX output: '+repr(edit_text())
adb('exec-out','screencap','-p')
(out/'keyboard.png').write_bytes(adb('exec-out','screencap','-p'))
print('PASS: installed and enabled IME; Unicode tap and left flick; LaTeX output')
