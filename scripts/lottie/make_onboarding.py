#!/usr/bin/env python3
"""온보딩 설명 그림 로띠를 만든다.

  python3 scripts/lottie/make_onboarding.py

색은 "@accent" 같은 자리표시로 적고, 앱이 그릴 때 디자인 토큰 값으로 바꾼다(OnboardingArt.kt).
그래서 다크 모드를 따로 만들지 않는다. 글자를 넣지 않아 언어도 따로 만들지 않는다.
"""
import json
import math
import os
import re

OUT = "shared/feature/welcome/src/commonMain/composeResources/files"
W, H, FPS = 288, 208, 30
PEN = "M 14.5 5.5 L 18.5 9.5 L 9 19 L 5 19 L 5 15 L 14.5 5.5 Z"
CLOUD = ("M 7 15 L 17 15 C 18.06 15 19.08 14.58 19.83 13.83 C 20.58 13.08 21 12.06 21 11 C 21 9.94 20.58 8.92 19.83 8.17 "
         "C 19.08 7.42 18.06 7 17 7 C 16.54 5.84 15.71 4.87 14.63 4.25 C 13.55 3.63 12.29 3.39 11.06 3.58 "
         "C 9.83 3.77 8.7 4.36 7.85 5.28 C 7.01 6.19 6.49 7.36 6.4 8.6 C 5.69 8.84 5.09 9.3 4.68 9.93 "
         "C 4.27 10.55 4.09 11.3 4.16 12.04 C 4.23 12.78 4.55 13.48 5.06 14.01 C 5.58 14.55 6.26 14.9 7 15 Z")
EASE_OUT = ({"x": [0.2], "y": [1]}, {"x": [0.4], "y": [0]})
LINEAR = ({"x": [0.5], "y": [0.5]}, {"x": [0.5], "y": [0.5]})


def static(v):
    return {"a": 0, "k": v}


def keys(frames, ease=EASE_OUT):
    """[(프레임, 값)]을 키프레임으로 바꾼다. 값은 숫자 목록이다."""
    out = []
    for i, (t, v) in enumerate(frames):
        v = v if isinstance(v, list) else [v]
        k = {"t": t, "s": v}
        if i < len(frames) - 1:
            n = len(v)
            k["i"] = {"x": ease[0]["x"] * n, "y": ease[0]["y"] * n}
            k["o"] = {"x": ease[1]["x"] * n, "y": ease[1]["y"] * n}
        out.append(k)
    return {"a": 1, "k": out}


def fill(color):
    return {"ty": "fl", "c": static(color), "o": static(100), "r": 1}


def stroke(color, width, opacity=100):
    return {"ty": "st", "c": static(color), "o": static(opacity), "w": static(width), "lc": 2, "lj": 2, "ml": 4}


def rect(cx, cy, w, h, r):
    return {"ty": "rc", "p": static([cx, cy]), "s": static([w, h]), "r": static(r), "d": 1}


def ellipse(cx, cy, r):
    return {"ty": "el", "p": static([cx, cy]), "s": static([r * 2, r * 2]), "d": 1}


def path(d, scale=1.0, dx=0.0, dy=0.0):
    """M, L, C, Z만 쓰는 절대 좌표 경로를 로띠 도형으로. M이 여럿이면 하위 경로마다 도형 하나(이어 그리지 않게)"""
    parts = [p for p in re.split(r"(?=M)", d.strip()) if p.strip()]
    if len(parts) > 1:
        return [path(p, scale, dx, dy) for p in parts]
    tokens = re.findall(r"[MLCZ]|-?[\d.]+", d)
    v, i, o, closed, pos = [], [], [], False, 0

    def num(n):
        nonlocal pos
        out = [float(x) for x in tokens[pos:pos + n]]
        pos += n
        return out

    while pos < len(tokens):
        c = tokens[pos]
        pos += 1
        if c in "ML":
            x, y = num(2)
            v.append([x, y]); i.append([0, 0]); o.append([0, 0])
        elif c == "C":
            x1, y1, x2, y2, x, y = num(6)
            px, py = v[-1]
            o[-1] = [x1 - px, y1 - py]
            v.append([x, y]); i.append([x2 - x, y2 - y]); o.append([0, 0])
        elif c == "Z":
            closed = True
    if closed and v[0] == v[-1]:
        i[0] = i[-1]
        v, i, o = v[:-1], i[:-1], o[:-1]
    m = lambda p: [round(p[0] * scale + dx, 3), round(p[1] * scale + dy, 3)]
    s = lambda p: [round(p[0] * scale, 3), round(p[1] * scale, 3)]
    return {"ty": "sh", "ks": static({"v": [m(p) for p in v], "i": [s(p) for p in i], "o": [s(p) for p in o], "c": closed})}


def group(items):
    tr = {"ty": "tr", "p": static([0, 0]), "a": static([0, 0]), "s": static([100, 100]), "r": static(0), "o": static(100)}
    flat = []
    for it in items:
        flat.extend(it if isinstance(it, list) else [it])
    return {"ty": "gr", "it": flat + [tr]}


