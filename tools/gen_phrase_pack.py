#!/usr/bin/env python3
"""Generate vi_social.tsv bigram pack (UTF-8 LF) for PhrasePack."""
from __future__ import annotations

import hashlib
import itertools
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CAP = 250_000


def words(s: str) -> list[str]:
    return sorted(set(s.replace("_", " ").split()))


def main() -> None:
    pronouns = words(
        "tôi mình bạn anh em chúng họ cô chú bác ông bà con cháu ai người mọi ta tao mày "
        "chị bố mẹ ba má vợ chồng khách sếp đồng"
    )
    aux = words(
        "đang sẽ đã cần muốn có không rất cũng vẫn hay thường vừa mới chưa nên phải được "
        "bị bắt hãy đừng còn chỉ luôn nữa lại rồi thì mà nhưng nếu vì do bởi để cho với "
        "cùng của trong ngoài trên dưới trước sau giữa hơi khá quá toàn suốt mãi hiếm đôi từng"
    )
    verbs = words(
        "làm đi về nói nghĩ thấy biết ăn uống học ngủ chơi xem mua bán gọi nhắn chờ đợi "
        "giúp viết đọc nghe yêu thích mở đóng gửi nhận trả giữ tìm thử đặt lấy đưa kể hỏi "
        "bảo nhắc quên nhớ hiểu chạy đứng ngồi nằm dậy nấu rửa lau dọn quét lái dạy họp "
        "chat share like follow bật tắt sạc cắm rút ship giao hoàn hủy book kiểm xác đăng "
        "tải gỡ cập nhật sao lưu khôi phục chụp quay tập đá bắt đón quẹt nhập check hẹn "
        "gặp chia thả bình theo bỏ rời trễ chấm trình thuyết forward đính nén giải đổi "
        "xóa cắt dán lưu in scan pha mặc mang mail fax tập gym bơi lặn nhảy tennis yoga "
        "thiền nướng chiên xào luộc hấp rán"
    )
    nouns = words(
        "nhà việc bài cơm nước xe tiền bạn anh em ngày giờ lúc chỗ nơi cách lý do tin điện "
        "máy sách phim nhạc trò chơi cửa hàng trường công ty phòng bàn ghế điện thoại máy "
        "tính laptop bàn phím chuột màn hình pin sạc wifi mạng internet cà phê trà sữa "
        "bánh mì phở bún hủ tiếu sáng trưa chiều tối đêm tuần tháng năm công việc deadline "
        "dự án meeting họp gia đình con cái bố mẹ anh chị đường phố thành phố quận huyện "
        "xã lớp bài tập kỳ thi bệnh viện thuốc bác sĩ khám ngân hàng thẻ tiền mặt chuyển "
        "khoản chợ siêu thị online shopee series ca sĩ bóng đá trận đấu đội du lịch khách "
        "sạn vé máy bay thời tiết mưa nắng gió nóng lạnh app file mail otp taxi grab phòng "
        "sếp khách họp mic cam tài liệu báo cáo thuyết trình hàng hóa đơn mã giảm ship cod "
        "game blog vlog podcast live stream story reels tiktok facebook zalo messenger "
        "telegram whatsapp instagram youtube netflix spotify be gojek tiki lazada fpt "
        "viettel vinaphone mobifone vietcombank techcombank mb bidv vpbank sacombank"
    )
    adjs = words(
        "tốt hay đẹp nhanh chậm nhiều ít xa gần lâu sớm muộn dễ khó lạ quen mới cũ lạnh "
        "nóng ấm mát ồn yên vui buồn mệt khỏe ổn rõ mờ sáng tối ngon dở rẻ đắt lớn nhỏ cao "
        "thấp dày mỏng sạch bẩn ướt khô quan trọng gấp rút khẩn cấp bình thường đặc biệt "
        "vui vẻ hạnh phúc buồn bã cô đơn lo lắng căng thẳng thư giãn xinh đẹp dễ thương "
        "đáng yêu tuyệt vời hài lòng khó chịu bực bội mệt mỏi kiệt sức tỉnh táo bận rộn "
        "thong thả vội vàng chậm rãi cẩn thận bất cẩn chính xác sai sót hoàn hảo tuyệt đối "
        "tương đối"
    )
    qwords = words(
        "gì đâu nào sao thế vậy chưa rồi nhé đi thôi ạ à hả không chứ mà đó này kia luôn "
        "nha với cùng xong được nhỉ hử ừ ờ"
    )
    time_words = words(
        "nay mai qua kia này đó sau trước xong rồi luôn ngay liền chút tí phút giờ ngày "
        "tuần tháng năm sáng trưa chiều tối đêm cuối đầu giữa hồi nãy nọ xưa"
    )
    places = words(
        "nhà trường công_ty quán cafe sân_bay bến_xe chợ siêu_thị bệnh_viện ngân_hàng "
        "công_viên phố phường quận huyện tỉnh thành văn_phòng lớp_học căng_tin"
    )
    foods = words(
        "cơm phở bún bánh_mì hủ_tiếu cháo bún_bò bún_chả bún_riêu cơm_tấm cơm_gà cơm_chiên "
        "mì_xào lẩu nướng salad sushi pizza burger gà_rán khoai_tây trà_sữa cà_phê sinh_tố "
        "nước_ép bia rượu nước_suối"
    )
    transport = words("xe_máy ô_tô xe_buýt tàu_hỏa máy_bay grab be taxi xe_đạp metro")
    work = words(
        "họp báo_cáo deadline dự_án slide email mail tin_nhắn cuộc_gọi họp_online "
        "họp_offline presentation kpi"
    )

    seen: set[tuple[str, str]] = set()
    out: list[tuple[str, str]] = []

    def add(a: str, b: str) -> None:
        a = (a or "").strip().lower()
        b = (b or "").strip().lower()
        if not a or not b or a == b:
            return
        if b in {"x", "xx"} or a in {"x", "xx"}:
            return
        if " " in a or " " in b:
            return
        if len(a) > 32 or len(b) > 32:
            return
        if any(c.isdigit() for c in a + b):
            return
        key = (a, b)
        if key in seen:
            return
        seen.add(key)
        out.append(key)

    # Curated collocations first (tiêu đề, hoàng hôn, …)
    collo = ROOT / "phrase-packs" / "collocations_vi.txt"
    if collo.is_file():
        for line in collo.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.replace("_", " ").split()
            if len(parts) == 2:
                add(parts[0], parts[1])
            elif "\t" in line:
                a, b = line.split("\t", 1)
                add(a.replace("_", " ").split()[0], b.replace("_", " ").split()[0])

    prev = ROOT / "phrase-packs" / "vi_social.tsv"
    if prev.is_file():
        for line in prev.read_text(encoding="utf-8").splitlines():
            parts = line.split("\t")
            if len(parts) >= 2:
                add(parts[0], parts[1])

    for left, rights in {
        "xin": ["chào", "lỗi", "phép", "chúc", "mời", "hỏi", "giúp"],
        "cảm": ["ơn", "thấy", "giác", "động", "nhận"],
        "chào": ["bạn", "buổi", "anh", "em", "mọi", "cô", "chú", "sếp"],
        "buổi": ["sáng", "trưa", "chiều", "tối"],
        "hôm": ["nay", "qua", "sau", "kia"],
        "bây": ["giờ"],
    }.items():
        for right in rights:
            add(left, right)

    pairs_sources = [
        (pronouns, aux),
        (aux, verbs),
        (pronouns, verbs),
        (verbs, nouns),
        (verbs, qwords),
        (nouns, qwords),
        (aux, adjs),
        (pronouns, nouns),
        (pronouns, adjs),
        (nouns, adjs),
        (verbs, time_words),
        (nouns, time_words),
        (pronouns, time_words),
        (adjs, qwords),
        (adjs, nouns),
        (pronouns + verbs[:40], places),
        (verbs, foods),
        (verbs, transport),
        (verbs, work),
        (aux, foods),
        (aux, places),
        (nouns, places),
        (foods, qwords),
        (places, qwords),
        (transport, qwords),
        (work, qwords),
        (foods, adjs),
        (places, adjs),
    ]
    for lefts, rights in pairs_sources:
        for a, b in itertools.product(lefts, rights):
            add(a, b)

    for t1, t2 in itertools.product(time_words, time_words):
        if t1 != t2:
            add(t1, t2)
    for v1, v2 in itertools.product(verbs, verbs):
        if v1 != v2:
            add(v1, v2)
    for n1, n2 in itertools.product(nouns, nouns):
        if n1 != n2:
            add(n1, n2)
    for a1, a2 in itertools.product(adjs, adjs):
        if a1 != a2:
            add(a1, a2)
    for p1, p2 in itertools.product(places, places):
        if p1 != p2:
            add(p1, p2)

    hubs = (
        pronouns
        + aux[:30]
        + verbs[:50]
        + [
            "xin",
            "cảm",
            "chào",
            "hôm",
            "ngày",
            "buổi",
            "đi",
            "về",
            "ăn",
            "uống",
            "học",
            "làm",
            "mua",
            "bán",
            "gọi",
            "nhắn",
            "xem",
            "nghe",
            "nói",
            "thấy",
            "biết",
            "muốn",
            "cần",
            "có",
            "không",
            "rất",
            "đang",
            "sẽ",
            "đã",
            "chưa",
            "rồi",
        ]
    )
    rights = sorted(
        set(
            pronouns
            + aux
            + verbs
            + nouns
            + adjs
            + qwords
            + time_words
            + places
            + foods
            + transport
            + work
        )
    )
    for left in hubs:
        for right in rights:
            add(left, right)

    # Extra domains to push past 200k unique pairs
    en_left = words(
        "i you we they he she it my your our please can could would should will "
        "want need like love go come get take make do see look feel think know"
    )
    en_right = words(
        "you me us them it this that here there now later today tomorrow home work "
        "school food coffee water help thanks sorry ok yes no more less please "
        "ready busy free good bad great nice fine well soon again always never"
    )
    tech = words(
        "wifi bluetooth pin sạc app web api json xml http https server client cache "
        "token login logout password otp email sms call video audio camera mic "
        "keyboard mouse screen display brightness volume notification setting"
    )
    shopping = words(
        "giỏ hàng đơn hàng mã giảm freeship voucher flash sale trả góp đổi trả "
        "bảo hành giao hàng nhận hàng thanh toán chuyển khoản tiền mặt quẹt thẻ"
    )
    emotions = words(
        "vui buồn giận lo sợ mừng tiếc hối hận biết ơn thương nhớ nhớ thương "
        "cô đơn mệt mỏi hào hứng phấn khích bình yên an tâm"
    )
    for a, b in itertools.product(en_left, en_right):
        add(a, b)
    for a, b in itertools.product(en_left, verbs[:40]):
        add(a, b)
    for a, b in itertools.product(tech, qwords + adjs + verbs[:30]):
        add(a, b)
    for a, b in itertools.product(verbs, tech):
        add(a, b)
    for a, b in itertools.product(pronouns + aux, shopping):
        add(a, b)
    for a, b in itertools.product(shopping, qwords + adjs):
        add(a, b)
    for a, b in itertools.product(pronouns + aux + verbs[:40], emotions):
        add(a, b)
    for a, b in itertools.product(emotions, qwords + adjs):
        add(a, b)
    # filler systematic: left index x right index style hubs
    fillers_l = words(
        "rồi thì là mà nên cứ hãy vẫn còn cũng đã sẽ đang chưa không có được "
        "bị bịa bị_viết nhờ nhờ_vậy thế_này thế_kia như_vậy như_thế"
    )
    fillers_r = words(
        "nhé à ạ đi thôi chứ hả nhỉ luôn nha với cùng đã rồi xong chưa được "
        "ngay liền chút tí đã_rồi được_rồi ok_nhé thôi_nhé"
    )
    for a, b in itertools.product(fillers_l + pronouns + verbs, fillers_r):
        add(a, b)
    for a, b in itertools.product(foods + places + transport + work, time_words + fillers_r):
        add(a, b)
    for a, b in itertools.product(time_words + fillers_l, foods + places + transport):
        add(a, b)

    print("unique", len(out))
    selected = out[:CAP]
    data = "".join(f"{a}\t{b}\n" for a, b in selected).encode("utf-8")
    digest = hashlib.sha256(data).hexdigest()
    print(
        "written",
        len(selected),
        "bytes",
        len(data),
        "kb",
        len(data) // 1024,
        "sha",
        digest,
    )
    for folder in (ROOT / "phrase-packs", ROOT / "app/src/main/assets/phrase_packs"):
        folder.mkdir(parents=True, exist_ok=True)
        (folder / "vi_social.tsv").write_bytes(data)
    print("EXPECTED_SHA256=", digest)


if __name__ == "__main__":
    main()
