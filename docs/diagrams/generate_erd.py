"""Generates the CampusNest ER diagrams as SVG files next to this script.

  erd-chen-a.svg / erd-chen-b.svg   conceptual ERD (Chen notation), two subject areas
  erd-attributes.svg                attribute notation applied to CampusNest
  erd-logical-a.svg / -b.svg        logical ERD (Crow's Foot notation, PK/FK, 0/1/N)

Run:  python docs/diagrams/generate_erd.py
The tables and columns mirror the Flyway migrations V1-V7; update both together.
"""
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
TEAL = "#0f7f78"
INK = "#13232f"
FK_BLUE = "#2f5d9c"
UK_PURPLE = "#7a4fa3"

STYLE = """
text{font-family:Arial,Helvetica,sans-serif;fill:#13232f}
.h1{font-size:22px;font-weight:bold}.h2{font-size:13px;fill:#555}
.en{font-size:13px;font-weight:bold}.rel{font-size:11px;font-weight:bold}
.role{font-size:10px;fill:#555;font-style:italic}.at{font-size:11.5px}
.card{font-size:13px;font-weight:bold;fill:#0f7f78}
.tn{font-size:13px;font-weight:bold;fill:#ffffff}.col{font-size:11.5px}
.tag{font-size:9.5px;font-weight:bold}.lbl{font-size:11px;font-style:italic;fill:#2b3a44}
.note{font-size:11.5px;fill:#2b3a44}.nb{font-size:12px;font-weight:bold;fill:#13232f}
.cap{font-size:12px;fill:#555;font-style:italic}
"""


def esc(s):
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


class Svg:
    def __init__(self, w, h, title, subtitle):
        self.w, self.h, self.title, self.subtitle = w, h, title, subtitle
        self.back, self.mid, self.front = [], [], []

    def text(self, x, y, s, cls, anchor="start", layer="front"):
        getattr(self, layer).append(f'<text x="{x:.1f}" y="{y:.1f}" class="{cls}" text-anchor="{anchor}">{esc(s)}</text>')

    def label_box(self, x, y, s, cls="lbl"):
        w = len(s) * 5.6 + 8
        self.front.append(f'<rect x="{x - w / 2:.1f}" y="{y - 11:.1f}" width="{w:.1f}" height="15" fill="#ffffff" opacity="0.92"/>')
        self.text(x, y, s, cls, "middle")

    def note(self, x, y, w, title, lines):
        h = 30 + 16 * len(lines)
        self.mid.append(f'<path d="M{x},{y} H{x + w - 14} L{x + w},{y + 14} V{y + h} H{x} Z" fill="#fffef2" stroke="#999" stroke-width="1"/>'
                        f'<path d="M{x + w - 14},{y} V{y + 14} H{x + w}" fill="none" stroke="#999" stroke-width="1"/>')
        self.text(x + 12, y + 20, title, "nb")
        for i, line in enumerate(lines):
            self.text(x + 12, y + 40 + 16 * i, line, "note")
        return y + h

    def save(self, name):
        out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}">',
               f'<title>{esc(self.title)}</title><style>{STYLE}</style>',
               f'<rect width="{self.w}" height="{self.h}" fill="#ffffff"/>',
               f'<text x="{self.w / 2}" y="34" class="h1" text-anchor="middle">{esc(self.title)}</text>',
               f'<text x="{self.w / 2}" y="56" class="h2" text-anchor="middle">{esc(self.subtitle)}</text>']
        out += self.back + self.mid + self.front + ["</svg>"]
        with open(os.path.join(HERE, name), "w", encoding="utf-8") as f:
            f.write("\n".join(out))


# ---------------------------------------------------------------- Chen notation

def rect_exit(cx, cy, w, h, tx, ty):
    """Point where the segment from the box centre towards (tx, ty) leaves the box, plus the unit direction."""
    dx, dy = tx - cx, ty - cy
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    t = min((w / 2) / abs(ux) if ux else 1e9, (h / 2) / abs(uy) if uy else 1e9)
    return cx + ux * t, cy + uy * t, ux, uy