class Comp:
    def __init__(self, name, frames, out=OUT, w=W, h=H):
        self.name, self.frames, self.layers, self.out, self.w, self.h = name, frames, [], out, w, h

    def layer(self, name, shapes, anchor=(0, 0), pos=None, scale=None, opacity=None, rotation=None):
        """나중에 더한 층이 아래로 간다."""
        self.layers.append({
            "ddd": 0, "ind": len(self.layers) + 1, "ty": 4, "nm": name, "sr": 1, "ao": 0, "bm": 0,
            "ip": 0, "op": self.frames, "st": 0,
            "ks": {"o": opacity or static(100), "r": rotation or static(0), "p": pos or static([anchor[0], anchor[1], 0]),
                   "a": static([anchor[0], anchor[1], 0]), "s": scale or static([100, 100, 100])},
            "shapes": shapes,
        })

    def save(self):
        data = {"v": "5.7.4", "fr": FPS, "ip": 0, "op": self.frames, "w": self.w, "h": self.h, "nm": self.name, "ddd": 0, "assets": [],
                "layers": self.layers}
        with open(os.path.join(self.out, f"{self.name}.json"), "w") as f:
            json.dump(data, f, separators=(",", ":"))


def pop(start, end):
    """톡 나타나는 크기. 살짝 넘쳤다 돌아온다."""
    mid = start + (end - start) * 0.65
    return keys([(start, [0, 0, 100]), (mid, [112, 112, 100]), (end, [100, 100, 100])])


def face(cx, cy, r):
    k = r / 18
    eye = lambda dx: group([ellipse(cx + dx * k, cy - 4 * k, 2 * k), fill("@faceLine")])
    smile = group([path("M 0 0 C 2 5 10 5 12 0", k, cx - 6 * k, cy + 3 * k), stroke("@faceLine", 2 * k)])
    return [eye(-5.5), eye(5.5), smile, group([ellipse(cx, cy, r), fill("@face")])]


def check(cx, cy, r, comp, start, end):
    comp.layer("확인", [
        group([path("M 20 6 L 9 17 L 4 12", r / 16 * 0.67, cx - r / 16 * 8, cy - r / 16 * 8), stroke("@onAccent", 2.2)]),
        group([ellipse(cx, cy, r), fill("@accent")]),
    ], anchor=(cx, cy), scale=pop(start, end))


def image_icon(cx, cy, size, color):
    k = size / 24
    dx, dy = cx - size / 2, cy - size / 2
    w = 1.75 * k * 1.3
    return [
        group([path("M 3 19 L 15 10 L 21 15", k, dx, dy), stroke(color, w)]),
        group([ellipse(cx - 3 * k, cy - 3 * k, 2 * k), stroke(color, w)]),
        group([rect(cx, cy, 18 * k, 18 * k, 2 * k), stroke(color, w)]),
    ]


def pen_layer(c, points, tilt, start, finish, k=1.5):
    """펜 끝(왼쪽 아래)이 글 끝에 닿게 둔다. 레퍼런스처럼 큼직하게 그리고, 쓰는 동안 손목처럼 기울인다."""
    c.layer("펜", [group([path(PEN, k, -5 * k, -19 * k), fill("@accentText")])],
            pos=keys(points, LINEAR), rotation=keys(tilt, LINEAR),
            opacity=keys([(start - 4, 0), (start, 100), (finish, 100), (finish + 4, 0)]))


def card(c):
    c.layer("얼굴", [*face(54, 50, 18)], anchor=(54, 50), scale=pop(0, 14))
    c.layer("카드", [group([rect(144, 104, 270, 176, 16), stroke("@line", 1), fill("@surface")])])


def write_lines():
    """공책 줄처럼 곧은 줄 셋을 넓게 둔다. 줄 끝은 길이를 달리해 글처럼 보이게 한다."""
    speed = 240 / FPS  # 1초에 240. 110은 늘어져 보임
    lift = 8  # 다음 줄로 펜을 옮기는 프레임
    lines = [(36, 252, 92), (36, 216, 116), (36, 156, 140)]
    frame, pen, tilt, spans = 18.0, [], [], []
    for x1, x2, y in lines:
        end = frame + (x2 - x1) / speed
        spans.append((x1, x2, y, frame, end))
        pen += [(frame, [x1, y, 0]), (end, [x2, y, 0])]
        for i in range(13):
            u = i / 12
            tilt.append((frame + (end - frame) * u, -4 + 6 * math.sin(4 * math.pi * u)))
        frame = end + lift
    finish = frame - lift
    c = Comp("onboarding_write", round(finish + 22))
    check(240, 156, 14, c, finish + 6, finish + 20)
    pen_layer(c, pen, tilt, 18, finish)
    for n, (x1, x2, y, a, b) in enumerate(spans):
        c.layer(f"줄 {n + 1}", [group([
            path(f"M {x1} {y} L {x2} {y}"),
            {"ty": "tm", "s": static(0), "e": keys([(a, 0), (b, 100)], LINEAR), "o": static(0), "m": 1},
            stroke("@textTertiary", 5, 50),
        ])])
    card(c)
    c.save()


