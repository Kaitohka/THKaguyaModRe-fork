#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Language-file audit for the THKaguyaMod 1.21.1 port (resource only, no Java edits).

Scans the five lang JSONs under
``porting/neoforge-1.21.1/src/main/resources/assets/thkaguyamod/lang/`` and asserts:

1. every file is valid JSON (``json.load``);
2. all five files expose the *same* key set (missing/extra keys per language are printed);
3. every entity in ``init/ModEntities.java`` has ``entity.thkaguyamod.<registry_id>``;
4. every registered item / block has its ``item.`` / ``block.`` key;
5. every dynamic / legacy key that Java builds at runtime exists
   (``entity.<registry_path>.name`` from ``EntityTHFairy#getName``,
   ``item.thSpellCard.<id>.name`` from ``THKaguyaLib#spellCardDeclaration``,
   ``entity.Sakuya.name`` from ``EntitySakuya#mobInteract``,
   ``thKaguya.color.0..11`` / ``thKaguya.danmakuForm.0..6``);
6. every literal ``Component.translatable("...")`` in the Java tree resolves (comments stripped).

Usage::

    python porting/tools/check_lang.py            # read-only audit; exit 1 on any failure
    python porting/tools/check_lang.py --audit    # print the markdown audit table of overlay keys
    python porting/tools/check_lang.py --apply    # (re)apply the key overlay, then re-check