class Chen(Svg):
    def __init__(self, *a):
        super().__init__(*a)
        self.shapes = {}

    def entity(self, key, cx, cy, name, w=150, h=46, weak=False):
        self.shapes[key] = (cx, cy, w, h)
        s = f'<rect x="{cx - w / 2}" y="{cy - h / 2}" width="{w}" height="{h}" fill="#ffffff" stroke="{TEAL}" stroke-width="1.8"/>'
        if weak:
            s += f'<rect x="{cx - w / 2 + 5}" y="{cy - h / 2 + 5}" width="{w - 10}" height="{h - 10}" fill="none" stroke="{TEAL}" stroke-width="1.3"/>'
        self.mid.append(s)
        self.text(cx, cy + 4.5, name, "en", "middle", "mid")

    def diamond(self, key, cx, cy, name, sub=None, ident=False, w=124, h=58):
        self.shapes[key] = (cx, cy, w, h)
        pts = f"{cx},{cy - h / 2} {cx + w / 2},{cy} {cx},{cy + h / 2} {cx - w / 2},{cy}"
        s = f'<polygon points="{pts}" fill="#ffffff" stroke="{TEAL}" stroke-width="1.8"/>'
        if ident:
            iw, ih = w - 16, h - 11
            s += f'<polygon points="{cx},{cy - ih / 2} {cx + iw / 2},{cy} {cx},{cy + ih / 2} {cx - iw / 2},{cy}" fill="none" stroke="{TEAL}" stroke-width="1.3"/>'
        self.mid.append(s)
        if sub:
            self.text(cx, cy, name, "rel", "middle", "mid")
            self.text(cx, cy + 12, sub, "role", "middle", "mid")
        else:
            self.text(cx, cy + 4, name, "rel", "middle", "mid")

    def oval(self, cx, cy, label, kind="simple", rx=None, ry=15):
        rx = rx or max(36, len(label) * 3.5 + 14)
        dash = ' stroke-dasharray="5 4"' if kind == "derived" else ""
        s = f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#ffffff" stroke="{TEAL}" stroke-width="1.5"{dash}/>'
        if kind == "multi":
            s += f'<ellipse cx="{cx}" cy="{cy}" rx="{rx - 4}" ry="{ry - 4}" fill="none" stroke="{TEAL}" stroke-width="1.2"/>'
        self.mid.append(s)
        deco = ' text-decoration="underline"' if kind == "key" else ""
        self.mid.append(f'<text x="{cx}" y="{cy + 4}" class="at" text-anchor="middle"{deco}>{esc(label)}</text>')

    def line(self, p, q):
        self.back.append(f'<line x1="{p[0]}" y1="{p[1]}" x2="{q[0]}" y2="{q[1]}" stroke="{INK}" stroke-width="1.3"/>')

    def connect(self, ent, other, card, side=1):
        """Line from an entity to a diamond (or oval centre) with the cardinality written near the entity."""
        ex, ey, ew, eh = self.shapes[ent]
        ox, oy = self.shapes[other][:2] if isinstance(other, str) else other
        self.line((ex, ey), (ox, oy))
        if card:
            # Write the cardinality part-way along the line (not at the entity's corner, where lines crowd).
            bx, by, ux, uy = rect_exit(ex, ey, ew, eh, ox, oy)
            if isinstance(other, str):
                _, _, dw, dh = self.shapes[other]
                t = 1 / (abs(ux) / (dw / 2) + abs(uy) / (dh / 2))
                tx, ty = ox - ux * t, oy - uy * t
            else:
                tx, ty = ox, oy
            f = 0.4 if math.hypot(tx - bx, ty - by) > 70 else 0.5
            mx, my = bx + (tx - bx) * f, by + (ty - by) * f
            lx, ly = mx - uy * 11 * side, my + ux * 11 * side
            self.text(lx, ly + 4.5, card, "card", "middle")

    def link(self, a, b):
        self.line(self.shapes[a][:2], self.shapes[b][:2])


