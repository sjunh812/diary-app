"""스토어 스크린샷용 견본 일기를 iOS 시뮬레이터 앱 DB에 넣는다.

쓰는 법(앱을 끈 뒤):
  python3 -I scripts/store/seed_ios.py <기기 UDID> <ko|en|ja> <그림 폴더>
그림 폴더는 scripts/store/make_photos.swift가 만든 곳이다.
견본만 쓰는 새 시뮬레이터에서만 돌린다. 그 기기의 일기와 사진을 모두 지우고 다시 넣는다.
"""

import hashlib
import shutil
import sqlite3
import subprocess
import sys
import uuid
from datetime import datetime
from pathlib import Path

# (날짜, 시각, 기분, 날씨, 장소 번호, 즐겨찾기, 그림들)
PLAN = [
    ("2026-11-13", "08:42", "good", "sunny", None, False, []),
    ("2026-11-08", "18:15", "laugh", "sunny", 0, True, ["sea", "dunes", "clouds"]),
    ("2026-11-12", "22:30", "cool", "cloudy", None, False, []),
    ("2026-11-11", "23:05", "tired", "rain", None, False, []),
    ("2026-11-10", "19:20", "good", "sunny", 1, False, ["lavender"]),
    ("2026-11-07", "15:10", "laugh", "sunny", None, True, ["hills", "clouds"]),
    ("2026-11-09", "22:40", "meh", "mostly_cloudy", None, False, []),
    ("2026-10-25", "21:25", "good", "sunny", None, False, ["clouds"]),
    ("2026-11-05", "21:10", "sad", "rain", None, False, []),
    ("2026-11-04", "20:35", "cool", "partly_cloudy", 2, False, []),
    ("2026-11-03", "14:23", "laugh", "partly_cloudy", None, False, []),
    ("2026-10-18", "10:50", "good", "sunny", 0, True, ["sea"]),
    ("2026-11-01", "22:05", "meh", "cloudy", None, False, []),
    ("2026-10-31", "20:05", "good", "mostly_cloudy", None, False, []),
    ("2026-11-02", "23:10", "laugh", "partly_cloudy", None, False, []),
    ("2026-10-29", "22:26", "tired", "cloudy", None, False, []),
    ("2026-10-28", "18:48", "good", "sunny", 2, False, ["dunes"]),
    ("2026-10-26", "08:30", "cool", "sunny", 1, False, []),
    ("2026-10-24", "17:01", "good", "sunny", None, False, []),
    ("2026-10-22", "22:28", "bad", "cloudy", None, False, []),
    ("2026-10-21", "13:12", "meh", "cloudy", None, False, []),
    ("2026-10-20", "23:00", "laugh", "sunny", None, False, ["night"]),
    ("2026-10-19", "20:34", "good", "sunny", 1, False, []),
    ("2026-10-17", "19:13", "cool", "partly_cloudy", None, False, []),
    ("2026-10-16", "23:30", "tired", "mostly_cloudy", None, False, []),
    ("2026-10-15", "12:40", "good", "sunny", 2, False, []),
    ("2026-10-11", "16:40", "laugh", "sunny", None, False, ["hills"]),
    ("2026-10-10", "21:09", "meh", "rain", None, False, []),
    ("2026-10-03", "11:30", "good", "partly_cloudy", 1, False, []),
    ("2026-10-07", "22:50", "sick", "cloudy", None, False, []),
]

PLACES = {
    "ko": ["바닷가 산책길", "동네 작은 카페", "한강공원"],
    "en": ["Seaside trail", "Corner café", "Riverside park"],
    "ja": ["海沿いの遊歩道", "近所の小さなカフェ", "川沿いの公園"],
}

