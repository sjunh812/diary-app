#!/usr/bin/env python3
"""온보딩 설명 그림 로띠 두 개를 만든다.

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
    """M, L, C, Z만 쓰는 절대 좌표 경로를 로띠 도형으로 바꾼다."""
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
    return {"ty": "gr", "it": items + [tr]}


class Comp:
    def __init__(self, name, frames):
        self.name, self.frames, self.layers = name, frames, []

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
        data = {"v": "5.7.4", "fr": FPS, "ip": 0, "op": self.frames, "w": W, "h": H, "nm": self.name, "ddd": 0, "assets": [],
                "layers": self.layers}
        with open(os.path.join(OUT, f"{self.name}.json"), "w") as f:
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
    start, end, peak = (46, 104), (240, 104), 36
    check(268, 136, 16, c, 74, 90)
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
        *face(24, 46, 10),
        group([path("M 16 70 L 76 70"), stroke("@sunken", 6)]),
        group([path("M 16 84 L 76 84"), stroke("@sunken", 6)]),
        *image_icon(46, 130, 16, "@textTertiary"),
        group([rect(46, 130, 68, 60, 8), fill("@sunken")]),
        group([rect(46, 104, 91, 155, 16), stroke("@line", 1), fill("@surface")]),
    ])
    c.save()


if __name__ == "__main__":
    write_lines()
    backup()
