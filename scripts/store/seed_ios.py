"""스토어 스크린샷용 견본 일기를 iOS 시뮬레이터 앱 DB에 넣는다.

쓰는 법(앱을 끈 뒤):
  python3 -I scripts/store/seed_ios.py <기기 UDID> <ko|en|ja|zh-Hant|es|pt|de|fr> <그림 폴더> [photo]
넷째 인자로 photo를 주면 사진 일기 페이지용으로 사진 붙은 일기를 늘린다.
mood를 주면 기분 일기 페이지용으로 1월부터 하루 한 편 가까이 채워 한 해 기분이 보이게 한다.
그림 폴더는 scripts/store/make_photos.swift가 만든 곳이다.
견본만 쓰는 새 시뮬레이터에서만 돌린다. 그 기기의 일기와 사진을 모두 지우고 다시 넣는다.
"""

import hashlib
import random
import shutil
import sqlite3
import subprocess
import sys
import uuid
from datetime import date as day, datetime, timedelta
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

# 사진 일기 페이지용으로 더 붙일 그림. 사진 격자 첫 화면에서 같은 그림이 이웃하지 않게 고름
PHOTO_EXTRA = {
    "2026-11-13": ["clouds"],
    "2026-11-11": ["night"],
    "2026-11-04": ["dunes"],
    "2026-11-08": ["hills", "lavender"],
    "2026-11-01": ["clouds"],
    "2026-10-31": ["night"],
}

# 기분 일기 페이지용 한 해 채우기. 좋음이 가장 많고 힘든 기분은 드물게
YEAR_MOODS = ["good"] * 10 + ["laugh"] * 5 + ["cool"] * 4 + ["meh"] * 4 + ["tired"] * 3 + ["sad"] * 2 + ["bad", "sick", "angry"]
YEAR_WEATHER = ["sunny", "sunny", "partly_cloudy", "mostly_cloudy", "cloudy", "rain"]


def year_plan():
    """1월 1일부터 견본 마지막 일기까지 견본이 없는 날의 열에 여덟쯤 채운 일기"""
    pick = random.Random(7)
    taken = {row[0] for row in PLAN}
    last = max(datetime.strptime(row[0], "%Y-%m-%d").date() for row in PLAN)
    current, plan = day(last.year, 1, 1), []
    while current <= last:
        if current.isoformat() not in taken and pick.random() < 0.8:
            plan.append((current.isoformat(), f"{pick.randint(19, 23):02d}:{pick.randint(0, 59):02d}",
                         pick.choice(YEAR_MOODS), pick.choice(YEAR_WEATHER)))
        current += timedelta(days=1)
    return plan