def backup():
    c = Comp("onboarding_backup", 96)
    # 구름 원은 반지름 48에 7%까지 커져, 칸 끝에 붙여 두면 커질 때 잘림. 휴대폰과 원을 4씩 안으로
    start, end, peak = (50, 104), (236, 104), 36
    check(264, 136, 16, c, 74, 90)
    icons = [lambda: group([path(PEN, 0.75, -9, -9), fill("@onAccent")]),
             lambda: group(image_icon(0, 0, 18, "@onAccent")),
             lambda: group(image_icon(0, 0, 18, "@onAccent"))]
    for n, (a, b) in enumerate([(0, 32), (20, 52), (40, 72)]):
        pts = []
        for i in range(9):
            t = i / 8
            pts.append((a + (b - a) * t, [start[0] + (end[0] - start[0]) * t, start[1] - peak * math.sin(math.pi * t), 0]))
        c.layer(f"조각 {n + 1}", [icons[n](), group([rect(0, 0, 32, 32, 8), fill("@accent")])],
                pos=keys(pts, LINEAR),
                scale=keys([(a, [100, 100, 100]), (b, [60, 60, 100])], LINEAR),
                opacity=keys([(a, 0), (a + 5, 100), (b - 5, 100), (b, 0)], LINEAR))
    bump = keys([(0, [100, 100, 100]), (32, [100, 100, 100]), (38, [107, 107, 100]), (46, [100, 100, 100]),
                 (52, [100, 100, 100]), (58, [107, 107, 100]), (66, [100, 100, 100]), (72, [100, 100, 100]),
                 (78, [107, 107, 100]), (86, [100, 100, 100])])
    c.layer("구름", [group([path(CLOUD, 44 / 24, end[0] - 22, end[1] - 21), stroke("@accentText", 3.2)]),
                   group([ellipse(*end, 48), fill("@selected")])], anchor=end, scale=bump)
    c.layer("휴대폰", [
        *face(28, 46, 10),
        group([path("M 20 70 L 80 70"), stroke("@sunken", 6)]),
        group([path("M 20 84 L 80 84"), stroke("@sunken", 6)]),
        *image_icon(50, 130, 16, "@textTertiary"),
        group([rect(50, 130, 68, 60, 8), fill("@sunken")]),
        group([rect(50, 104, 91, 155, 16), stroke("@line", 1), fill("@surface")]),
    ])
    c.save()


# 결제 화면 그림(스펙 010). 핵심 혜택 넷을 하나씩: 아래 혜택 아이콘이 켜지고 → 가운데 카드에 그 혜택 그림 → 넷이 다 켜지면 Plus 배지
# 아래 아이콘은 결제 화면 혜택 목록(DiaryIcons)과 같은 모양, 가운데는 아이콘 대신 혜택이 실제로 어떻게 보이는지
# 그림 칸에 맞춰 통째로 줄고 늘어 가운데에 놓임(PlusScreen의 Fit). 한 번 재생 뒤 끝 모습(배지 + 켜진 아이콘 넷)이 완성 그림
PLUS_W, PLUS_H, PLUS_FRAMES = 330, 160, 210
PLUS_CX = PLUS_W / 2
# 견본 셋(정한 색 로즈·바다·숲). 테마 색을 고르는 시연이라 사용자 테마와 상관없이 고정
LAVENDER = [0.431, 0.337, 0.812, 1]
ROSE = [0.667, 0.290, 0.376, 1]
SEA = [0.251, 0.431, 0.588, 1]
FOREST = [0.251, 0.459, 0.322, 1]
WHITE = [1, 1, 1, 1]
BLACK = [0, 0, 0, 1]
# 기분 색 아픔(통계 막대 셋째). 자리표시가 없는 기분 색이라 값으로
MOOD_SICK = [0.745, 0.914, 0.706, 1]
# 앱 Plus 아이콘(DiaryIcons.SparklesSolid, 24 격자)과 같은 큰 별 + 작은 별. 가운데 0,0
SPARKLES = ("M -1.5 -8 C -0.567 -2.933 2.433 0.067 7.5 1 C 2.433 1.933 -0.567 4.933 -1.5 10 "
            "C -2.433 4.933 -5.433 1.933 -10.5 1 C -5.433 0.067 -2.433 -2.933 -1.5 -8 Z "
            "M 7 -10.2 C 7.333 -8.4 8.4 -7.333 10.2 -7 C 8.4 -6.667 7.333 -5.6 7 -3.8 "
            "C 6.667 -5.6 5.6 -6.667 3.8 -7 C 5.6 -7.333 6.667 -8.4 7 -10.2 Z")
