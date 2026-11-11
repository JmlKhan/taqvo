#!/usr/bin/env python3
"""Uzbek Latin -> Cyrillic, the same rules as widgetcore's UzbekCyrillic.kt.

    python3 tools/uz_cyrillic.py            # rewrites the Cyrillic resources from the Latin ones
    python3 tools/uz_cyrillic.py --check    # fails if they are out of date

Writes shared/src/commonMain/composeResources/values-b+uz+Cyrl/strings.xml (from values-uz) and
files/adhkar/text/uz-Cyrl.json (from uz.json). The Android app's own resources (widget labels,
channel names) are the system's to pick and stay Latin: Android has no Cyrillic Uzbek.
Run it after changing any Uzbek (Latin) string; the Cyrillic files are generated, never edited.
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

KEEP_WORDS = {
    "Tazkir", "Android", "iPhone", "iOS", "iPad", "Google", "Apple", "Safari", "Chrome", "Samsung",
    "TestFlight", "App", "Store", "Play", "GitHub", "Wi", "Fi", "WiFi", "TTS", "MB", "GB", "KB",
    "Manrope", "Amiri", "Lora", "Nunito", "OFL", "CC", "BY", "SA", "PDF", "URL", "OK", "AM", "PM",
    "Tasma", "Hijri", "Quran", "Sahih", "Saheeh", "QuranEnc", "Kuliev", "Kulieva",
}
PLACEHOLDER = re.compile(r"%(\d+\$)?[sd]|%%|\\n|\\'|&[a-zA-Z]+;|\{[a-z]+\}|<[^>]+>")
NOT_UZBEK_C = re.compile(r"c(?!h)")
SINGLE = {
    "a": "а", "b": "б", "d": "д", "e": "е", "f": "ф", "g": "г", "h": "ҳ", "i": "и", "j": "ж",
    "k": "к", "l": "л", "m": "м", "n": "н", "o": "о", "p": "п", "q": "қ", "r": "р", "s": "с",
    "t": "т", "u": "у", "v": "в", "x": "х", "y": "й", "z": "з",
}
TUTUQ = set("’ʼ")
APOS = set("'‘’ʻʼ`")


def is_letter(c):
    return ("a" <= c <= "z") or ("A" <= c <= "Z")


def word(w):
    # The product name stays in Latin script, and its Uzbek suffix is written in Cyrillic after it
    # ("Tazkir’ni" -> "Tazkirни"), as the bold forms already are.
    if w.startswith("Tazkir") and len(w) > 7 and w[6] in APOS:
        return "Tazkir" + word(w[7:])
    letters = "".join(c for c in w if is_letter(c))
    low = letters.lower()
    if letters in KEEP_WORDS or "w" in low or NOT_UZBEK_C.search(low):
        return w
    out = []
    i = 0
    while i < len(w):
        c = w[i]
        lc = c.lower()
        nxt = w[i + 1] if i + 1 < len(w) else None
        lnext = nxt.lower() if nxt else None
        upper = c.isupper()
        shout = upper and nxt is not None and nxt.isupper()

        def put(cyr, consumed=1):
            nonlocal i
            if upper:
                cyr = cyr.upper() if (shout and len(cyr) > 1) else cyr[0].upper() + cyr[1:]
            out.append(cyr)
            i += consumed

        if lc in "og" and nxt is not None and nxt in APOS and nxt not in TUTUQ:
            put("ў" if lc == "o" else "ғ", 2)
        elif lc == "s" and lnext == "h":
            put("ш", 2)
        elif lc == "c" and lnext == "h":
            put("ч", 2)
        elif lc == "y" and lnext == "a":
            put("я", 2)
        elif lc == "y" and lnext == "u":
            put("ю", 2)
        elif lc == "y" and lnext == "o":
            put("ё", 2)
        elif lc == "y" and lnext == "e":
            put("е", 2)
        elif lc == "e" and i == 0:
            put("э")
        elif c in APOS:
            out.append("ъ")
            i += 1
        elif lc in SINGLE:
            put(SINGLE[lc])
        else:
            out.append(c)
            i += 1
    return "".join(out)


def from_latin(text):
    out = []
    i = 0
    n = len(text)
    while i < n:
        if text.startswith("**", i):
            end = text.find("**", i + 2)
            if end > 0:
                out.append(text[i:end + 2])
                i = end + 2
                continue
        m = PLACEHOLDER.match(text, i)
        if m:
            out.append(m.group(0))
            i = m.end()
            continue
        if is_letter(text[i]):
            j = i
            while j < n and (is_letter(text[j]) or (text[j] in APOS and j + 1 < n and is_letter(text[j + 1]) and j > i)):
                j += 1
            out.append(word(text[i:j]))
            i = j
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


# Values that are not Uzbek words at all, or that the Cyrillic file says differently.
# The three widget names the Widgets steps tell the reader to look for in the phone's own widget
# list, which is in the system's Latin Uzbek: they must match it, so they are not respelled.
KEEP_LATIN = {"widgets_prayer_name", "widgets_ayah_name", "widgets_dua_name"}

OVERRIDES = {
    "ui_language": "uz-Cyrl",
    "language_name": "Ўзбекча",
}


def convert_strings(src_path, dst_path, check):
    text = open(src_path, encoding="utf-8").read()

    def repl(m):
        name, body = m.group(1), m.group(3)
        if name in KEEP_LATIN:
            return m.group(0)
        if name in OVERRIDES:
            return m.group(0).replace(body, OVERRIDES[name])
        return m.group(0).replace(body, from_latin(body)) if body.strip() else m.group(0)

    out = re.sub(r'<string name="([^"]+)"([^>]*)>(.*?)</string>', repl, text, flags=re.S)
    # <item quantity="one">…</item> inside <plurals>
    out = re.sub(r'(<item quantity="[a-z]+">)(.*?)(</item>)',
                 lambda m: m.group(1) + from_latin(m.group(2)) + m.group(3), out, flags=re.S)
    return write(dst_path, out, check)


def write(path, content, check):
    if check:
        have = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if have != content:
            print("out of date:", os.path.relpath(path, ROOT))
            return False
        return True
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("wrote", os.path.relpath(path, ROOT))
    return True


def convert_json(node):
    if isinstance(node, str):
        return from_latin(node)
    if isinstance(node, list):
        return [convert_json(x) for x in node]
    if isinstance(node, dict):
        return {k: (v if k in ("language", "reviewed", "schema") else convert_json(v)) for k, v in node.items()}
    return node


def main():
    check = "--check" in sys.argv
    ok = True
    compose = os.path.join(ROOT, "shared/src/commonMain/composeResources")
    ok &= convert_strings(os.path.join(compose, "values-uz/strings.xml"), os.path.join(compose, "values-b+uz+Cyrl/strings.xml"), check)
    src = os.path.join(compose, "files/adhkar/text/uz.json")
    if os.path.exists(src):
        data = json.load(open(src, encoding="utf-8"))
        data = convert_json(data)
        if "language" in data:
            data["language"] = "uz-Cyrl"
        ok &= write(os.path.join(compose, "files/adhkar/text/uz-Cyrl.json"), json.dumps(data, ensure_ascii=False, indent=2) + "\n", check)
    return 0 if ok else 1


if __name__ == "__main__":
    if "--selftest" in sys.argv:
        for s in ["Oʻzbekcha", "Qur’on", "yaxshi", "ekran", "Sozlamalar", "Shom namozi", "gʻoya", "%1$s ta oyat", "**Add**",
                  "Yordam", "Bomdod", "Kechki zikrlar", "ma’no", "Tazkir"]:
            print(s, "->", from_latin(s))
        sys.exit(0)
    sys.exit(main())