``--apply`` exists because ``build_phase3_assets.py`` *generates* these JSONs; a regeneration
drops the keys added here.  Re-running ``--apply`` restores them deterministically and is
idempotent (existing values are never overwritten).  The overlay tables below are the single
source of truth for the keys that the 1.7.10 lang files never had.
"""

import argparse
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
MODULE = os.path.join(ROOT, 'porting', 'neoforge-1.21.1')
LANG_DIR = os.path.join(MODULE, 'src', 'main', 'resources', 'assets', 'thkaguyamod', 'lang')
JAVA_DIR = os.path.join(MODULE, 'src', 'main', 'java')
INIT_DIR = os.path.join(JAVA_DIR, 'thkaguyamod', 'init')
ENTITY_SRC = os.path.join(INIT_DIR, 'ModEntities.java')
ITEM_SRC = os.path.join(INIT_DIR, 'ModItems.java')
BLOCK_SRC = os.path.join(INIT_DIR, 'ModBlocks.java')
SPELLCARD_SRC = os.path.join(INIT_DIR, 'ModSpellCards.java')

LANGS = ['en_us', 'ja_jp', 'ko_kr', 'zh_cn', 'zh_tw']
REFERENCE = 'en_us'

# Keys owned by vanilla Minecraft (present in the vanilla lang, not in the mod jar).
VANILLA_KEYS = {'container.inventory'}

# ---------------------------------------------------------------------------
# Overlay: entity names for the 32 entities whose names never existed in 1.7.10.
# Most of them render an existing item/block, so the value is taken from that
# item's key in the *same* language; the rest are newly written.
# ---------------------------------------------------------------------------
ENTITY_VALUE_FROM = {
    'aja_red_stone_effect': 'item.thkaguyamod.red_stone_of_aja',
    'cursed_decoy_doll': 'item.thkaguyamod.cursed_decoy_doll',
    'divine_spirit': 'block.thkaguyamod.divine_spirit_red',
    'dragon_neck_jewel': 'item.thkaguyamod.jewel_from_the_dragons_neck',
    'hisou': 'item.thkaguyamod.sword_of_scarlet_perception',
    'homing_amulet': 'item.thkaguyamod.homing_amulet',
    'kinkakuzi': 'item.thkaguyamod.seamless_ceiling_of_kinkakuji',
    'marisa_broom': 'item.thkaguyamod.magic_broom',
    'mazinkyoukan': 'item.thkaguyamod.sorcerers_sutra_scroll',
    'mini_hakkero': 'item.thkaguyamod.mini_hakkero',
    'musou_fuuin': 'item.thkaguyamod.spell_card_musou_fuuin',
    'nazrin_pendulum': 'item.thkaguyamod.nazrin_pendulum',
    'normal_laser': 'item.thkaguyamod.middle_laser',
    'normal_shot': 'item.thkaguyamod.small_shot',
    'onmyoudama': 'item.thkaguyamod.yin_yang_orb',
    'private_square': 'item.thkaguyamod.sakuyas_pocket_watch',
    'sakuya_stop_watch': 'item.thkaguyamod.sakuyas_stopwatch',
    'set_laser': 'item.thkaguyamod.middle_laser',
    'shot_material': 'item.thkaguyamod.shot_material',
    'silver_knife': 'item.thkaguyamod.silver_knife_blue',
    'spell_card': 'itemGroup.thkaguyamod.spellcard',
    'spiritual_strike_talisman': 'item.thkaguyamod.spiritual_strike_talisman',
    'sukima': 'item.thkaguyamod.gap',
    'yuuka_parasol': 'item.thkaguyamod.yuukas_parasol',
}

ENTITY_NEW = {
    'cirno_ice_box': {
        'en_us': 'Ice Block', 'ja_jp': '氷ブロック', 'ko_kr': '얼음 블록',
        'zh_cn': '冰块', 'zh_tw': '冰塊'},
    'hakurei_shield': {
        'en_us': 'Hakurei Barrier', 'ja_jp': '博麗の結界', 'ko_kr': '하쿠레이 결계',
        'zh_cn': '博丽结界', 'zh_tw': '博麗結界'},
    'hakurou_reflecter': {
        'en_us': 'Hakurouken Slash', 'ja_jp': '白楼剣・斬撃', 'ko_kr': '백루검 참격',
        'zh_cn': '白楼剑·斩击', 'zh_tw': '白樓劍·斬擊'},
    'miracle_circle': {
        'en_us': 'Miracle Circle', 'ja_jp': '奇跡の魔法陣', 'ko_kr': '기적의 마법진',
        'zh_cn': '奇迹法阵', 'zh_tw': '奇蹟法陣'},
    'nuclear_shot': {
        'en_us': 'Nuclear Shot', 'ja_jp': '核弾', 'ko_kr': '핵탄',
        'zh_cn': '核弹', 'zh_tw': '核彈'},
    'sanae_wind': {
        'en_us': "Sanae's Wind", 'ja_jp': '早苗の風', 'ko_kr': '사나에의 바람',
        'zh_cn': '早苗之风', 'zh_tw': '早苗之風'},
    'spell_card_circle': {
        'en_us': 'Spell Card Circle', 'ja_jp': 'スペルカードの魔法陣', 'ko_kr': '스펠 카드 마법진',
        'zh_cn': '符卡法阵', 'zh_tw': '符卡法陣'},
    'th_item': {
        'en_us': 'Drop Item', 'ja_jp': 'ドロップアイテム', 'ko_kr': '드롭 아이템',
        'zh_cn': '掉落道具', 'zh_tw': '掉落道具'},
}

# ---------------------------------------------------------------------------
# Overlay: keys Java assembles at runtime from the 1.7.10 key scheme.
# ---------------------------------------------------------------------------
LEGACY_NAME_FROM = {
    'entity.th_fairy.name': 'entity.thkaguyamod.th_fairy',
    'entity.sunflower_fairy.name': 'entity.thkaguyamod.sunflower_fairy',
    'entity.th_phantom.name': 'entity.thkaguyamod.th_phantom',
    'entity.familiar.name': 'entity.thkaguyamod.familiar',
    'entity.Sakuya.name': 'entity.thkaguyamod.sakuya',
    'entity.Sanae.name': 'entity.thkaguyamod.sanae',
}
LEGACY_NAME_NEW = {
    'entity.THGhost.name': {
        'en_us': 'Ghost', 'ja_jp': '亡霊', 'ko_kr': '망령',
        'zh_cn': '亡灵', 'zh_tw': '亡靈'},
}

# ---------------------------------------------------------------------------
# Overlay: GUI / message keys.  1.7.10 itself lacked all three (its GUI showed
# the raw keys), so the text is newly written from the GUI semantics.
# ---------------------------------------------------------------------------
GUI_NEW = {
    'thKaguya.msg.sneScCharge': {
        'en_us': 'Not enough Miracle Power!', 'ja_jp': '奇跡の力が足りない！',
        'ko_kr': '기적의 힘이 부족합니다!', 'zh_cn': '奇迹之力不足！', 'zh_tw': '奇蹟之力不足！'},
    'miracleMallet.material': {
        'en_us': 'Material', 'ja_jp': '素材', 'ko_kr': '소재', 'zh_cn': '素材', 'zh_tw': '素材'},
    'miracleMallet.result': {
        'en_us': 'Result', 'ja_jp': '結果', 'ko_kr': '결과', 'zh_cn': '结果', 'zh_tw': '結果'},
}

# 1.7.10 lang bugs: ja_JP / zh_TW wrote this key with a comma instead of a dot, so the
# pass-through in build_phase3_assets.py silently dropped it.
COLOR_FIX = {
    'ja_jp': {'thKaguya.color.4': '紫'},
    'zh_tw': {'thKaguya.color.4': '紫'},
}


def snake(name):
    """Same camel->snake rule as RenderTHBoss#spellCardTranslationKey."""
    out, prev = '', ''
    for ch in name.replace("'", '').replace('-', '_'):
        if ch.isupper() and prev and (prev.islower() or prev.isdigit()):
            out += '_'
        out += ch.lower()
        prev = ch
    return out