# 선으로 그린 글자 AD(가운데 0,0). A와 D 사이를 넓혀 붙어 보이지 않게
AD_MARK = "M -5.6 3.5 L -3.6 -3.5 L -1.6 3.5 M -4.9 1.4 L -2.3 1.4 M 1.8 -3.5 L 1.8 3.5 L 3.2 3.5 C 5 3.5 6 2.3 6 0 C 6 -2.3 5 -3.5 3.2 -3.5 Z"
MOUNTAINS = "M -17 12 L -7 1 L -1 7 L 5 1 L 17 12 Z"
# 선으로 그린 숫자(높이 14, 폭 9). 글자가 아니라 언어와 상관없음
DIGIT_PATHS = {
    "3": "M 1 2 C 2 0.7 3.4 0 5 0 C 7.3 0 8.7 1.5 8.7 3.5 C 8.7 5.6 7 6.9 4.6 6.9 C 7.4 6.9 9 8.4 9 10.5 C 9 12.6 7.3 14 4.8 14 "
         "C 3.2 14 1.6 13.4 0.6 12.2",
    "0": "M 4.5 0 C 7.2 0 9 2.7 9 7 C 9 11.3 7.2 14 4.5 14 C 1.8 14 0 11.3 0 7 C 0 2.7 1.8 0 4.5 0 Z",
}
# 결제 화면 혜택 목록 아이콘(DiaryIcons ShieldCheck·Image·TabInsight·Palette)과 같은 모양. 호(arc)는 같은 모양의 곡선으로 옮김
BENEFIT_ICONS = {
    "ad": ["M 20 13 C 20 18 16.5 20.5 12.34 21.95 L 11.67 21.94 C 7.5 20.5 4 18 4 13 L 4 6 C 4 5.45 4.45 5 5 5 "
           "C 7 5 9.5 3.8 11.24 2.28 C 11.68 1.9 12.32 1.9 12.76 2.28 C 14.51 3.81 17 5 19 5 C 19.55 5 20 5.45 20 6 L 20 13 Z",
           "M 9 12 L 11 14 L 15 10"],
    "photo": ["M 6.5 5 L 17.5 5 C 18.88 5 20 6.12 20 7.5 L 20 16.5 C 20 17.88 18.88 19 17.5 19 L 6.5 19 C 5.12 19 4 17.88 4 16.5 "
              "L 4 7.5 C 4 6.12 5.12 5 6.5 5 Z",
              "M 10.6 10 C 10.6 10.88 9.88 11.6 9 11.6 C 8.12 11.6 7.4 10.88 7.4 10 C 7.4 9.12 8.12 8.4 9 8.4 C 9.88 8.4 10.6 9.12 10.6 10 Z",
              "M 5 17 L 9.5 12.5 L 12.5 15.5 L 15 13 L 19 16.6"],
    "stats": ["M 5 20 L 5 11 M 12 20 L 12 5 M 19 20 L 19 14"],
    "theme": ["M 12 22 C 6.48 22 2 17.52 2 12 C 2 6.48 6.48 2 12 2 C 17.52 2 22 6.03 22 11 C 22 13.76 19.76 16 17 16 L 14.75 16 "
              "C 13.6 16 12.7 17.9 13.35 18.8 L 13.65 19.2 C 14.3 20.1 13.6 22 12.25 22 Z"],
}
PALETTE_DOTS = ((13.5, 6.5), (17.5, 10.5), (6.5, 12.5), (8.5, 7.5))


def color_keys(frames):
    """[(프레임, 색)]. 색은 "@accent" 같은 자리표시나 [r, g, b, 1]
    색 사이를 섞으면 중간에 회색이 비쳐 다음 키까지 그 색을 그대로 둠"""
    return {"a": 1, "k": [{"t": t, "s": v, "h": 1} for t, v in frames]}


def grp(*items, t=None):
    """여러 도형 무리. t를 주면 그 변형(움직임)을 씀"""
    flat = []
    for it in items:
        flat.extend(it if isinstance(it, list) else [it])
    if t is None:
        return group(flat)
    return {"ty": "gr", "it": flat + [t]}


def transform(p=(0, 0), s=None, o=None, a=None):
    return {"ty": "tr", "p": static(list(p)), "a": static(list(a if a is not None else p)), "s": s or static([100, 100]),
            "r": static(0), "o": o or static(100)}


def fill_o(color, opacity):
    return {"ty": "fl", "c": color if isinstance(color, dict) else static(color), "o": static(opacity), "r": 1}


def pop_in(start, dur=10):
    """무리 안에서 톡 나타나는 크기(2차원)"""
    return keys([(start, [0, 0]), (start + dur * 0.6, [110, 110]), (start + dur, [100, 100])])


def digits(text, cx, cy, height, color, width):
    k = height / 14
    gap = 3.6 * k
    x = cx - (len(text) * 9 * k + (len(text) - 1) * gap) / 2
    shapes = []
    for ch in text:
        p = path(DIGIT_PATHS[ch], k, x, cy - height / 2)
        shapes.extend(p if isinstance(p, list) else [p])
        x += 9 * k + gap
    return grp(*shapes, stroke(color, width))


def benefit_icon(kind, cx, cy, size, color):
    k = size / 24
    dx, dy = cx - 12 * k, cy - 12 * k
    shapes = []
    for d in BENEFIT_ICONS[kind]:
        p = path(d, k, dx, dy)
        shapes.extend(p if isinstance(p, list) else [p])
    out = [grp(*shapes, stroke(color, 1.9 * k))]
    if kind == "theme":
        out += [grp(ellipse(dx + x * k, dy + y * k, 1.25 * k), fill(color)) for x, y in PALETTE_DOTS]
    return out