# 언어마다 새로 쓴 글. 한 편은 한두 문장, 담백하게
TEXT = {
    "ko": [
        ["출근길 하늘이 유난히 파랬다. 오늘은 좋은 일이 있을 것 같다."],
        ["오랜만에 바다를 보러 갔다. 파도 소리를 들으며 한참 걸었다.", "돌아오는 길에 먹은 호떡까지 완벽했다."],
        ["퇴근하고 영화 한 편. 결말이 마음에 오래 남는다."],
        ["야근하고 들어오니 비가 쏟아졌다. 따뜻한 물로 씻고 바로 잘 거다."],
        ["카페 창가 자리에서 책을 읽었다. 라벤더 라테가 생각보다 맛있었다."],
        ["조카랑 공원에서 연을 날렸다. 하늘까지 닿을 것처럼 높이 올라갔다."],
        ["별일 없는 하루. 그래도 일기는 남겨 둔다."],
        ["친구 결혼식에 다녀왔다. 오랜만에 다들 만나서 웃다가 목이 쉬었다."],
        ["비 오는 날은 괜히 마음이 가라앉는다. 따뜻한 차를 마시며 일찍 누웠다."],
        ["저녁에 강변을 따라 달렸다. 바람이 벌써 차갑다."],
        ["엄마랑 통화했다. 별일 없냐는 말에 괜히 뭉클했다."],
        ["주말 아침 바닷가 산책. 사진을 몇 장 찍었는데 하늘색이 그대로 담겼다."],
        ["11월 첫날. 올해 남은 두 달은 조금 천천히 보내고 싶다."],
        ["핼러윈이라 동네가 북적였다. 나는 조용히 집에서 귤을 까먹었다."],
        ["새로 산 책을 반쯤 읽었다. 주인공이 답답하지만 왠지 나 같아서 계속 읽게 된다."],
        ["회의가 세 개나 있어서 정신이 없었다. 내일은 조금 일찍 나가서 커피 한 잔 마시고 시작하고 싶다."],
        ["한강에서 노을을 봤다. 하늘이 주황색에서 보라색으로 바뀌는 데 십 분도 안 걸렸다."],
        ["단골 카페에 새 원두가 들어왔다. 산미가 있어서 내 취향."],
        ["대청소를 했다. 서랍 하나 비웠을 뿐인데 마음까지 가벼워졌다."],
        ["괜히 예민해서 동생한테 말을 세게 했다. 내일 사과해야지."],
        ["점심에 먹은 김치찌개가 맛있었다. 오늘 기억나는 건 그게 전부."],
        ["밤하늘에 별이 많았다. 달이 밝아서 한참 올려다봤다."],
        ["카페에서 오래된 친구를 만났다. 서로 사는 얘기에 시간 가는 줄 몰랐다."],
        ["오랜만에 요리를 했다. 파스타가 조금 짰지만 뿌듯했다."],
        ["마감이 겹쳐서 하루가 길었다. 그래도 끝냈다."],
        ["한강공원에서 점심 도시락을 먹었다. 가을볕이 딱 좋았다."],
        ["보랏빛 언덕이 있는 수목원에 다녀왔다. 걷기만 해도 기분이 맑아졌다."],
        ["비가 와서 하루 종일 집에 있었다. 밀린 드라마를 봤다."],
        ["동네 카페 테라스에서 브런치. 주말 아침은 이런 맛이지."],
        ["감기 기운이 있어서 일찍 잤다. 내일은 나아지길."],
    ],
    "en": [
        ["The sky on my way to work was so blue. Feels like a good day."],
        ["Drove out to the sea for the first time in ages. Walked for an hour just listening to the waves.",
         "The hot pancake on the way back made it perfect."],
        ["Watched a movie after work. The ending is still on my mind."],
        ["Worked late and got caught in the rain. Hot shower, then straight to bed."],
        ["Read by the window at the café. The lavender latte was better than I expected."],
        ["Flew a kite with my niece in the park. It went so high it almost touched the clouds."],
        ["Nothing special today. Writing it down anyway."],
        ["Went to a friend's wedding. Saw everyone again and laughed until my voice was gone."],
        ["Rainy days make me a little low. Had some tea and went to bed early."],
        ["Ran along the river this evening. The wind is getting cold already."],
        ["Called Mom. She asked if everything was okay, and somehow that got to me."],
        ["Weekend walk by the sea. The photos caught the exact color of the sky."],
        ["First day of November. I want to take the last two months of the year a little slower."],
        ["Halloween crowds everywhere downtown. I stayed home and ate tangerines."],
        ["Halfway through the new book. The main character frustrates me, but I see myself in her."],
        ["Three meetings back to back. Tomorrow I'll leave early and start with a coffee."],
        ["Watched the sunset at the park. Orange to purple in under ten minutes."],
        ["My favorite café has new beans. A little bright and fruity, just how I like it."],
        ["Cleaned the whole apartment. Emptied one drawer and somehow felt lighter."],
        ["I snapped at my brother for no reason. I'll apologize tomorrow."],
        ["Lunch was a really good bowl of soup. That's the main thing I remember today."],
        ["So many stars tonight. The moon was bright enough that I stood outside for a while."],
        ["Ran into an old friend at the café. We talked so long we lost track of time."],
        ["Cooked for the first time in a while. The pasta was a bit salty, but I'm proud of it."],
        ["Two deadlines on the same day. Long day, but done."],
        ["Had a packed lunch at the park. Perfect autumn sun."],
        ["Visited the gardens with the purple hills. Just walking there cleared my head."],
        ["Rained all day, so I stayed in and caught up on my show."],
        ["Brunch on the café terrace. This is what weekend mornings are for."],
        ["Feeling a cold coming on, so I went to bed early. Hope I'm better tomorrow."],
    ],
    "ja": [
        ["通勤中の空がとても青かった。今日はいいことがありそう。"],
        ["久しぶりに海を見に行った。波の音を聞きながら、しばらく歩いた。", "帰りに食べたたい焼きまで完璧だった。"],
        ["仕事帰りに映画を一本。ラストがずっと心に残っている。"],
        ["残業して帰ったら大雨。温かいお風呂に入って、すぐ寝る。"],
        ["カフェの窓際で本を読んだ。ラベンダーラテが思ったよりおいしかった。"],
        ["姪っ子と公園で凧あげ。空に届きそうなくらい高く上がった。"],
        ["特に何もない一日。それでも日記は書いておく。"],
        ["友だちの結婚式に行ってきた。久しぶりにみんなと会って、笑いすぎて声がかれた。"],
        ["雨の日はなんとなく気分が沈む。温かいお茶を飲んで早めに寝た。"],
        ["夜、川沿いを走った。風がもう冷たい。"],
        ["母と電話した。「変わりない？」の一言に、なぜかじんときた。"],
        ["週末の朝、海沿いを散歩。写真に空の色がそのまま写った。"],
        ["11月の初日。今年の残り2か月は、少しゆっくり過ごしたい。"],
        ["ハロウィンで街がにぎやかだった。私は家で静かにみかんを食べた。"],
        ["新しく買った本を半分まで読んだ。主人公にもどかしさを感じつつ、自分に似ていて読み進めてしまう。"],
        ["会議が3つもあってバタバタだった。明日は少し早く出て、コーヒーを飲んでから始めたい。"],
        ["公園で夕焼けを見た。オレンジから紫に変わるまで10分もかからなかった。"],
        ["行きつけのカフェに新しい豆が入った。少し酸味があって好みの味。"],
        ["大掃除をした。引き出しを一つ空けただけなのに、気持ちまで軽くなった。"],
        ["なんだかイライラして、弟にきつく言ってしまった。明日謝ろう。"],
        ["お昼に食べた味噌ラーメンがおいしかった。今日覚えているのはそれくらい。"],
        ["夜空に星がたくさん。月が明るくて、しばらく見上げていた。"],
        ["カフェで昔からの友だちに会った。近況を話していたら、あっという間だった。"],
        ["久しぶりに料理をした。パスタが少ししょっぱかったけど、満足。"],
        ["締め切りが重なって長い一日だった。でも終わらせた。"],
        ["川沿いの公園でお弁当を食べた。秋の日差しがちょうどよかった。"],
        ["紫の丘がある植物園へ。歩くだけで気分がすっきりした。"],
        ["雨で一日中家にいた。たまっていたドラマを見た。"],
        ["近所のカフェのテラスでブランチ。週末の朝はこうでなくちゃ。"],
        ["風邪っぽいので早めに寝た。明日には治りますように。"],
    ],
}


