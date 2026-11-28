"""견본 기기의 백업 설정에 "오늘 오전 8:10 백업" 상태를 넣는다(구글 로그인 없이 백업 화면을 찍으려고).

쓰는 법(앱을 끈 뒤): python3 -I scripts/store/seed_backup_prefs.py <앱 컨테이너>/Library/Application Support/datastore/backup.preferences_pb
견본만 쓰는 시뮬레이터에서만 돌린다. 실제 드라이브에는 아무것도 올라가지 않는다.
"""
import sys, time, datetime
def varint(n):
    out=b''
    while True:
        b=n&0x7f; n>>=7
        if n: out+=bytes([b|0x80])
        else: return out+bytes([b])
def ld(field, data): return varint(field<<3|2)+varint(len(data))+data
def entry(k, vfield, v):
    if vfield==5: val=ld(5, v.encode())
    elif vfield==1: val=varint(1<<3)+varint(1 if v else 0)
    elif vfield==4: val=varint(4<<3)+varint(v)
    elif vfield==3: val=varint(3<<3)+varint(v)
    e=ld(1,k.encode())+ld(2,val)
    return ld(1,e)
f=sys.argv[1]
t=int(datetime.datetime.now().replace(hour=8, minute=10, second=0, microsecond=0).timestamp()*1000)
import os; data=open(f,'rb').read() if os.path.exists(f) else b''
data+=entry("account_key",5,"store-sample")+entry("account_label",5,"견본")+entry("auto_enabled",1,True)+entry("last_success_at",4,t)+entry("last_attempt_at",4,t)+entry("photos_not_backed_up",3,0)
open(f,'wb').write(data)