def load(path):
    with open(path, encoding='utf-8') as fh:
        return json.load(fh)


def write(path, data):
    with open(path, 'w', encoding='utf-8') as fh:
        json.dump(data, fh, ensure_ascii=False, indent=2, sort_keys=True)
        fh.write('\n')


def strip_java_comments(text):
    """Remove // and /* */ comments without touching string literals."""
    out, i, n = [], 0, len(text)
    while i < n:
        ch = text[i]
        if ch == '"':
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == '"':
                    break
                j += 1
            out.append(text[i:j + 1])
            i = j + 1
        elif ch == '/' and i + 1 < n and text[i + 1] == '/':
            j = text.find('\n', i)
            i = n if j < 0 else j
        elif ch == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            i = n if j < 0 else j + 2
        else:
            out.append(ch)
            i += 1
    return ''.join(out)


def java_files():
    for base, _dirs, names in os.walk(JAVA_DIR):
        for name in names:
            if name.endswith('.java'):
                yield os.path.join(base, name)


def registered_entities():
    src = open(ENTITY_SRC, encoding='utf-8').read()
    return sorted(set(re.findall(r'ENTITY_TYPES\.register\(\s*"([^"]+)"', src)))


def registered_items():
    src = open(ITEM_SRC, encoding='utf-8').read()
    return sorted(set(re.findall(r'registerItem\(\s*"([^"]+)"', src)))


def registered_blocks():
    src = open(BLOCK_SRC, encoding='utf-8').read()
    ids = set(re.findall(r'BLOCKS\.registerBlock\(\s*"([^"]+)"\s*,', src))
    ids.update('divine_spirit_' + c for c in re.findall(r'divineSpirit\("([a-z]+)"\)', src))
    return sorted(ids)


def spellcard_names():
    """[(registration_name, old_id)] in registration order."""
    src = open(SPELLCARD_SRC, encoding='utf-8').read()
    return [(name, int(cid)) for name, cid in re.findall(
        r'registerSpellCard\(\S+?::new,\s*DOMAIN,\s*"([^"]+)",\s*(\d+)\)', src)]


def registered_spellcard_ids():
    return sorted(cid for _name, cid in spellcard_names())