# 혜택 그림(1배에서 44×28 칸 안, 가운데 cx, cy). t는 그림 안 움직임 시작
def art_ad(cx, cy, s, t):
    """일기 화면(글 두 줄) 아래 'AD ✕' 띠. ✕가 눌리면 띠가 내려가며 사라짐"""
    line = lambda d: grp(path(d, s, cx, cy), stroke("@line", 2.2 * s))
    by = cy + 9 * s
    xb = (cx + 12.2 * s, by)
    press = keys([(t, [100, 100]), (t + 3, [82, 82]), (t + 6, [100, 100])])
    close = grp(grp(path("M -1.8 -1.8 L 1.8 1.8 M 1.8 -1.8 L -1.8 1.8", s, *xb), stroke("@textTertiary", 1.3 * s)),
                t=transform(xb, press))
    banner = {"ty": "gr", "it": [
        close,
        grp(path(AD_MARK, s * 0.6, cx - 10 * s, by), stroke("@textTertiary", 1.3 * s)),
        grp(rect(cx, by, 40 * s, 13 * s, 6.5 * s), fill("@sunken")),
        {"ty": "tr", "p": keys([(t + 8, [cx, by]), (t + 18, [cx, by + 6 * s])]), "a": static([cx, by]), "s": static([100, 100]),
         "r": static(0), "o": keys([(t + 8, 100), (t + 18, 0)], LINEAR)}]}
    return [banner, line("M -20 -10 L 16 -10"), line("M -20 -3 L 4 -3")]


def art_photo(cx, cy, s, t):
    """사진 두 장(앞은 해와 산)과 '30' 알약"""
    def sheet(x, y, color, start, extra=()):
        items = [*extra, grp(rect(x, y, 30 * s, 22 * s, 5 * s), stroke("@surface", 1.4 * s), fill(color))]
        return grp(*items, t=transform((x, y), pop_in(start)))
    fx, fy = cx - 6 * s, cy + 2 * s
    front = sheet(fx, fy, "@moodA", t + 3, extra=[grp(path(MOUNTAINS, s * 0.75, fx, fy - 0.5 * s), fill_o(WHITE, 92)),
                                                   grp(ellipse(fx - 7.5 * s, fy - 3.5 * s, 2.4 * s), fill_o(WHITE, 92))])
    px, py = cx + 14 * s, cy + 9 * s
    pill = grp(digits("30", px, py, 5 * s, WHITE, 1.05 * s), grp(rect(px, py, 20 * s, 12 * s, 6 * s), fill_o(BLACK, 70)),
               grp(rect(px, py, 20 * s, 12 * s, 6 * s), stroke("@surface", 1.6 * s)), t=transform((px, py), pop_in(t + 8, 12)))
    return [pill, front, sheet(cx + 1 * s, cy - 5 * s, "@moodB", t)]


def art_stats(cx, cy, s, t):
    """돌아보기 기분 그래프처럼 기분 점 + 같은 색 가로 막대, 많은 차례"""
    out = []
    for i, (dy, color, length) in enumerate(((-9, "@face", 32), (0, "@moodC", 22), (9, MOOD_SICK, 13))):
        y = cy + dy * s
        grow = keys([(t + i * 3, [0, 100]), (t + i * 3 + 8, [100, 100])])
        out.append(grp(rect(length * s / 2, 0, length * s, 5 * s, 2.5 * s), fill(color), t=transform((cx - 14 * s, y), grow, a=(0, 0))))
        out.append(grp(ellipse(cx - 20 * s, y, 3 * s), fill(color)))
    return out


def art_theme(cx, cy, s, t):
    """카드가 곧 일기 화면: 제목 줄과 글 줄 셋, 오른쪽 세로 견본. 고름 고리가 옮겨 갈 때마다 제목 줄이 그 색으로"""
    sx = cx + 17 * s
    # 견본 3.4, 고리 5.2. 사이 9.5라 고리가 옆 견본에 닿지 않음
    ys = [cy - 9.5 * s, cy, cy + 9.5 * s]
    ring_pos = keys([(0, [sx, ys[0], 0]), (t + 4, [sx, ys[0], 0]), (t + 9, [sx, ys[1], 0]), (t + 13, [sx, ys[1], 0]), (t + 18, [sx, ys[2], 0])])
    accent = color_keys([(0, LAVENDER), (t + 4, ROSE), (t + 13, SEA), (t + 22, FOREST)])
    ring = {"ty": "gr", "it": [ellipse(0, 0, 5.2 * s), stroke("@textPrimary", 1.1 * s),
                                {"ty": "tr", "p": ring_pos, "a": static([0, 0]), "s": static([100, 100]), "r": static(0), "o": static(100)}]}
    line = lambda d: grp(path(d, s, cx, cy), stroke("@line", 2.2 * s))
    return [ring, *[grp(ellipse(sx, y, 3.4 * s), fill(color)) for y, color in zip(ys, (ROSE, SEA, FOREST))],
            line("M -20 -2 L 6 -2"), line("M -20 4.5 L 2 4.5"), line("M -20 11 L -6 11"),
            grp(rect(cx - 10 * s, cy - 10 * s, 20 * s, 4.4 * s, 2.2 * s), {"ty": "fl", "c": accent, "o": static(100), "r": 1})]