PLACES = {
    "ko": ["바닷가 산책길", "동네 작은 카페", "한강공원"],
    "en": ["Seaside trail", "Corner café", "Riverside park"],
    "ja": ["海沿いの遊歩道", "近所の小さなカフェ", "川沿いの公園"],
    "zh-Hant": ["海邊步道", "巷口小咖啡廳", "河濱公園"],
    "es": ["Sendero junto al mar", "Café de la esquina", "Parque junto al río"],
    "pt": ["Trilha à beira-mar", "Café da esquina", "Parque à beira do rio"],
    "de": ["Küstenweg", "Café an der Ecke", "Park am Fluss"],
    "fr": ["Sentier du bord de mer", "Café du coin", "Parc au bord de la rivière"],
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
    "zh-Hant": [
        ['上班路上的天空特別藍。今天應該會有好事。'],
        ['好久沒去看海了。聽著海浪聲走了好一陣子。', '回程吃的鯛魚燒也很完美。'],
        ['下班後看了一部電影。結局一直留在心裡。'],
        ['加班回家遇到大雨。洗個熱水澡就要睡了。'],
        ['在咖啡廳窗邊看書。薰衣草拿鐵比想像中好喝。'],
        ['和姪女在公園放風箏。飛得好高，好像快碰到雲了。'],
        ['平凡的一天。還是寫下來。'],
        ['去參加朋友的婚禮。好久沒見大家，笑到聲音都啞了。'],
        ['下雨天心情總是有點低落。喝了杯熱茶早早睡了。'],
        ['晚上沿著河邊跑步。風已經變冷了。'],
        ['和媽媽講電話。她問我最近還好嗎，不知為何有點感動。'],
        ['週末早上在海邊散步。照片拍出了天空原本的顏色。'],
        ['十一月的第一天。今年剩下的兩個月想過得慢一點。'],
        ['萬聖節街上好熱鬧。我在家安靜地吃橘子。'],
        ['新買的書讀了一半。主角讓人著急，卻很像我，忍不住一直讀下去。'],
        ['連開三個會，忙得團團轉。明天想早點出門，先喝杯咖啡再開始。'],
        ['在公園看夕陽。從橘色變成紫色不到十分鐘。'],
        ['常去的咖啡廳進了新豆子。帶點酸味，正合我意。'],
        ['大掃除。只清空一個抽屜，心情也跟著輕鬆了。'],
        ['莫名煩躁，對弟弟說話太衝了。明天要道歉。'],
        ['午餐的牛肉麵很好吃。今天記得的大概就這個。'],
        ['夜空好多星星。月亮很亮，抬頭看了好久。'],
        ['在咖啡廳遇到老朋友。聊近況聊到忘了時間。'],
        ['好久沒下廚了。義大利麵有點鹹，但很有成就感。'],
        ['截稿日撞在一起，好漫長的一天。但還是完成了。'],
        ['在河濱公園吃便當。秋天的陽光剛剛好。'],
        ['去了有紫色山丘的植物園。光是走走心情就清爽了。'],
        ['下雨整天待在家。把累積的劇補完了。'],
        ['在附近咖啡廳的露台吃早午餐。週末早上就該這樣。'],
        ['好像要感冒了，早早上床。希望明天會好一點。'],
    ],
    "es": [
        ['El cielo camino al trabajo estaba azulísimo. Presiento un buen día.'],
        ['Fui a ver el mar después de mucho tiempo. Caminé un buen rato escuchando las olas.', 'Los churros de regreso lo hicieron perfecto.'],
        ['Vi una película después del trabajo. El final se me quedó en la cabeza.'],
        ['Salí tarde del trabajo y me agarró la lluvia. Ducha caliente y a dormir.'],
        ['Leí junto a la ventana en el café. El latte de lavanda estaba mejor de lo que esperaba.'],
        ['Volé una cometa con mi sobrina en el parque. Subió tan alto que casi tocaba las nubes.'],
        ['Un día sin nada especial. Igual lo escribo.'],
        ['Fui a la boda de una amiga. Volví a ver a todos y me reí hasta quedarme sin voz.'],
        ['Los días de lluvia me bajan un poco el ánimo. Tomé un té y me acosté temprano.'],
        ['Salí a correr junto al río. El viento ya está frío.'],
        ['Hablé con mamá. Me preguntó si todo estaba bien y no sé por qué me conmovió.'],
        ['Paseo de fin de semana junto al mar. Las fotos captaron el color exacto del cielo.'],
        ['Primer día de noviembre. Quiero vivir los dos últimos meses del año con más calma.'],
        ['Por Halloween el centro estaba lleno. Yo me quedé en casa comiendo mandarinas.'],
        ['Voy por la mitad del libro nuevo. La protagonista me desespera, pero me veo en ella.'],
        ['Tres reuniones seguidas. Mañana saldré temprano y empezaré con un café.'],
        ['Vi el atardecer en el parque. De naranja a morado en menos de diez minutos.'],
        ['Mi café favorito tiene granos nuevos. Un poco ácidos, justo como me gustan.'],
        ['Limpié toda la casa. Vacié un solo cajón y todo se siente más ligero.'],
        ['Le hablé mal a mi hermano sin razón. Mañana le pido perdón.'],
        ['La sopa del almuerzo estaba buenísima. Es lo que más recuerdo de hoy.'],
        ['Cuántas estrellas esta noche. La luna brillaba tanto que me quedé mirándola un rato.'],
        ['Me encontré con una vieja amiga en el café. Hablamos tanto que perdimos la noción del tiempo.'],
        ['Cociné después de mucho tiempo. La pasta quedó un poco salada, pero valió la pena.'],
        ['Dos entregas el mismo día. Día largo, pero terminado.'],
        ['Almorcé en el parque. El sol de otoño estaba perfecto.'],
        ['Fui a los jardines de las colinas moradas. Solo caminar ahí me despejó.'],
        ['Llovió todo el día, así que me quedé en casa viendo mi serie.'],
        ['Brunch en la terraza del café. Para esto son las mañanas de fin de semana.'],
        ['Siento que me va a dar gripe, así que me acosté temprano. Ojalá mañana esté mejor.'],
    ],
    "pt": [
        ['O céu no caminho do trabalho estava azul demais. Acho que vai ser um bom dia.'],
        ['Fui ver o mar depois de muito tempo. Caminhei um tempão ouvindo as ondas.', 'O pastel na volta deixou tudo perfeito.'],
        ['Vi um filme depois do trabalho. O final não sai da minha cabeça.'],
        ['Saí tarde do trabalho e peguei chuva. Banho quente e cama.'],
        ['Li perto da janela no café. O latte de lavanda era melhor do que eu esperava.'],
        ['Soltei pipa com minha sobrinha no parque. Subiu tão alto que quase encostou nas nuvens.'],
        ['Nada de especial hoje. Mesmo assim, deixo registrado.'],
        ['Fui ao casamento de uma amiga. Revi todo mundo e ri até perder a voz.'],
        ['Dia de chuva me deixa meio pra baixo. Tomei um chá e dormi cedo.'],
        ['Corri na beira do rio à noite. O vento já está frio.'],
        ['Liguei para minha mãe. Ela perguntou se estava tudo bem e, sei lá, me emocionei.'],
        ['Caminhada de fim de semana na praia. As fotos pegaram a cor exata do céu.'],
        ['Primeiro dia de novembro. Quero viver os dois últimos meses do ano com mais calma.'],
        ['O centro estava lotado por causa do Halloween. Fiquei em casa comendo tangerina.'],
        ['Já li metade do livro novo. A protagonista me irrita, mas eu me vejo nela.'],
        ['Três reuniões seguidas. Amanhã vou sair mais cedo e começar com um café.'],
        ['Vi o pôr do sol no parque. Do laranja ao roxo em menos de dez minutos.'],
        ['Meu café preferido recebeu grãos novos. Um pouco ácidos, do jeito que eu gosto.'],
        ['Faxina geral em casa. Esvaziei uma gaveta e até a cabeça ficou mais leve.'],
        ['Respondi mal ao meu irmão sem motivo. Amanhã peço desculpas.'],
        ['A sopa do almoço estava ótima. É o que mais lembro de hoje.'],
        ['Quantas estrelas hoje. A lua estava tão clara que fiquei um tempo olhando.'],
        ['Encontrei uma amiga antiga no café. Conversamos tanto que perdemos a hora.'],
        ['Cozinhei depois de muito tempo. O macarrão ficou meio salgado, mas valeu a pena.'],
        ['Dois prazos no mesmo dia. Dia longo, mas entreguei tudo.'],
        ['Almocei no parque. O sol de outono estava perfeito.'],
        ['Fui ao jardim das colinas roxas. Só de caminhar lá a cabeça clareou.'],
        ['Choveu o dia todo, então fiquei em casa colocando a série em dia.'],
        ['Brunch no terraço do café. Manhã de fim de semana é isso.'],
        ['Senti um resfriado chegando e fui dormir cedo. Tomara que amanhã esteja melhor.'],
    ],
    "de": [
        ['Der Himmel auf dem Weg zur Arbeit war so blau. Das wird ein guter Tag.'],
        ['Endlich mal wieder am Meer. Ich bin lange gelaufen und habe den Wellen zugehört.', 'Die warmen Waffeln auf dem Rückweg haben alles perfekt gemacht.'],
        ['Nach der Arbeit einen Film geschaut. Das Ende geht mir nicht aus dem Kopf.'],
        ['Lange gearbeitet und in den Regen gekommen. Heiß duschen und ab ins Bett.'],
        ['Im Café am Fenster gelesen. Der Lavendel-Latte war besser als erwartet.'],
        ['Mit meiner Nichte im Park einen Drachen steigen lassen. Er flog fast bis in die Wolken.'],
        ['Nichts Besonderes heute. Ich schreibe es trotzdem auf.'],
        ['Auf der Hochzeit einer Freundin gewesen. Alle wiedergesehen und gelacht, bis die Stimme weg war.'],
        ['Regentage drücken etwas auf die Stimmung. Tee getrunken und früh ins Bett.'],
        ['Abends am Fluss joggen gewesen. Der Wind ist schon kalt.'],
        ['Mit Mama telefoniert. Sie fragte, ob alles okay ist, und das hat mich irgendwie berührt.'],
        ['Wochenendspaziergang am Meer. Die Fotos zeigen genau die Farbe des Himmels.'],
        ['Erster November. Die letzten zwei Monate des Jahres will ich ruhiger angehen.'],
        ['Wegen Halloween war die Stadt voll. Ich bin zu Hause geblieben und habe Mandarinen gegessen.'],
        ['Das neue Buch zur Hälfte gelesen. Die Hauptfigur nervt mich, aber ich erkenne mich in ihr.'],
        ['Drei Meetings hintereinander. Morgen gehe ich früher los und starte mit einem Kaffee.'],
        ['Den Sonnenuntergang im Park gesehen. In weniger als zehn Minuten von Orange zu Lila.'],
        ['Mein Lieblingscafé hat neue Bohnen. Leicht fruchtig, genau mein Geschmack.'],
        ['Die ganze Wohnung geputzt. Nur eine Schublade ausgemistet und schon fühlt sich alles leichter an.'],
        ['Ohne Grund meinen Bruder angefahren. Morgen entschuldige ich mich.'],
        ['Mittags gab es eine richtig gute Suppe. Mehr ist von heute nicht hängen geblieben.'],
        ['So viele Sterne heute Nacht. Der Mond war so hell, dass ich lange draußen stand.'],
        ['Im Café eine alte Freundin getroffen. Wir haben so viel geredet, dass wir die Zeit vergessen haben.'],
        ['Endlich mal wieder gekocht. Die Pasta war etwas salzig, aber es hat sich gelohnt.'],
        ['Zwei Abgaben am selben Tag. Langer Tag, aber geschafft.'],
        ['Mittagessen im Park. Die Herbstsonne war perfekt.'],
        ['Im Garten mit den lila Hügeln gewesen. Schon das Spazieren hat den Kopf frei gemacht.'],
        ['Den ganzen Tag Regen, also zu Hause geblieben und meine Serie nachgeholt.'],
        ['Brunch auf der Café-Terrasse. Genau dafür gibt es Wochenendmorgen.'],
        ['Eine Erkältung bahnt sich an, also früh ins Bett. Hoffentlich geht es morgen besser.'],
    ],
    "fr": [
        ['Le ciel en allant au travail était si bleu. Ça sent la bonne journée.'],
        ["Enfin revu la mer. J'ai marché longtemps en écoutant les vagues.", "Les crêpes au retour, c'était parfait."],
        ['Un film après le travail. La fin me trotte encore dans la tête.'],
        ['Journée à rallonge et une averse en rentrant. Douche chaude, puis au lit.'],
        ['Lecture près de la fenêtre au café. Le latte à la lavande était meilleur que prévu.'],
        ["Cerf-volant au parc avec ma nièce. Il est monté presque jusqu'aux nuages."],
        ["Rien de spécial aujourd'hui. Je l'écris quand même."],
        ["Mariage d'une amie. On s'est tous retrouvés et j'ai ri à en perdre la voix."],
        ['Les jours de pluie me rendent un peu morose. Un thé, puis au lit tôt.'],
        ['Course le long de la rivière ce soir. Le vent est déjà froid.'],
        ["Appel avec maman. Elle m'a demandé si tout allait bien, et ça m'a fait quelque chose."],
        ['Balade du week-end au bord de la mer. Les photos ont gardé la couleur exacte du ciel.'],
        ["Premier jour de novembre. J'ai envie de vivre les deux derniers mois de l'année plus doucement."],
        ['Le centre-ville était bondé pour Halloween. Moi, soirée mandarines à la maison.'],
        ["J'en suis à la moitié du nouveau livre. L'héroïne m'agace, mais je me reconnais en elle."],
        ["Trois réunions d'affilée. Demain, je pars plus tôt et je commence par un café."],
        ["Coucher de soleil au parc. De l'orange au violet en moins de dix minutes."],
        ['Mon café préféré a de nouveaux grains. Un peu fruités, comme je les aime.'],
        ['Grand ménage. Un seul tiroir vidé et tout paraît plus léger.'],
        ["J'ai parlé sèchement à mon frère sans raison. Je m'excuserai demain."],
        ["La soupe du midi était délicieuse. C'est surtout ça que je retiens d'aujourd'hui."],
        ["Tant d'étoiles ce soir. La lune brillait tant que je l'ai regardée un long moment."],
        ["Croisé une vieille amie au café. On a tellement parlé qu'on a oublié l'heure."],
        ['Enfin recuisiné. Les pâtes étaient un peu salées, mais ça valait le coup.'],
        ["Deux rendus le même jour. Longue journée, mais c'est fait."],
        ["Déjeuner au parc. Le soleil d'automne était parfait."],
        ["Visite du jardin aux collines violettes. Rien que marcher m'a vidé la tête."],
        ['Pluie toute la journée, alors journée à la maison à rattraper ma série.'],
        ["Brunch sur la terrasse du café. Les matins de week-end, c'est fait pour ça."],
        ["Un rhume qui arrive, alors au lit tôt. J'espère aller mieux demain."],
    ],
}