def dynamic_keys():
    """Keys Java assembles at runtime (1.7.10 key scheme preserved)."""
    keys = {
        'entity.th_fairy.name', 'entity.sunflower_fairy.name',
        'entity.th_phantom.name', 'entity.familiar.name',
        'entity.THGhost.name', 'entity.Sakuya.name',
    }
    keys.update('item.thSpellCard.%d.name' % i for i in registered_spellcard_ids())
    keys.update('thKaguya.color.%d' % i for i in range(12))
    keys.update('thKaguya.danmakuForm.%d' % i for i in range(7))
    return sorted(keys)


def literal_java_keys():
    """Literal keys in Component.translatable("..."), skipping runtime-built prefix strings.

    ``THKaguyaLib`` concatenates ``"item.thSpellCard." + id + ".name"`` and ``EntityTHFairy``
    concatenates ``"entity." + path + ".name"``; those ends are covered by {@link dynamic_keys}.
    """
    found = {}
    for path in java_files():
        src = strip_java_comments(open(path, encoding='utf-8').read())
        for key in re.findall(r'translatable\(\s*"([^"]+)"', src):
            if key.endswith('.'):
                continue
            found.setdefault(key, os.path.relpath(path, ROOT).replace('\\', '/'))
    return found


def overlay():
    """The full key overlay as {lang: {key: value}}, values resolved per language."""
    lans = {lang: load(os.path.join(LANG_DIR, lang + '.json')) for lang in LANGS}
    cards = {lang: {k[len('item.thkaguyamod.'):]: v for k, v in lans[lang].items()
                    if k.startswith('item.thkaguyamod.spell_card_')} for lang in LANGS}
    out = {lang: {} for lang in LANGS}
    for lang in LANGS:
        cur = out[lang]
        for eid, src_key in ENTITY_VALUE_FROM.items():
            cur['entity.thkaguyamod.' + eid] = lans[lang][src_key]
        for eid, table in ENTITY_NEW.items():
            cur['entity.thkaguyamod.' + eid] = table[lang]
        for key, src_key in LEGACY_NAME_FROM.items():
            cur[key] = lans[lang][src_key]
        for key, table in LEGACY_NAME_NEW.items():
            cur[key] = table[lang]
        for key, table in GUI_NEW.items():
            cur[key] = table[lang]
        for name, cid in spellcard_names():
            cur['item.thSpellCard.%d.name' % cid] = cards[lang]['spell_card_' + snake(name)]
        cur.update(COLOR_FIX.get(lang, {}))
    return out


def apply_overlay():
    changed = []
    over = overlay()
    for lang in LANGS:
        path = os.path.join(LANG_DIR, lang + '.json')
        data = load(path)
        before = len(data)
        for key, value in over[lang].items():
            data.setdefault(key, value)
        if len(data) != before:
            write(path, data)
            changed.append((lang, len(data) - before))
    return changed


