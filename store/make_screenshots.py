"""Frames raw emulator screenshots into Play Store images (1080x1920) with a caption per language.

    python store/make_screenshots.py <raw-dir>

<raw-dir>/<app-locale>/<n>-<name>.png are 1080x2160 captures (see store/README.md);
the output goes to store/screenshots/<play-locale>/<n>-<name>.png.
"""
import os
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from listings import APP_LOCALE, L  # noqa: E402

W, H = 1080, 1920
SHOT_H = 1540
TOP = 340
FONTS = "C:/Windows/Fonts" if os.name == "nt" else "/usr/share/fonts"
LATIN = os.path.join(FONTS, "seguibl.ttf")       # Segoe UI Black
DEVANAGARI = os.path.join(FONTS, "NirmalaB.ttf")  # Nirmala UI Bold

BG_TOP, BG_BOTTOM = (7, 7, 22), (20, 12, 51)
CYAN, MAGENTA = (34, 211, 238), (232, 121, 249)


def background() -> Image.Image:
    bg = Image.new("RGB", (W, H))
    px = ImageDraw.Draw(bg)
    for y in range(H):
        t = y / (H - 1)
        px.line([(0, y), (W, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM)))
    # Aurora lights, like the game background.
    glow = Image.new("RGB", (W, H), (0, 0, 0))
    g = ImageDraw.Draw(glow)
    for (cx, cy, r, color) in [(250, 260, 520, (124, 58, 237)), (950, 1100, 460, (6, 182, 212)), (180, 1800, 560, (219, 39, 119))]:
        g.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    glow = glow.filter(ImageFilter.GaussianBlur(170))
    return Image.blend(bg, Image.eval(glow, lambda v: v * 0.55).convert("RGB"), 0.5).point(lambda v: min(255, v * 2))


def caption(img: Image.Image, text: str, devanagari: bool) -> None:
    path = DEVANAGARI if devanagari else LATIN
    size = 84
    while True:
        font = ImageFont.truetype(path, size)
        box = ImageDraw.Draw(img).textbbox((0, 0), text, font=font)
        if box[2] - box[0] <= W - 120 or size <= 44:
            break
        size -= 2
    tw, th = box[2] - box[0], box[3] - box[1]
    x = (W - tw) // 2 - box[0]
    y = (TOP - th) // 2 - box[1] + 20
    mask = Image.new("L", (W, H), 0)
    ImageDraw.Draw(mask).text((x, y), text, font=font, fill=255)
    # Soft magenta glow behind the letters.
    halo = Image.new("RGB", (W, H), MAGENTA)
    img.paste(halo, (0, 0), mask.filter(ImageFilter.GaussianBlur(16)).point(lambda v: v * 0.55))
    grad = Image.new("RGB", (W, H))
    gd = ImageDraw.Draw(grad)
    left, right = x + box[0], x + box[0] + tw
    for xx in range(W):
        t = min(1, max(0, (xx - left) / max(1, right - left)))
        gd.line([(xx, 0), (xx, H)], fill=tuple(int(a + (b - a) * t) for a, b in zip(CYAN, MAGENTA)))
    img.paste(grad, (0, 0), mask)


def phone(img: Image.Image, shot: Image.Image) -> None:
    w = int(shot.width * SHOT_H / shot.height)
    shot = shot.convert("RGB").resize((w, SHOT_H), Image.LANCZOS)
    x = (W - w) // 2
    radius = 46
    mask = Image.new("L", (w, SHOT_H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, SHOT_H - 1], radius, fill=255)
    # Glow and glass border around the screen.
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle([x - 6, TOP - 6, x + w + 6, TOP + SHOT_H + 6], radius + 6, fill=200)
    img.paste(Image.new("RGB", (W, H), (139, 92, 246)), (0, 0), shadow.filter(ImageFilter.GaussianBlur(28)).point(lambda v: v * 0.6))
    img.paste(shot, (x, TOP), mask)
    ImageDraw.Draw(img).rounded_rectangle([x, TOP, x + w - 1, TOP + SHOT_H - 1], radius, outline=(255, 255, 255, 60), width=3)


def main(raw: str) -> None:
    out_root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "screenshots")
    bg = background()
    for loc, t in L.items():
        src = os.path.join(raw, APP_LOCALE[loc])
        names = sorted(f for f in os.listdir(src) if f.endswith(".png")) if os.path.isdir(src) else []
        if len(names) != len(t["captions"]):
            print("skip", loc, len(names))
            continue
        out = os.path.join(out_root, loc)
        os.makedirs(out, exist_ok=True)
        for name, text in zip(names, t["captions"]):
            img = bg.copy()
            caption(img, text, devanagari=loc == "hi-IN")
            phone(img, Image.open(os.path.join(src, name)))
            img.save(os.path.join(out, name), optimize=True)
        print("ok", loc)


if __name__ == "__main__":
    main(sys.argv[1])