def chen_a():
    c = Chen(1500, 950, "CampusNest — Conceptual ERD (Chen) · Part A",
             "Listings and student engagement · 1 / N / M on each line = maximum cardinality (minimums are in the business rules)")
    c.entity("USER", 140, 520, "USER", 130)
    c.entity("LISTING", 800, 520, "LISTING", 140)
    c.entity("MESSAGE", 470, 140, "MESSAGE")
    c.entity("ENQUIRY", 470, 300, "ENQUIRY")
    c.entity("REVIEW", 470, 700, "REVIEW")
    c.entity("STATUS", 470, 860, "STATUS_CHANGE", 170)
    c.entity("ROOM", 1190, 290, "ROOM_OPTION", 160)
    c.entity("CHARGE", 1190, 440, "CHARGE")
    c.entity("IMAGE", 1190, 600, "LISTING_IMAGE", 170)
    c.entity("AMENITY", 1190, 780, "AMENITY")

    c.diamond("CONTAINS", 470, 220, "CONTAINS")
    c.diamond("SENDS", 250, 140, "SENDS")
    c.diamond("STARTS", 290, 385, "STARTS", "(student)")
    c.diamond("ABOUT", 650, 385, "IS ABOUT")
    c.diamond("SAVES", 470, 440, "SAVES", "(student)")
    c.oval(470, 382, "saved_at", rx=40, ry=13)
    c.diamond("OWNS", 470, 540, "OWNS", "(owner)")
    c.diamond("WRITES", 290, 650, "WRITES", "(student)")
    c.diamond("RECEIVES", 650, 650, "RECEIVES")
    c.diamond("MAKES", 290, 810, "MAKES", "(owner / admin)")
    c.diamond("LOGS", 650, 810, "LOGS")
    c.diamond("OFFERS", 1000, 290, "OFFERS")
    c.diamond("LEVIES", 1000, 440, "LEVIES")
    c.diamond("SHOWS", 1000, 600, "SHOWS")
    c.diamond("PROVIDES", 1000, 780, "PROVIDES")
    c.oval(1000, 860, "available", rx=42, ry=13)
    c.line((1000, 809), (1000, 847))
    c.line((470, 395), (470, 411))

    c.connect("MESSAGE", "CONTAINS", "N")
    c.connect("ENQUIRY", "CONTAINS", "1")
    c.connect("USER", "SENDS", "1")
    c.connect("MESSAGE", "SENDS", "N")
    c.connect("USER", "STARTS", "1")
    c.connect("ENQUIRY", "STARTS", "N")
    c.connect("ENQUIRY", "ABOUT", "N")
    c.connect("LISTING", "ABOUT", "1", -1)
    c.connect("USER", "SAVES", "M")
    c.connect("LISTING", "SAVES", "N", -1)
    c.connect("USER", "OWNS", "1", -1)
    c.connect("LISTING", "OWNS", "N")
    c.connect("USER", "WRITES", "1", -1)
    c.connect("REVIEW", "WRITES", "N")
    c.connect("REVIEW", "RECEIVES", "N", -1)
    c.connect("LISTING", "RECEIVES", "1")
    c.connect("USER", "MAKES", "1", -1)
    c.connect("STATUS", "MAKES", "N")
    c.connect("STATUS", "LOGS", "N", -1)
    c.connect("LISTING", "LOGS", "1")
    for d, e in (("OFFERS", "ROOM"), ("LEVIES", "CHARGE"), ("SHOWS", "IMAGE"), ("PROVIDES", "AMENITY")):
        c.connect("LISTING", d, "M" if d == "PROVIDES" else "1", -1 if d in ("OFFERS", "LEVIES") else 1)
        c.connect(e, d, "N")

    c.note(1300, 90, 185, "Legend", [""] * 12)
    legend = [
        (f'<rect x="1312" y="122" width="56" height="22" fill="#fff" stroke="{TEAL}" stroke-width="1.6"/>', "entity"),
        (f'<rect x="1312" y="150" width="56" height="22" fill="#fff" stroke="{TEAL}" stroke-width="1.6"/><rect x="1316" y="154" width="48" height="14" fill="none" stroke="{TEAL}"/>', "weak entity"),
        (f'<polygon points="1340,180 1368,192 1340,204 1312,192" fill="#fff" stroke="{TEAL}" stroke-width="1.6"/>', "relationship"),
        (f'<ellipse cx="1340" cy="222" rx="27" ry="10" fill="#fff" stroke="{TEAL}" stroke-width="1.4"/>', "attribute"),
    ]
    for i, (shape, txt) in enumerate(legend):
        c.front.append(shape)
        c.text(1380, 137 + 28 * i + (1 if i == 2 else 0), txt, "note")
    c.text(1312, 262, "1 = one, N / M = many", "note")
    c.text(1312, 280, "(role) = which kind of USER", "note")
    c.note(1300, 330, 185, "Not shown here", ["Accounts, profiles,", "trust and roommates:", "see Part B.",
                                               "USER is the same", "entity in both parts."])
    c.save("erd-chen-a.svg")