def check():
    failures = []

    def fail(msg):
        failures.append(msg)

    def want(key, why):
        for lang in LANGS:
            if key not in keys[lang]:
                fail('%s: missing %s (%s)' % (lang, key, why))

    keys = {}
    for lang in LANGS:
        path = os.path.join(LANG_DIR, lang + '.json')
        if not os.path.isfile(path):
            fail('%s: file missing' % path)
            continue
        try:
            keys[lang] = set(load(path))
        except ValueError as exc:
            fail('%s: invalid JSON (%s)' % (lang, exc))
    if len(keys) != len(LANGS):
        return failures, {}

    ref = keys[REFERENCE]
    for lang in LANGS:
        if lang == REFERENCE:
            continue
        missing = sorted(ref - keys[lang])
        extra = sorted(keys[lang] - ref)
        if missing:
            fail('%s: %d key(s) missing vs %s: %s' % (lang, len(missing), REFERENCE, missing))
        if extra:
            fail('%s: %d key(s) not in %s: %s' % (lang, len(extra), REFERENCE, extra))

    for eid in registered_entities():
        want('entity.thkaguyamod.' + eid, 'ModEntities registry id')
    for iid in registered_items():
        want('item.thkaguyamod.' + iid, 'ModItems registry id')
    for bid in registered_blocks():
        want('block.thkaguyamod.' + bid, 'ModBlocks registry id')
    for key in ('itemGroup.thkaguyamod', 'itemGroup.thkaguyamod.spellcard'):
        want(key, 'creative tab')
    for key in dynamic_keys():
        want(key, 'runtime-assembled key')
    for key, where in literal_java_keys().items():
        if key not in VANILLA_KEYS:
            want(key, 'Component.translatable in ' + where)

    info = {
        'key_count': {lang: len(keys[lang]) for lang in LANGS},
        'union': len(set().union(*keys.values())),
        'entities': len(registered_entities()),
        'items': len(registered_items()),
        'blocks': len(registered_blocks()),
        'spellcards': len(registered_spellcard_ids()),
    }
    return failures, info


AUDIT_HEAD = ('| key | en_us | ja_jp | ko_kr | zh_cn | zh_tw | source |\n'
              '| --- | --- | --- | --- | --- | --- | --- |')


def audit_table():
    """Markdown table of every overlay key: per-language status and provenance."""
    lans = {lang: load(os.path.join(LANG_DIR, lang + '.json')) for lang in LANGS}
    over = overlay()
    rows = []
    for key in sorted(over[REFERENCE]):
        cells = []
        for lang in LANGS:
            if key not in lans[lang]:
                cells.append('MISSING')
            elif lans[lang][key] == over[lang][key]:
                cells.append('补写')
            else:
                cells.append('已存在')
        if key in COLOR_FIX.get('ja_jp', {}):
            source = '1.7.10 原文（原文键分隔符写成逗号，丢失；此处修复）'
        elif key.startswith('item.thSpellCard.'):
            source = '1.7.10 原文（符卡动态键别名，Java 按 ID 拼接）'
        elif key.startswith('entity.thkaguyamod.'):
            eid = key.rsplit('.', 1)[1]
            source = ('新写（1.7.10 无此实体名，直译）' if eid in ENTITY_NEW
                      else '1.7.10 无；取同语言物品名 `%s`' % ENTITY_VALUE_FROM[eid])
        elif key.startswith('entity.'):
            source = ('新写（1.7.10 无此实体名）' if key in LEGACY_NAME_NEW
                      else '1.7.10 实体名（旧键体系，Java 运行时拼接）')
        else:
            source = '新写（GUI 语义；1.7.10 亦缺）'
        rows.append('| `%s` | %s |' % (key, ' | '.join(cells + [source])))
    return AUDIT_HEAD + '\n' + '\n'.join(rows) + '\n'


def main():
    ap = argparse.ArgumentParser(description='THKaguyaMod lang audit')
    ap.add_argument('--apply', action='store_true', help='write the key overlay, then re-check')
    ap.add_argument('--audit', action='store_true', help='print the markdown audit table')
    args = ap.parse_args()

    if args.audit:
        sys.stdout.write(audit_table())
        return 0

    if args.apply:
        for lang, delta in apply_overlay():
            print('applied %+d key(s) to %s.json' % (delta, lang))

    failures, info = check()
    if info:
        print('keys per language: %s (union %d)' % (
            ', '.join('%s=%d' % (k, v) for k, v in info['key_count'].items()), info['union']))
        print('registries: %d entities, %d items, %d blocks, %d spell cards' % (
            info['entities'], info['items'], info['blocks'], info['spellcards']))
    if failures:
        print('\nFAIL (%d)' % len(failures))
        for msg in failures:
            print('  - ' + msg)
        return 1
    print('\nOK: 5 language files parse; key sets identical; registries and Java references covered.')
    return 0


if __name__ == '__main__':
    sys.exit(main())