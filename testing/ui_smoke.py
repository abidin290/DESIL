import subprocess, xml.etree.ElementTree as ET, re
from pathlib import Path
adb=r'C:\Users\ACER\AppData\Local\Android\Sdk\platform-tools\adb.exe'
def run(*args): return subprocess.check_output([adb,'-s','emulator-5554',*args])
def tap(text):
 run('shell','uiautomator','dump','/sdcard/uismoke.xml')
 tree=ET.fromstring(run('shell','cat','/sdcard/uismoke.xml'))
 node=next(n for n in tree.iter('node') if n.get('text')==text)
 x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds')))
 run('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2))
def screenshot(name):
 run('shell','uiautomator','dump','/sdcard/uismoke.xml')
 Path('testing/'+name+'.png').write_bytes(run('exec-out','screencap','-p'))
tap('DEPAN RUMAH');screenshot('v22-preview-fit')
tap('+');screenshot('v22-preview-zoom')
tap('Tutup preview');tap('Riwayat');screenshot('v22-history')
print('PASS: open photo preview, zoom control, close preview, open history')