def html(paragraphs):
    escape = lambda t: t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return "".join(f"<p>{escape(p)}</p>" for p in paragraphs)


def millis(date, time):
    return int(datetime.strptime(f"{date} {time}", "%Y-%m-%d %H:%M").timestamp() * 1000)


def main():
    udid, lang, art = sys.argv[1], sys.argv[2], Path(sys.argv[3])
    container = subprocess.run(
        ["xcrun", "simctl", "get_app_container", udid, "org.sjhstudio.diary", "data"],
        check=True, capture_output=True, text=True,
    ).stdout.strip()
    support = Path(container) / "Library" / "Application Support"
    media_dir, thumbs_dir = support / "media", support / "thumbs"
    for folder in (media_dir, thumbs_dir):
        shutil.rmtree(folder, ignore_errors=True)
        folder.mkdir(parents=True)

    photos = {}
    for jpg in sorted(art.glob("*.jpg")):
        data = jpg.read_bytes()
        sha = hashlib.sha256(data).hexdigest()
        (media_dir / f"{sha}.jpg").write_bytes(data)
        # 썸네일: 긴 변 480px(ThumbnailRepository와 같은 규칙)
        subprocess.run(["sips", "-Z", "480", str(media_dir / f"{sha}.jpg"), "--out", str(thumbs_dir / f"{sha}.jpg")],
                       check=True, capture_output=True)
        photos[jpg.stem] = (sha, len(data))

    db = sqlite3.connect(support / "diary.db")
    db.executescript("DELETE FROM media; DELETE FROM entry; DELETE FROM draft; DELETE FROM draft_media;")
    for (date, time, mood, weather, place, favorite, pics), paragraphs in zip(PLAN, TEXT[lang]):
        entry_id, stamp = str(uuid.uuid4()), millis(date, time)
        db.execute(
            "INSERT INTO entry (id, date, time, body_html, plain_text, mood, weather, place_name, is_favorite, created_at, updated_at)"
            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (entry_id, date, time, html(paragraphs), "\n".join(paragraphs), mood, weather,
             PLACES[lang][place] if place is not None else None, int(favorite), stamp, stamp),
        )
        for order, name in enumerate(pics):
            sha, size = photos[name]
            db.execute(
                "INSERT INTO media (id, entry_id, sort_order, status, file_name, sha256, width, height, byte_size, created_at)"
                " VALUES (?, ?, ?, 'normal', ?, ?, 1600, 1200, ?, ?)",
                (str(uuid.uuid4()), entry_id, order, f"{sha}.jpg", sha, size, stamp),
            )
    db.commit()
    count = db.execute("SELECT count(*) FROM entry").fetchone()[0]
    print(f"{lang}: 일기 {count}편, 그림 {len(photos)}장")


if __name__ == "__main__":
    main()