def chen_b():
    c = Chen(1500, 950, "CampusNest — Conceptual ERD (Chen) · Part B",
             "Accounts, profiles, trust and roommates · double rectangle = weak entity, double diamond = identifying relationship")
    c.entity("COLLEGE", 110, 300, "COLLEGE", 130)
    c.entity("SP", 330, 140, "STUDENT_PROFILE", 170, weak=True)
    c.entity("PREF", 590, 140, "STUDENT_PREFERENCES", 200, weak=True)
    c.entity("RMP", 860, 140, "ROOMMATE_PROFILE", 190, weak=True)
    c.entity("OP", 1130, 140, "OWNER_PROFILE", 170, weak=True)
    c.entity("USER", 700, 500, "USER", 170, 50)
    c.entity("RR", 1270, 500, "ROOMMATE_REQUEST", 200)
    c.entity("VR", 430, 860, "VERIFICATION_RECORD", 210)
    c.entity("REPORT", 990, 860, "REPORT")

    c.diamond("STUDIES", 110, 140, "STUDIES AT", w=118)
    c.diamond("HAS1", 330, 300, "HAS", ident=True)
    c.diamond("SETS", 590, 300, "SETS", ident=True)
    c.diamond("CREATES", 860, 300, "CREATES", ident=True)
    c.diamond("HAS2", 1130, 300, "HAS", ident=True)
    c.diamond("BLOCKS", 380, 500, "BLOCKS", "(student → student)", w=150)
    c.diamond("SENDS", 1000, 420, "SENDS")
    c.diamond("RECEIVES", 1000, 580, "RECEIVES")
    c.diamond("UNDERGOES", 470, 680, "UNDERGOES", "(owner)", w=132)
    c.diamond("DECIDES", 660, 720, "DECIDES", "(admin)")
    c.diamond("FILES", 880, 690, "FILES")
    c.diamond("RESOLVES", 1080, 690, "RESOLVES", "(admin)")

    c.connect("SP", "STUDIES", "N")
    c.connect("COLLEGE", "STUDIES", "1")
    for d, e in (("HAS1", "SP"), ("SETS", "PREF"), ("CREATES", "RMP"), ("HAS2", "OP")):
        c.connect(e, d, "1")
        c.connect("USER", d, "1", -1 if d in ("HAS1", "SETS") else 1)
    # unary M:N: two lines from USER to BLOCKS
    c.line((615, 488), (455, 500))
    c.line((615, 512), (455, 500))
    c.text(596, 478, "M", "card", "middle")
    c.text(596, 534, "N", "card", "middle")
    c.text(540, 470, "blocker", "role", "middle")
    c.text(540, 542, "blocked", "role", "middle")
    c.connect("USER", "SENDS", "1")
    c.connect("RR", "SENDS", "N", -1)
    c.connect("USER", "RECEIVES", "1", -1)
    c.connect("RR", "RECEIVES", "N")
    c.connect("USER", "UNDERGOES", "1")
    c.connect("VR", "UNDERGOES", "N", -1)
    c.connect("USER", "DECIDES", "1", -1)
    c.connect("VR", "DECIDES", "N")
    c.connect("USER", "FILES", "1")
    c.connect("REPORT", "FILES", "N")
    c.connect("USER", "RESOLVES", "1", -1)
    c.connect("REPORT", "RESOLVES", "N", -1)
    c.text(990, 906, "concerns exactly one LISTING,", "cap", "middle")
    c.text(990, 922, "REVIEW or USER (target)", "cap", "middle")

    c.mid.append('<rect x="30" y="660" width="190" height="250" rx="6" fill="none" stroke="#8a8a8a" stroke-dasharray="6 4"/>')
    c.text(42, 682, "Reference data", "nb")
    c.text(42, 698, "no relationships; read by", "note")
    c.text(42, 714, "the commute estimator", "note")
    c.entity("STATION", 125, 770, "STATION", 140)
    c.entity("FARE", 125, 860, "FARE_RULE", 140)

    c.note(1230, 690, 255, "Reading Part B", ["Profiles cannot exist without", "their USER and share its key",
                                               "(1 : 0..1), so they are drawn as", "weak entities.",
                                               "ROOMMATE_REQUEST, REPORT and", "VERIFICATION_RECORD keep their",
                                               "own history (date, status, notes)."])
    c.save("erd-chen-b.svg")


def chen_attributes():
    c = Chen(1500, 620, "CampusNest — Attribute types (Chen notation)",
             "Key (underlined), simple, composite, derived (dashed) and multivalued (double) attributes, using real CampusNest data")
    c.entity("LISTING", 400, 330, "LISTING", 150)
    for (x, y, t, k) in ((170, 230, "listing_id", "key"), (170, 330, "title", "simple"), (170, 430, "status", "simple"),
                         (200, 530, "average_rating", "derived"), (390, 540, "lowest_rent", "derived")):
        c.oval(x, y, t, k)
        c.line((400, 330), (x, y))
    c.oval(600, 210, "address", "simple")
    c.line((400, 330), (600, 210))
    for (x, t) in ((470, "address_line"), (590, "locality"), (690, "city"), (790, "pincode")):
        c.oval(x, 112, t, "simple")
        c.line((600, 210), (x, 112))
    c.oval(620, 440, "location", "simple")
    c.line((400, 330), (620, 440))
    for (x, t) in ((560, "latitude"), (700, "longitude")):
        c.oval(x, 530, t, "simple")
        c.line((620, 440), (x, 530))
    c.text(420, 596, "LISTING: composite address and location; average_rating and lowest_rent are derived (calculated, never stored)", "cap", "middle")

    c.entity("USER", 1130, 110, "USER", 140)
    c.diamond("SETS", 1130, 200, "SETS", ident=True)
    c.entity("PREF", 1130, 300, "STUDENT_PREFERENCES", 210, weak=True)
    c.link("USER", "SETS")
    c.link("SETS", "PREF")
    c.text(1150, 152, "1", "card")
    c.text(1150, 262, "1", "card")
    for (x, y, t, k) in ((930, 380, "budget_max", "simple"), (960, 470, "max_commute_minutes", "simple"),
                         (1130, 420, "room_choices", "multi"), (1310, 470, "must_have_amenities", "multi"),
                         (1340, 380, "nice_to_have_amenities", "multi"), (1330, 240, "commute_mode", "simple")):
        c.oval(x, y, t, k)
        c.line((1130, 300), (x, y))
    c.text(1130, 560, "STUDENT_PREFERENCES: weak entity identified through USER (shares user_id);", "cap", "middle")
    c.text(1130, 576, "room types and amenities are multivalued (stored as ordered code lists, see design notes)", "cap", "middle")
    c.save("erd-attributes.svg")


