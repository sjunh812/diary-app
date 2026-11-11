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
    c.layer("펜", [group([path(PEN, k, -5 * k, -19 * k), fill("@accent")])],
            pos=keys(points, LINEAR), rotation=keys(tilt, LINEAR),
            opacity=keys([(start - 4, 0), (start, 100), (finish, 100), (finish + 4, 0)]))


def card(c):
    c.layer("얼굴", [*face(54, 50, 18)], anchor=(54, 50), scale=pop(0, 14))
    c.layer("카드", [group([rect(144, 104, 270, 176, 16), stroke("@line", 1), fill("@surface")])])


def write_lines():
    """공책 줄처럼 곧은 줄 셋을 넓게 둔다. 줄 끝은 길이를 달리해 글처럼 보이게 한다."""
    speed = 240 / FPS  # 1초에 240. 110은 늘어졌다.
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
    c.layer("구름", [group([path(CLOUD, 44 / 24, end[0] - 22, end[1] - 21), stroke("@accent", 3.2)]),
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


def plus():
    """결제 화면 머리 그림. 일기 한 장이 칸을 채우고, 광고 자리가 접힌 뒤 사진이 차고 통계가 붙는다. 카드 아래는 칸 밖으로 잘린다."""
    c = Comp("plus_benefits", 84, "shared/feature/plus/src/commonMain/composeResources/files", 350, 220)
    for i, (x, h, start) in enumerate([(264, 20, 58), (282, 34, 62), (300, 50, 66)]):
        c.layer(f"막대 {i + 1}", [group([rect(0, -h / 2, 14, h, 4.5), fill("@accent")])], pos=static([x, 204, 0]),
                scale=keys([(start, [100, 0, 100]), (start + 12, [100, 100, 100])]))
    c.layer("통계 칸", [group([rect(282, 170, 92, 84, 18), stroke("@line", 1), fill("@surface")])], anchor=(282, 170),
            scale=pop(50, 62))
    for i, (x, color, start) in enumerate([(97.5, "@moodA", 22), (175, "@moodB", 30), (252.5, "@moodC", 38)]):
        c.layer(f"사진 {i + 1}", [*image_icon(x, 143, 28, "@surface"), group([rect(x, 143, 67, 67, 12), fill(color)])],
                anchor=(x, 143), scale=pop(start, start + 14))
    c.layer("광고 자리", [
        group([path("M 84 134 L 214 134"), stroke("@line", 7)]),
        group([path("M 84 152 L 160 152"), stroke("@line", 7)]),
        group([rect(175, 143, 222, 67, 12), fill("@sunken")]),
    ], anchor=(175, 143), scale=keys([(0, [100, 100, 100]), (8, [100, 100, 100]), (22, [100, 0, 100])]),
        opacity=keys([(8, 100), (22, 0)], LINEAR))
    c.layer("일기 머리", [
        *face(82, 70, 18),
        group([path("M 114 61 L 262 61"), stroke("@line", 8)]),
        group([path("M 114 80 L 208 80"), stroke("@line", 8)]),
    ])
    c.layer("일기 카드", [group([path("M 64 200 L 196 200"), stroke("@line", 8)]),
                       group([rect(175, 150, 262, 240, 22), stroke("@line", 1), fill("@surface")])])
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
    # 굵게: 진한 색으로 두꺼워진다
    x1, x2, y = rows[0]
    c.layer("굵게", [group([path(f"M {x1} {y} L {x2} {y}"), {"ty": "st", "c": static("@textPrimary"), "o": static(100),
                                                           "w": keys([(steps[0][0], 5), (steps[0][1], 6)]), "lc": 2, "lj": 2, "ml": 4}])],
            opacity=keys([(steps[0][0], 0), (steps[0][0] + 4, 100)], LINEAR))
    x1, x2, y = rows[1]
    c.layer("글자색", [group([path(f"M {x1} {y} L {x2} {y}"),
                              {"ty": "tm", "s": static(0), "e": keys([(steps[1][0], 0), (steps[1][1], 100)], LINEAR), "o": static(0), "m": 1},
                              stroke("@accent", 5)])])
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
         group([path(f"M {tools[1] - 7} {ty + 8} L {tools[1] + 7} {ty + 8}"), stroke("@accent", 3)])],
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
        c.layer(f"막대 {i + 1}", [group([rect(0, -h / 2, 12, h, 4), fill("@accent")])], pos=static([x, 172, 0]),
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
    sun = [group([ellipse(sx, 74, 3), stroke("@accent", 1.6)])] + [
        group([path(f"M {sx + 5 * math.cos(a)} {74 + 5 * math.sin(a)} L {sx + 7 * math.cos(a)} {74 + 7 * math.sin(a)}"),
               stroke("@accent", 1.6)]) for a in [k * math.pi / 4 for k in range(8)]]
    c.layer("날씨 칩", [*sun, group([path(f"M {left2 + 29} 74 L {left2 + 43} 74"), stroke("@accent", 3)]),
                       group([rect(left2 + w / 2, 74, w, 26, 13), fill("@selected")])], anchor=(left2 + w / 2, 74), scale=pop(20, 32))
    c.layer("기분 칩", [*face(left1 + 16, 74, 7), group([path(f"M {left1 + 29} 74 L {left1 + 43} 74"), stroke("@accent", 3)]),
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
               stroke("@accent", 3.2)]),
    ], anchor=(dx + 3 * k, dy + 6 * k), rotation=lid)
    c.layer("통", [
        group([path("M 19 6 L 19 20 C 19 21 18 22 17 22 L 7 22 C 6 22 5 21 5 20 L 5 6", k, dx, dy),
               path("M 10 11 L 10 17 M 14 11 L 14 17", k, dx, dy), stroke("@accent", 3.2)]),
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