def plus():
    c = Comp("plus_benefits", PLUS_FRAMES, "shared/feature/plus/src/commonMain/composeResources/files", PLUS_W, PLUS_H)
    kinds = ("ad", "photo", "stats", "theme")
    arts = (art_ad, art_photo, art_stats, art_theme)
    # 혜택 하나에 46프레임(1.5초). 아이콘 원 40, 사이 8
    # 카드와 아이콘 줄 사이 10, 위아래 여백 18로 가운데
    xs, icon_y = (93, 141, 189, 237), 122
    starts, hold = (10, 56, 102, 148), 40
    stage_y, k = 55, 1.7
    # 그림마다 내용 테두리를 재서 44×28 안에 가운데로(가운데 카드 60×44, 안쪽 여백 8). (가로 옮김, 세로 옮김, 배율)
    fit = {"ad": (0, -2.25, 1.0), "photo": (-1.35, 0.45, 0.9), "stats": (2.5, 0, 1.0), "theme": (-0.5, -0.6, 1.0)}
    for i, (kind, art, x, t) in enumerate(zip(kinds, arts, xs, starts)):
        # 아이콘이 먼저 켜지고(t), 가운데 카드에 그 혜택 그림이 나옴(t+4). 그림 안 움직임은 t+8부터
        # 카드는 그대로 두고 안의 그림만 바뀜. 장면마다 카드가 꺼졌다 켜지면 깜빡여 보임
        shown, gone = t + 4, t + hold
        fx, fy, fk = fit[kind]
        c.layer(f"그림 {i}", art(PLUS_CX + fx * k, stage_y + fy * k, k * fk, t + 8),
                anchor=(PLUS_CX, stage_y),
                scale=keys([(shown, [92, 92, 100]), (shown + 8, [100, 100, 100]), (gone, [100, 100, 100]), (gone + 6, [96, 96, 100])]),
                opacity=keys([(shown, 0), (shown + 6, 100), (gone, 100), (gone + 6, 0)], LINEAR))
        # 혜택 아이콘: 회색으로 있다가 켜짐(혜택 목록의 아이콘 칸처럼 selected 원 + accent 아이콘)
        lit = grp(*benefit_icon(kind, x, icon_y, 20, "@accentText"), grp(ellipse(x, icon_y, 20), fill("@selected")),
                  t=transform((x, icon_y), keys([(t, [70, 70]), (t + 6, [112, 112]), (t + 10, [100, 100])]),
                              o=keys([(t, 0), (t + 3, 100)], LINEAR)))
        c.layer(f"아이콘 {i}", [lit, *benefit_icon(kind, x, icon_y, 20, "@textTertiary"), grp(ellipse(x, icon_y, 20), fill("@sunken"))],
                anchor=(x, icon_y), scale=pop(0, 10))
    # 끝: 카드가 줄며 사라지는 사이 가운데에 Plus 배지(사용자 테마 색)
    end = starts[-1] + hold + 4
    first = starts[0] + 4
    c.layer("배지", [grp(*path(SPARKLES, 1.25, PLUS_CX, stage_y), fill("@onAccent")),
                   grp(rect(PLUS_CX, stage_y, 52, 52, 16), fill("@accent"))], anchor=(PLUS_CX, stage_y), scale=pop(end, end + 12))
    # 나중에 더한 층이 아래. 카드는 그림·배지 밑에
    c.layer("카드", [grp(rect(PLUS_CX, stage_y, 60 * k, 44 * k, 20), stroke("@line", 1), fill("@surface"))],
            anchor=(PLUS_CX, stage_y),
            scale=keys([(first, [80, 80, 100]), (first + 10, [100, 100, 100]), (end - 4, [100, 100, 100]), (end + 4, [70, 70, 100])]),
            opacity=keys([(first, 0), (first + 6, 100), (end - 4, 100), (end + 4, 0)], LINEAR))
    c.save()


def lines_static(c, name, lines, color="@textTertiary", width=5, opacity=50):
    c.layer(name, [group([path(f"M {x1} {y} L {x2} {y}") for x1, x2, y in lines] + [stroke(color, width, opacity)])])