# ---------------------------------------------------------------- Crow's Foot notation

ROW, HEAD, STUB = 18, 30, 34


class Crow(Svg):
    def __init__(self, *a):
        super().__init__(*a)
        self.tables, self.rels = {}, []

    def table(self, key, x, y, rows, w=230):
        h = HEAD + ROW * len(rows)
        self.tables[key] = dict(x=x, y=y, w=w, h=h, rows=[r[1] for r in rows])
        self.mid.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="6" fill="#ffffff" stroke="#9fb9bd" stroke-width="1.2"/>'
                        f'<path d="M{x},{y + HEAD} V{y + 6} Q{x},{y} {x + 6},{y} H{x + w - 6} Q{x + w},{y} {x + w},{y + 6} V{y + HEAD} Z" fill="{TEAL}"/>')
        self.text(x + 12, y + 20, key, "tn", layer="mid")
        for i, (tag, col) in enumerate(rows):
            ry = y + HEAD + ROW * i
            if i:
                self.mid.append(f'<line x1="{x + 8}" y1="{ry}" x2="{x + w - 8}" y2="{ry}" stroke="#e3ecee"/>')
            colour = TEAL if tag.startswith("PK") else FK_BLUE if tag.startswith("FK") else UK_PURPLE
            if tag:
                self.mid.append(f'<text x="{x + 10}" y="{ry + 13}" class="tag" fill="{colour}" style="fill:{colour}">{tag}</text>')
            bold = ' font-weight="bold"' if tag.startswith("PK") else ""
            self.mid.append(f'<text x="{x + 58}" y="{ry + 13}" class="col"{bold}>{esc(col)}</text>')

    def row_y(self, key, col):
        t = self.tables[key]
        return t["y"] + HEAD + ROW * t["rows"].index(col) + ROW / 2

    def rel(self, parent, child, col, pcard, ccard, label, **kw):
        self.rels.append(dict(parent=parent, child=child, col=col, pcard=pcard, ccard=ccard, label=label, **kw))

    def glyph(self, e, d, kind):
        ex, ey = e
        dx, dy = d
        px, py = -dy, dx

        def at(k):
            return ex - dx * k, ey - dy * k

        def bar(k):
            bx, by = at(k)
            self.front.append(f'<line x1="{bx + px * 8:.1f}" y1="{by + py * 8:.1f}" x2="{bx - px * 8:.1f}" y2="{by - py * 8:.1f}" stroke="{TEAL}" stroke-width="1.7"/>')

        def circle(k):
            cx, cy = at(k)
            self.front.append(f'<circle cx="{cx:.1f}" cy="{cy:.1f}" r="5" fill="#ffffff" stroke="{TEAL}" stroke-width="1.6"/>')

        def crow():
            ax, ay = at(14)
            for s in (1, -1):
                self.front.append(f'<line x1="{ax:.1f}" y1="{ay:.1f}" x2="{ex + px * 8 * s:.1f}" y2="{ey + py * 8 * s:.1f}" stroke="{TEAL}" stroke-width="1.7"/>')

        if kind == "one":
            bar(8), bar(15)
        elif kind == "zero_one":
            bar(9), circle(21)
        elif kind == "one_many":
            crow(), bar(21)
        elif kind == "zero_many":
            crow(), circle(24)

    def finish(self):
        out_dir = {"left": (-1, 0), "right": (1, 0), "top": (0, -1), "bottom": (0, 1)}
        groups = {}
        for r in self.rels:
            if "path" in r:
                continue
            p, c = self.tables[r["parent"]], self.tables[r["child"]]
            if c["x"] >= p["x"] + p["w"]:
                r["pside"], r["cside"] = "right", "left"
            elif c["x"] + c["w"] <= p["x"]:
                r["pside"], r["cside"] = "left", "right"
            elif c["y"] > p["y"]:
                r["pside"], r["cside"] = "bottom", "top"
            else:
                r["pside"], r["cside"] = "top", "bottom"
            if r["cside"] in ("left", "right"):
                cx = c["x"] if r["cside"] == "left" else c["x"] + c["w"]
                r["cpt"] = (cx, self.row_y(r["child"], r["col"]))
            else:
                cy = c["y"] if r["cside"] == "top" else c["y"] + c["h"]
                r["cpt"] = (r.get("cx", c["x"] + c["w"] / 2), cy)
            groups.setdefault((r["parent"], r["pside"]), []).append(r)
        for (pk, side), rs in groups.items():
            p = self.tables[pk]
            if side in ("left", "right"):
                rs.sort(key=lambda r: r["cpt"][1])
                x = p["x"] if side == "left" else p["x"] + p["w"]
                for k, r in enumerate(rs):
                    r["ppt"] = (x, p["y"] + HEAD + (k + 0.5) * (p["h"] - HEAD) / len(rs))
            else:
                y = p["y"] if side == "top" else p["y"] + p["h"]
                for r in rs:
                    r["ppt"] = (r["px"], y)
        for r in self.rels:
            if "path" in r:
                pts = r["path"]
            else:
                p0, p3 = r["ppt"], r["cpt"]
                op, oc = out_dir[r["pside"]], out_dir[r["cside"]]
                pts = [p0, (p0[0] + op[0] * STUB, p0[1] + op[1] * STUB), (p3[0] + oc[0] * STUB, p3[1] + oc[1] * STUB), p3]
            self.back.append('<polyline points="' + " ".join(f"{x:.1f},{y:.1f}" for x, y in pts)
                             + f'" fill="none" stroke="{INK}" stroke-width="1.3"/>')

            def into(a, b):
                d = math.hypot(b[0] - a[0], b[1] - a[1])
                return ((b[0] - a[0]) / d, (b[1] - a[1]) / d)

            self.glyph(pts[0], into(pts[1], pts[0]), r["pcard"])
            self.glyph(pts[-1], into(pts[-2], pts[-1]), r["ccard"])
            lx, ly = r.get("lpos", ((pts[1][0] + pts[2][0]) / 2, (pts[1][1] + pts[2][1]) / 2 + 4))
            self.label_box(lx, ly, r["label"])

    def legend(self, x, y):
        self.mid.append(f'<rect x="{x}" y="{y}" width="240" height="196" rx="6" fill="#ffffff" stroke="#b9c9cf"/>')
        self.text(x + 12, y + 22, "Legend (Crow's Foot)", "nb")
        for i, (kind, txt) in enumerate((("one", "exactly one"), ("zero_one", "zero or one"),
                                          ("one_many", "one or many"), ("zero_many", "zero or many"))):
            yy = y + 46 + 26 * i
            self.mid.append(f'<line x1="{x + 14}" y1="{yy}" x2="{x + 70}" y2="{yy}" stroke="{INK}" stroke-width="1.3"/>'
                            f'<rect x="{x + 70}" y="{yy - 9}" width="10" height="18" fill="#eef5f5" stroke="{TEAL}"/>')
            self.glyph((x + 70, yy), (1, 0), kind)
            self.text(x + 92, yy + 4, txt, "note")
        self.text(x + 12, y + 164, "PK primary key · FK foreign key", "note")
        self.text(x + 12, y + 182, "UK part of a unique constraint", "note")


