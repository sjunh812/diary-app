#!/usr/bin/env python3
"""온보딩 로띠의 끝 모습을 SVG로 뽑는다. Figma "로띠 끝 모습" 프레임용.

  python3 -I scripts/lottie/end_svg.py <로띠.json> > 끝.svg

make_onboarding.py가 쓰는 도형(sh, rc, el, fl, st, tm)만 다룬다.
색 자리표시(@accent 등)는 토큰마다 정한 표시 색으로 바꾼다. Figma에서 이 색을 같은 이름 변수로 다시 묶는다.
"""
import json
import math
import sys

# 토큰별 표시 색. 겹치지 않게 첫 자리만 변경
MARK = ["@surface", "@sunken", "@selected", "@line", "@accent", "@onAccent", "@textTertiary",
        "@face", "@moodA", "@moodB", "@moodC", "@faceLine", "@textPrimary"]


def mark_hex(token):
    return "#%02x0101" % (MARK.index(token) + 1)


def last(prop):
    """끝 프레임 값."""
    if prop.get("a") == 1:
        return prop["k"][-1]["s"]
    return prop["k"]


def num(v):
    return v[0] if isinstance(v, list) else v


def shape_path(it):
    if it["ty"] == "sh":
        k = last(it["ks"])
        k = k[0] if isinstance(k, list) else k
        v, i, o = k["v"], k["i"], k["o"]
        d = f"M {v[0][0]} {v[0][1]}"
        n = len(v)
        for j in range(1, n + (1 if k["c"] else 0)):
            a, b = v[j - 1], v[j % n]
            c1 = (a[0] + o[j - 1][0], a[1] + o[j - 1][1])
            c2 = (b[0] + i[j % n][0], b[1] + i[j % n][1])
            d += f" C {c1[0]} {c1[1]} {c2[0]} {c2[1]} {b[0]} {b[1]}"
        return d + (" Z" if k["c"] else "")
    if it["ty"] == "rc":
        cx, cy = last(it["p"])
        w, h = last(it["s"])
        r = min(num(last(it["r"])), w / 2, h / 2)
        x, y = cx - w / 2, cy - h / 2
        return (f"M {x + r} {y} L {x + w - r} {y} A {r} {r} 0 0 1 {x + w} {y + r} L {x + w} {y + h - r} "
                f"A {r} {r} 0 0 1 {x + w - r} {y + h} L {x + r} {y + h} A {r} {r} 0 0 1 {x} {y + h - r} "
                f"L {x} {y + r} A {r} {r} 0 0 1 {x + r} {y} Z")
    if it["ty"] == "el":
        cx, cy = last(it["p"])
        rx = last(it["s"])[0] / 2
        return f"M {cx - rx} {cy} A {rx} {rx} 0 1 0 {cx + rx} {cy} A {rx} {rx} 0 1 0 {cx - rx} {cy} Z"
    return None


def group_svg(gr):
    paths, out = [], []
    for it in gr["it"]:
        p = shape_path(it)
        if p:
            paths.append(p)
    d = " ".join(paths)
    styles = [it for it in gr["it"] if it["ty"] in ("fl", "st")]
    for st in sorted(styles, key=lambda s: s["ty"] != "fl"):
        color = mark_hex(st["c"]["k"])
        alpha = num(last(st["o"])) / 100
        if st["ty"] == "fl":
            out.append(f'<path d="{d}" fill="{color}" fill-opacity="{alpha}"/>')
        else:
            cap = {1: "butt", 2: "round", 3: "square"}[st.get("lc", 2)]
            out.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-opacity="{alpha}" '
                       f'stroke-width="{num(last(st["w"]))}" stroke-linecap="{cap}" stroke-linejoin="round"/>')
    return "".join(out)


def layer_svg(layer):
    ks = layer["ks"]
    if num(last(ks["o"])) <= 0:
        return ""
    px, py = last(ks["p"])[:2]
    ax, ay = last(ks["a"])[:2]
    sx, sy = [v / 100 for v in last(ks["s"])[:2]]
    r = num(last(ks["r"]))
    t = f"translate({px} {py}) rotate({r}) scale({sx} {sy}) translate({-ax} {-ay})"
    # 같은 층 안에서는 앞에 적은 그룹이 위
    body = "".join(group_svg(g) for g in reversed(layer["shapes"]))
    return f'<g transform="{t}" opacity="{num(last(ks["o"])) / 100}">{body}</g>'


def main():
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    # 앞에 적은 층이 위
    body = "".join(layer_svg(l) for l in reversed(data["layers"]))
    print(f'<svg xmlns="http://www.w3.org/2000/svg" width="{data["w"]}" height="{data["h"]}" viewBox="0 0 {data["w"]} {data["h"]}">{body}</svg>')


if __name__ == "__main__":
    main()