def style():
    """꾸미기. 아래 도구 막대의 굵게·글자색·형광펜·목록이 차례로 눌리며 그 줄이 바뀐다."""
    c = Comp("onboarding_style", 100)
    rows = [(52, 228, 80), (52, 196, 104), (52, 150, 128)]
    tools = [62, 100, 138, 176]  # 도구 칸 가운데 x
    ty = 166
    steps = [(10, 22), (30, 42), (50, 62), (70, 82)]
    # 목록 점
    for n, (x1, x2, y) in enumerate(rows):
        a = steps[3][0] + n * 3
        c.layer(f"목록 점 {n + 1}", [group([ellipse(40, y, 3), fill("@textTertiary")])], anchor=(40, y), scale=pop(a, a + 10))
    # 굵게: 진한 색으로 두꺼워짐
    x1, x2, y = rows[0]
    c.layer("굵게", [group([path(f"M {x1} {y} L {x2} {y}"), {"ty": "st", "c": static("@textPrimary"), "o": static(100),
                                                           "w": keys([(steps[0][0], 5), (steps[0][1], 6)]), "lc": 2, "lj": 2, "ml": 4}])],
            opacity=keys([(steps[0][0], 0), (steps[0][0] + 4, 100)], LINEAR))
    x1, x2, y = rows[1]
    c.layer("글자색", [group([path(f"M {x1} {y} L {x2} {y}"),
                              {"ty": "tm", "s": static(0), "e": keys([(steps[1][0], 0), (steps[1][1], 100)], LINEAR), "o": static(0), "m": 1},
                              stroke("@accentText", 5)])])
    lines_static(c, "글줄", rows)
    x1, x2, y = rows[2]
    c.layer("형광펜", [group([path(f"M {x1 - 4} {y} L {x2 + 4} {y}"),
                              {"ty": "tm", "s": static(0), "e": keys([(steps[2][0], 0), (steps[2][1], 100)], LINEAR), "o": static(0), "m": 1},
                              {"ty": "st", "c": static("@moodB"), "o": static(100), "w": static(15), "lc": 1, "lj": 2, "ml": 4}])])
    # 도구 막대: 아이콘, 눌린 칸 바탕, 위 경계선
    k = 16 / 24
    icons = [
        [group([path("M 6 12 L 15 12 C 17.2 12 19 13.8 19 16 C 19 18.2 17.2 20 15 20 L 6 20 L 6 4 L 14 4 C 16.2 4 18 5.8 18 8 "
                      "C 18 10.2 16.2 12 14 12", k, tools[0] - 8, ty - 8), stroke("@textTertiary", 2.2)])],
        [group([path("M 7 18 L 12 5 L 17 18 M 9 13 L 15 13", k, tools[1] - 8, ty - 9), stroke("@textTertiary", 2)]),
         group([path(f"M {tools[1] - 7} {ty + 8} L {tools[1] + 7} {ty + 8}"), stroke("@accentText", 3)])],
        [group([path("M 14 4 L 20 10 L 11 19 L 5 19 L 5 13 Z", k, tools[2] - 8, ty - 9), stroke("@textTertiary", 2)]),
         group([path(f"M {tools[2] - 7} {ty + 8} L {tools[2] + 7} {ty + 8}"), stroke("@moodB", 3)])],
        [group([path(f"M {tools[3] - 3} {ty - 5} L {tools[3] + 7} {ty - 5} M {tools[3] - 3} {ty} L {tools[3] + 7} {ty} "
                     f"M {tools[3] - 3} {ty + 5} L {tools[3] + 7} {ty + 5}"), stroke("@textTertiary", 2)]),
         group([ellipse(tools[3] - 7, ty - 5, 1.3), ellipse(tools[3] - 7, ty, 1.3), ellipse(tools[3] - 7, ty + 5, 1.3), fill("@textTertiary")])],
    ]
    for n, x in enumerate(tools):
        c.layer(f"도구 {n + 1}", icons[n])
    for n, (a, b) in enumerate(steps):
        c.layer(f"눌림 {n + 1}", [group([rect(tools[n], ty, 30, 30, 8), fill("@selected")])], anchor=(tools[n], ty),
                opacity=keys([(a - 2, 0), (a + 2, 100), (b + 2, 100), (b + 8, 0)], LINEAR))
    c.layer("막대 선", [group([path("M 9 146 L 279 146"), stroke("@line", 1)])])
    card(c)
    c.save()


def find():
    """찾기와 돌아보기. 기분·날씨 조건을 고르면 목록이 걸러지고, 기분 막대가 올라온다."""
    c = Comp("onboarding_find", 100)
    for i, (x, h, start) in enumerate([(208, 22, 60), (226, 38, 64), (244, 54, 68)]):
        c.layer(f"막대 {i + 1}", [group([rect(0, -h / 2, 12, h, 4), fill("@accentText")])], pos=static([x, 172, 0]),
                scale=keys([(start, [100, 0, 100]), (start + 12, [100, 100, 100])]))
    c.layer("통계 칸", [group([rect(226, 142, 78, 74, 16), stroke("@line", 1), fill("@surface")])], anchor=(226, 142),
            scale=pop(52, 64))
    for n, y in enumerate([164, 136]):
        c.layer(f"걸러지는 줄 {n + 1}", [*face(46, y, 8), group([path(f"M 62 {y} L 168 {y}"), stroke("@textTertiary", 5, 50)])],
                anchor=(110, y), opacity=keys([(34 + n * 4, 100), (46 + n * 4, 0)]),
                scale=keys([(34 + n * 4, [100, 100, 100]), (46 + n * 4, [100, 70, 100])]))
    c.layer("남는 줄", [*face(46, 108, 8), group([path("M 62 108 L 168 108"), stroke("@textTertiary", 5, 50)])])
    # 칩: 왼쪽 9 · 아이콘 14 · 사이 6 · 줄 14 · 오른쪽 10, 칩 사이 8
    w, left1 = 53, 44
    left2 = left1 + w + 8
    sx = left2 + 9 + 7
    sun = [group([ellipse(sx, 74, 3), stroke("@accentText", 1.6)])] + [
        group([path(f"M {sx + 5 * math.cos(a)} {74 + 5 * math.sin(a)} L {sx + 7 * math.cos(a)} {74 + 7 * math.sin(a)}"),
               stroke("@accentText", 1.6)]) for a in [k * math.pi / 4 for k in range(8)]]
    c.layer("날씨 칩", [*sun, group([path(f"M {left2 + 29} 74 L {left2 + 43} 74"), stroke("@accentText", 3)]),
                       group([rect(left2 + w / 2, 74, w, 26, 13), fill("@selected")])], anchor=(left2 + w / 2, 74), scale=pop(20, 32))
    c.layer("기분 칩", [*face(left1 + 16, 74, 7), group([path(f"M {left1 + 29} 74 L {left1 + 43} 74"), stroke("@accentText", 3)]),
                       group([rect(left1 + w / 2, 74, w, 26, 13), fill("@selected")])], anchor=(left1 + w / 2, 74), scale=pop(10, 22))
    k = 14 / 24
    c.layer("찾기 칸", [
        group([ellipse(41, 38, 5), stroke("@textTertiary", 1.8)]),
        group([path("M 45 42 L 49 46"), stroke("@textTertiary", 1.8)]),
        group([path("M 60 40 L 120 40"), stroke("@textTertiary", 4, 40)]),
        group([rect(144, 40, 236, 28, 14), fill("@sunken")]),
    ])
    c.layer("카드", [group([rect(144, 104, 270, 176, 16), stroke("@line", 1), fill("@surface")])])
    c.save()