def logical_a():
    c = Crow(1760, 1010, "CampusNest — Logical ERD (Crow's Foot) · Part A",
             "Listings and student engagement · all columns, PK / FK / UK marked · symbol nearest a table = how many of THAT table")
    c.table("users", 40, 330, [("PK", "id"), ("UK", "email"), ("", "password_hash"), ("", "role"), ("", "status"),
                               ("", "status_reason"), ("", "display_name"), ("", "phone"), ("", "created_at"), ("", "last_login_at")], 220)
    c.table("saved_listings", 400, 110, [("PK", "id"), ("FK UK", "student_id"), ("FK UK", "listing_id"), ("", "created_at")], 250)
    c.table("enquiries", 400, 230, [("PK", "id"), ("FK UK", "listing_id"), ("FK UK", "student_id"), ("", "student_shares_contact"),
                                    ("", "owner_shares_contact"), ("", "created_at"), ("", "last_message_at")], 250)
    c.table("enquiry_messages", 400, 440, [("PK", "id"), ("FK", "enquiry_id"), ("FK", "sender_id"), ("", "body"), ("", "created_at")], 250)
    c.table("reviews", 400, 600, [("PK", "id"), ("FK UK", "listing_id"), ("FK UK", "student_id"), ("", "rating"), ("", "comment"),
                                  ("", "status"), ("", "hidden_reason"), ("", "created_at"), ("", "updated_at")], 250)
    c.table("listing_status_history", 400, 830, [("PK", "id"), ("FK", "listing_id"), ("", "from_status"), ("", "to_status"),
                                                 ("FK", "actor_id"), ("", "reason"), ("", "created_at")], 250)
    c.table("listings", 790, 110, [("PK", "id"), ("FK", "owner_id"), ("", "title"), ("", "description"), ("", "property_type"),
                                   ("", "gender_policy"), ("", "address_line"), ("", "locality"), ("", "city"), ("", "pincode"),
                                   ("", "latitude"), ("", "longitude"), ("", "status"), ("", "status_reason"), ("", "submitted_at"),
                                   ("", "published_at"), ("", "last_confirmed_at"), ("", "is_sample_data"), ("", "created_at"),
                                   ("", "updated_at"), ("", "version")], 240)
    c.table("room_options", 1170, 110, [("PK", "id"), ("FK", "listing_id"), ("", "room_type"), ("", "sharing_capacity"),
                                        ("", "monthly_rent"), ("", "security_deposit"), ("", "beds_total"), ("", "beds_available"),
                                        ("", "availability_updated_at")], 250)
    c.table("listing_charges", 1170, 330, [("PK", "id"), ("FK", "listing_id"), ("", "charge_type"), ("", "description"),
                                           ("", "amount"), ("", "frequency"), ("", "mandatory")], 250)
    c.table("listing_images", 1170, 520, [("PK", "id"), ("FK", "listing_id"), ("UK", "storage_key"), ("", "content_type"),
                                          ("", "size_bytes"), ("", "width"), ("", "height"), ("", "uploaded_at")], 250)
    c.table("listing_amenities", 1170, 740, [("PK", "id"), ("FK UK", "listing_id"), ("FK UK", "amenity_id"), ("", "available")], 250)
    c.table("amenities", 1530, 740, [("PK", "id"), ("UK", "code"), ("", "name"), ("", "is_core"), ("", "sort_order")], 210)

    c.rel("users", "listings", "owner_id", "one", "zero_many", "owns",
          path=[(150, 330), (150, 84), (910, 84), (910, 110)], lpos=(530, 88))
    for child, col, label in (("saved_listings", "student_id", "saves"), ("enquiries", "student_id", "starts"),
                              ("enquiry_messages", "sender_id", "sends"), ("reviews", "student_id", "writes")):
        c.rel("users", child, col, "one", "zero_many", label)
    c.rel("users", "listing_status_history", "actor_id", "zero_one", "zero_many", "changes")
    for child, label in (("saved_listings", "is saved in"), ("enquiries", "receives"), ("reviews", "is reviewed in"),
                         ("listing_status_history", "logs")):
        c.rel("listings", child, "listing_id", "one", "zero_many", label)
    c.rel("enquiries", "enquiry_messages", "enquiry_id", "one", "one_many", "contains", cx=525, px=525, lpos=(568, 418))
    for child, label in (("room_options", "offers"), ("listing_charges", "levies"), ("listing_images", "shows"),
                         ("listing_amenities", "declares")):
        c.rel("listings", child, "listing_id", "one", "zero_many", label)
    c.rel("amenities", "listing_amenities", "amenity_id", "one", "zero_many", "is declared in")
    c.finish()
    c.legend(1500, 110)
    c.note(1500, 330, 240, "Rules the diagram relies on", [
        "• An enquiry is always created", "  with its first message (1..N).",
        "• A listing needs ≥ 1 room option", "  before it can be submitted",
        "  (checked by the application).", "• UK on two columns = the pair is",
        "  unique, e.g. one review per", "  (listing, student).",
        "• listing_amenities and", "  saved_listings resolve M:N.",
        "• users plays owner, student or", "  admin (column role)."])
    c.save("erd-logical-a.svg")


