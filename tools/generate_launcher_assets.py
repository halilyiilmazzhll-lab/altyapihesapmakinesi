from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "artwork" / "app_icon_source.png"
RES = ROOT / "app" / "src" / "main" / "res"
LIGHT = (242, 242, 247, 255)


def fitted_mark(source: Image.Image, canvas_size: int, fill_ratio: float) -> Image.Image:
    alpha_box = source.getchannel("A").getbbox()
    if alpha_box is None:
        raise ValueError("The icon source has no visible pixels")
    mark = source.crop(alpha_box)
    limit = round(canvas_size * fill_ratio)
    mark.thumbnail((limit, limit), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    x = (canvas_size - mark.width) // 2
    y = (canvas_size - mark.height) // 2
    canvas.alpha_composite(mark, (x, y))
    return canvas


def legacy_icon(source: Image.Image, size: int, round_icon: bool) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    inset = max(1, round(size * 0.025))
    box = (inset, inset, size - inset - 1, size - inset - 1)
    if round_icon:
        draw.ellipse(box, fill=255)
    else:
        draw.rounded_rectangle(box, radius=round(size * 0.22), fill=255)
    background = Image.new("RGBA", (size, size), LIGHT)
    canvas.alpha_composite(Image.composite(background, canvas, mask))
    canvas.alpha_composite(fitted_mark(source, size, 0.72))
    return canvas


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    densities = {
        "mdpi": (48, 108),
        "hdpi": (72, 162),
        "xhdpi": (96, 216),
        "xxhdpi": (144, 324),
        "xxxhdpi": (192, 432),
    }
    for density, (legacy_size, adaptive_size) in densities.items():
        folder = RES / f"mipmap-{density}"
        legacy_icon(source, legacy_size, False).save(
            folder / "ic_launcher.webp", "WEBP", quality=100, lossless=True
        )
        legacy_icon(source, legacy_size, True).save(
            folder / "ic_launcher_round.webp", "WEBP", quality=100, lossless=True
        )
        fitted_mark(source, adaptive_size, 0.64).save(
            folder / "ic_launcher_foreground.webp", "WEBP", quality=100, lossless=True
        )

    splash = fitted_mark(source, 288, 0.68)
    splash.save(RES / "drawable-nodpi" / "ic_splash_brand.png", optimize=True)


if __name__ == "__main__":
    main()
