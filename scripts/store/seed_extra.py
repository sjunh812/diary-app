"""seed_ios.py <기기> <언어> <그림> mood 뒤에 돌려 스토어 캡처용 견본을 다듬는다.

쓰는 법(앱을 끈 뒤): python3 -I scripts/store/seed_extra.py <앱 DB 경로> <27일 장소 이름>
11월 빈 날 채우기, 새 기분 6종 섞기, 목록 첫 화면 사진, 백업 기록(사진까지 모두)을 넣는다.
견본만 쓰는 시뮬레이터에서만 돌린다.
"""
import sqlite3, sys, uuid, random
db = sqlite3.connect(sys.argv[1]); place = sys.argv[2]
r = random.Random(3)
moods = ['excited','good','laugh','proud','calm','cool','meh','bored','tired','good','laugh','sad']
shown = {x[0] for x in db.execute("select plain_text from entry where date between '2026-11-01' and '2026-11-13'")}
texts = [t for t in db.execute("select distinct body_html, plain_text from entry where date between '2026-10-01' and '2026-10-31'").fetchall() if t[1] not in shown]
have = {x[0] for x in db.execute("select date from entry")}
k = 0
for d in range(14, 28):
    ds = f"2026-11-{d:02d}"
    if ds in have or d in (19, 24): continue
    b, p = texts[k % len(texts)]; k += 1
    t = f"{r.choice([7,8,12,13,19,20,21,22,23]):02d}:{r.randint(0,59):02d}"
    db.execute("insert into entry (id,date,time,body_html,plain_text,mood,weather,place_name,is_favorite,created_at,updated_at) values (?,?,?,?,?,?,?,NULL,0,0,0)",
               (str(uuid.uuid4()), ds, t, b, p, r.choice(moods), r.choice(['sunny','partly_cloudy','cloudy'])))
new = ['excited','proud','calm','bored','excited','proud']
for (i,) in db.execute("select id from entry where date>='2026-10-01' and date<'2026-11-14' and date not in ('2026-11-08','2026-11-07')").fetchall():
    if r.random() < 0.3: db.execute("update entry set mood=? where id=?", (r.choice(new), i))
fix = {"2026-11-27":"excited","2026-11-26":"good","2026-11-25":"good","2026-11-23":"laugh","2026-11-22":"sad","2026-11-21":"calm","2026-11-20":"proud","2026-11-13":"excited","2026-11-12":"calm","2026-11-11":"tired","2026-11-10":"proud","2026-11-09":"bored","2026-11-05":"anxious","2026-10-22":"annoyed"}
for d, m in fix.items(): db.execute("update entry set mood=? where date=?", (m, d))
pick = db.execute("select sha256,file_name,byte_size from media group by sha256 order by sha256").fetchall()
for d, n in [("2026-11-27", 0), ("2026-11-26", 3), ("2026-11-21", 5)]:
    e = db.execute("select id from entry where date=?", (d,)).fetchone()
    if e and not db.execute("select 1 from media where entry_id=?", (e[0],)).fetchone():
        s, f, b = pick[n % len(pick)]
        db.execute("insert into media (id,entry_id,sort_order,status,file_name,sha256,width,height,byte_size,created_at) values (?,?,0,'normal',?,?,1600,1200,?,0)", (str(uuid.uuid4()), e[0], f, s, b))
db.execute("update entry set place_name=? where date='2026-11-27'", (place,))
db.execute("insert or replace into backup_media select 'store-sample', sha256, 'sample-'||sha256, max(byte_size), 1795000000000 from media group by sha256")
db.commit()
print(db.execute("select count(*) from entry").fetchone()[0], "편")