def logical_b():
    c = Crow(1760, 1010, "CampusNest — Logical ERD (Crow's Foot) · Part B",
             "Accounts, profiles, trust, roommates and reference data · all columns, PK / FK / UK marked")
    c.table("colleges", 40, 110, [("PK", "id"), ("", "name"), ("UK", "short_name"), ("", "address"), ("UK", "city"),
                                  ("", "latitude"), ("", "longitude")], 210)
    c.table("stations", 40, 560, [("PK", "id"), ("", "name"), ("", "line"), ("", "latitude"), ("", "longitude"), ("UK", "osm_node_id")], 210)
    c.table("fare_rules", 40, 730, [("PK", "id"), ("", "mode"), ("", "kind"), ("", "up_to_km"), ("", "base_fare"), ("", "base_km"),
                                    ("", "per_km"), ("", "description"), ("", "source_name"), ("", "source_url"),
                                    ("", "effective_from"), ("", "created_at")], 210)
    c.table("student_profiles", 370, 110, [("PK FK", "user_id"), ("FK", "college_id"), ("", "course"), ("", "year_of_study"), ("", "gender")], 250)
    c.table("student_preferences", 370, 270, [("PK FK", "user_id"), ("", "budget_max"), ("", "max_commute_minutes"), ("", "commute_mode"),
                                              ("", "room_choices"), ("", "must_have_amenities"), ("", "nice_to_have_amenities"), ("", "updated_at")], 250)
    c.table("roommate_profiles", 370, 480, [("PK FK", "user_id"), ("", "discoverable"), ("", "budget_min"), ("", "budget_max"),
                                            ("", "preferred_localities"), ("", "anywhere_near_college"), ("", "sleep_schedule"),
                                            ("", "smoking"), ("", "cannot_live_with_smoker"), ("", "study_environment"),
                                            ("", "cleanliness"), ("", "guests"), ("", "food_preference"), ("", "sharing_size"),
                                            ("", "same_gender_only"), ("", "about"), ("", "updated_at")], 250)
    c.table("roommate_blocks", 370, 850, [("PK", "id"), ("FK UK", "blocker_id"), ("FK UK", "blocked_id"), ("", "created_at")], 250)
    c.table("users", 760, 250, [("PK", "id"), ("UK", "email"), ("", "password_hash"), ("", "role"), ("", "status"),
                                ("", "status_reason"), ("", "display_name"), ("", "phone"), ("", "created_at"), ("", "last_login_at")], 220)
    c.table("roommate_requests", 760, 620, [("PK", "id"), ("FK", "from_user_id"), ("FK", "to_user_id"), ("", "status"),
                                            ("", "message"), ("", "created_at"), ("", "responded_at")], 220)
    c.table("owner_profiles", 1120, 110, [("PK FK", "user_id"), ("", "owner_type"), ("", "business_name"), ("", "verification_status"),
                                          ("", "verified_at"), ("FK", "verified_by"), ("", "verification_requested_at")], 260)
    c.table("verification_records", 1120, 310, [("PK", "id"), ("FK", "owner_id"), ("", "decision"), ("", "method"), ("", "notes"),
                                                ("FK", "reviewer_id"), ("", "created_at")], 260)
    c.table("reports", 1120, 510, [("PK", "id"), ("FK", "reporter_id"), ("", "target_type"), ("", "target_id"), ("", "reason"),
                                   ("", "details"), ("", "status"), ("FK", "resolved_by"), ("", "resolution_note"),
                                   ("", "created_at"), ("", "resolved_at")], 260)
    c.text(40, 548, "Reference data — no foreign keys", "nb")

    c.rel("colleges", "student_profiles", "college_id", "zero_one", "zero_many", "enrols")
    c.rel("users", "student_profiles", "user_id", "one", "zero_one", "has")
    c.rel("users", "student_preferences", "user_id", "one", "zero_one", "sets")
    c.rel("users", "roommate_profiles", "user_id", "one", "zero_one", "creates")
    c.rel("users", "roommate_blocks", "blocker_id", "one", "zero_many", "blocks")
    c.rel("users", "roommate_blocks", "blocked_id", "one", "zero_many", "is blocked in")
    c.rel("users", "roommate_requests", "from_user_id", "one", "zero_many", "sends", px=830, cx=830, lpos=(806, 545))
    c.rel("users", "roommate_requests", "to_user_id", "one", "zero_many", "receives", px=910, cx=910, lpos=(944, 545))
    c.rel("users", "owner_profiles", "user_id", "one", "zero_one", "has")
    c.rel("users", "owner_profiles", "verified_by", "zero_one", "zero_many", "last verified")
    c.rel("users", "verification_records", "owner_id", "one", "zero_many", "is checked in")
    c.rel("users", "verification_records", "reviewer_id", "one", "zero_many", "decides")
    c.rel("users", "reports", "reporter_id", "one", "zero_many", "files")
    c.rel("users", "reports", "resolved_by", "zero_one", "zero_many", "resolves")
    c.finish()
    c.text(1120, 756, "target_type + target_id point to one LISTING, REVIEW or", "cap")
    c.text(1120, 772, "USER (no FK possible; checked by the application)", "cap")
    c.legend(1500, 110)
    c.note(1500, 330, 240, "Rules the diagram relies on", [
        "• A profile shares its user's key", "  (PK = FK user_id): 1 : 0..1.",
        "• A request cannot be sent to", "  oneself; at most one active", "  request per pair of students.",
        "• A student cannot block", "  themself; one block per pair.",
        "• owner_profiles keeps the current", "  verification status;",
        "  verification_records keeps", "  every decision (history)."])
    c.save("erd-logical-b.svg")


if __name__ == "__main__":
    chen_a()
    chen_b()
    chen_attributes()
    logical_a()
    logical_b()
    print("ERD SVGs written to", HERE)
