#!/usr/bin/env python3
"""Exercise the installed IME through Android's UI, not just the layout tables."""
import os, re, struct, subprocess, time, zlib, xml.etree.ElementTree as ET
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
screen=adb('exec-out','screencap','-p')
(out/'keyboard-before.png').write_bytes(screen)
def keyboard_top(png):
    width,height,depth,color=struct.unpack('>IIBB',png[16:26])
    assert depth==8 and color in (2,6), 'unexpected screenshot format'
    channels=4 if color==6 else 3
    data=b'';pos=8
    while pos<len(png):
        n=struct.unpack('>I',png[pos:pos+4])[0];typ=png[pos+4:pos+8]
        if typ==b'IDAT': data+=png[pos+8:pos+8+n]
        pos+=12+n
        if typ==b'IEND': break
    raw=zlib.decompress(data);stride=width*channels+1;prev=[0]*(channels*3); streak=0
    for y in range(height):
        row=raw[y*stride:(y+1)*stride]; f=row[0]; cur=[]
        for i,byte in enumerate(row[1:1+channels*3]):
            a=cur[i-channels] if i>=channels else 0;b=prev[i];c=prev[i-channels] if i>=channels else 0
            pred=0
            if f==1:pred=a
            elif f==2:pred=b
            elif f==3:pred=(a+b)//2
            elif f==4:
                p=a+b-c;pa,pb,pc=abs(p-a),abs(p-b),abs(p-c);pred=a if pa<=pb and pa<=pc else b if pb<=pc else c
            cur.append((byte+pred)&255)
        prev=cur
        streak=streak+1 if tuple(cur[channels*2:channels*2+3])==(233,238,240) else 0
        if streak==10 and y>height//4:return y-9
    raise AssertionError('keyboard background not found in screenshot')
size=adb('shell','wm','size').decode(); density=adb('shell','wm','density').decode()
w,h=map(int,re.search(r'(\d+)x(\d+)',size).groups())
dpi=int(re.search(r'(\d+)',density).group(1)); scale=dpi/160
top=keyboard_top(screen)
print('keyboard top:',top,flush=True)
app_root=dump()
practice=find(app_root,lambda n:n.attrib.get('class')=='android.widget.EditText')
practice_bottom=list(map(int,re.findall(r'\d+',practice.attrib['bounds'])))[3]
print('practice field bottom:',practice_bottom,flush=True)
assert practice_bottom<=top, 'practice field is covered by keyboard'
def point(x,y): adb('shell','input','tap',str(round(x)),str(round(y)));time.sleep(.8)
point(w*1.5/6, top+52*scale)  # 論理 tab
x=w*0.135; y=top+110*scale     # first symbol key
point(x,y)
adb('shell','input','swipe',str(round(x)),str(round(y)),str(round(x-36*scale)),str(round(y)),'180');time.sleep(.7)
point(w*.88, top+16*scale)  # Unicode / TeX
point(x,y)
(out/'keyboard.png').write_bytes(adb('exec-out','screencap','-p'))
adb('shell','input','keyevent','4');time.sleep(1)
actual=edit_text();print('typed:',repr(actual),flush=True)
assert actual=='∀∃\\forall ', 'tap/flick/TeX output: '+repr(actual)
print('PASS: installed and enabled IME; Unicode tap and left flick; LaTeX')