def html(paragraphs):
    escape = lambda t: t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    return "".join(f"<p>{escape(p)}</p>" for p in paragraphs)


def millis(date, time):
    return int(datetime.strptime(f"{date} {time}", "%Y-%m-%d %H:%M").timestamp() * 1000)


def main():
    udid, lang, art = sys.argv[1], sys.argv[2], Path(sys.argv[3])
    mode = sys.argv[4] if len(sys.argv) > 4 else ""
    extra = PHOTO_EXTRA if mode == "photo" else {}
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
        for order, name in enumerate(pics + extra.get(date, [])):
            sha, size = photos[name]
            db.execute(
                "INSERT INTO media (id, entry_id, sort_order, status, file_name, sha256, width, height, byte_size, created_at)"
                " VALUES (?, ?, ?, 'normal', ?, ?, 1600, 1200, ?, ?)",
                (str(uuid.uuid4()), entry_id, order, f"{sha}.jpg", sha, size, stamp),
            )
    if mode == "mood":
        for index, (date, time, mood, weather) in enumerate(year_plan()):
            paragraphs, stamp = TEXT[lang][index % len(TEXT[lang])], millis(date, time)
            db.execute(
                "INSERT INTO entry (id, date, time, body_html, plain_text, mood, weather, place_name, is_favorite, created_at, updated_at)"
                " VALUES (?, ?, ?, ?, ?, ?, ?, NULL, 0, ?, ?)",
                (str(uuid.uuid4()), date, time, html(paragraphs), "\n".join(paragraphs), mood, weather, stamp, stamp),
            )
    db.commit()
    count = db.execute("SELECT count(*) FROM entry").fetchone()[0]
    print(f"{lang}: 일기 {count}편, 그림 {len(photos)}장")


if __name__ == "__main__":
    main()