def trash():
    """휴지통 30일. 일기가 휴지통에 들어갔다가 다시 나와 제자리로 돌아온다."""
    c = Comp("onboarding_trash", 104)
    top, bin_y = (144, 50), (144, 150)
    check(182, 26, 12, c, 84, 98)
    lid = keys([(0, 0), (12, 0), (18, -20), (34, -20), (40, 0), (50, 0), (56, -20), (74, -20), (80, 0)])
    # 백업 그림의 구름처럼 연보라 원 안에 보라 선 휴지통(Lucide trash-2)
    k = 40 / 24
    dx, dy = bin_y[0] - 20, bin_y[1] - 20
    c.layer("뚜껑", [
        group([path("M 3 6 L 21 6", k, dx, dy), path("M 8 6 L 8 4 C 8 3 9 2 10 2 L 14 2 C 15 2 16 3 16 4 L 16 6", k, dx, dy),
               stroke("@accentText", 3.2)]),
    ], anchor=(dx + 3 * k, dy + 6 * k), rotation=lid)
    c.layer("통", [
        group([path("M 19 6 L 19 20 C 19 21 18 22 17 22 L 7 22 C 6 22 5 21 5 20 L 5 6", k, dx, dy),
               path("M 10 11 L 10 17 M 14 11 L 14 17", k, dx, dy), stroke("@accentText", 3.2)]),
    ])
    c.layer("원", [group([ellipse(*bin_y, 48), fill("@selected")])])
    pos = keys([(0, [*top, 0]), (16, [*top, 0]), (34, [*bin_y, 0]), (54, [*bin_y, 0]), (74, [*top, 0])])
    scl = keys([(16, [100, 100, 100]), (34, [40, 40, 100]), (54, [40, 40, 100]), (74, [100, 100, 100])])
    opa = keys([(0, 100), (30, 100), (34, 0), (54, 0), (58, 100)], LINEAR)
    c.layer("일기", [
        *face(118, 48, 9),
        group([path("M 134 48 L 172 48"), stroke("@textTertiary", 4, 50)]),
        group([path("M 116 68 L 164 68"), stroke("@textTertiary", 4, 50)]),
        group([rect(144, 58, 92, 60, 12), stroke("@line", 1), fill("@surface")]),
    ], anchor=top, pos=pos, scale=scl, opacity=opa)
    c.save()


def tablet():
    """태블릿. 폰 화면이 태블릿만큼 넓어지고, 목록 옆에 고른 일기가 열린다."""
    c = Comp("onboarding_tablet", 100)
    grow = (14, 40)
    detail_in = keys([(40, [196, 104, 0]), (58, [184, 104, 0])])
    c.layer("상세", [
        *face(140, 52, 9),
        group([path("M 130 74 L 236 74"), stroke("@textTertiary", 5, 50)]),
        group([path("M 130 90 L 210 90"), stroke("@textTertiary", 5, 50)]),
        *image_icon(170, 132, 18, "@surface"),
        group([rect(170, 132, 84, 46, 8), fill("@moodA")]),
    ], anchor=(184, 104), pos=detail_in, opacity=keys([(40, 0), (54, 100)]))
    rows = []
    for n, y in enumerate([40, 64, 88, 112, 136]):
        rows.append(group([path(f"M -14 {y} L 18 {y}"), stroke("@textTertiary", 4, 50)]))
        rows.append(group([ellipse(-26, y, 4.5), fill("@moodA" if n else "@face")]))
        rows.append(group([rect(-4, y, 64, 18, 6), fill("@selected" if n == 0 else "@sunken")]))
    c.layer("목록", rows, pos=keys([(grow[0], [144, 12, 0]), (grow[1], [64, 12, 0])]))
    frame = {"ty": "rc", "p": static([0, 0]), "s": keys([(grow[0], [92, 176]), (grow[1], [252, 176])]), "r": static(16), "d": 1}
    c.layer("기기", [group([frame, stroke("@line", 1.5), fill("@surface")])], pos=static([144, 104, 0]))
    c.save()


if __name__ == "__main__":
    write_lines()
    backup()
    plus()
    style()
    find()
    trash()
    tablet()
