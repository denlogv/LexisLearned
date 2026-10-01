#!/usr/bin/env python3
"""Builds samples/lighthouse-sample.epub: a tiny original story (3 chapters) for testing EPUB -> deck generation."""
import zipfile, pathlib

CHAPTERS = [
    ("The Arrival", """The ferry deposited Marta on the jetty just as the fog began to lift. She was weary after the journey, and her luggage seemed more cumbersome with every step along the slippery boards. The harbour was almost deserted; a solitary fisherman was mending a tattered net and did not so much as glance at her.
"You must be the new keeper's niece," he muttered at last, without looking up. "The old man has been expecting you, though he would never admit it." Marta smiled politely. She had been warned that the islanders were laconic, even wary of strangers, and she resolved not to take offence.
The path to the lighthouse wound steeply across a barren headland. Gulls wheeled overhead, and the wind carried the pungent smell of seaweed and salt. By the time she reached the door she was breathless, her cheeks flushed, her resolve slightly frayed. She knocked twice. Nobody answered, but somewhere inside a kettle began to whistle, shrill and insistent, as if it had been waiting all morning for her."""),
    ("The Keeper", """Her uncle turned out to be a gruff, stooped man with bushy eyebrows and a habit of clearing his throat before every sentence. He showed her the cramped kitchen, the spiral staircase, and the narrow bed under the eaves without a word of welcome, yet he had left a bowl of warm porridge on the table, which she took as a tacit gesture of affection.
"The lamp must never go out," he said gravely that evening. "Ships depend on it. Storms here can be treacherous, and a single lapse might cost dozens of lives." Marta nodded, though she secretly doubted that anyone still relied on such an antiquated beacon.
Over the following days she learned to polish the enormous lens, to trim the wick, and to record the weather in a battered ledger. Her uncle scrutinised every entry with a critical eye, grumbling about her untidy handwriting. Slowly, however, his manner softened, and she began to suspect that his brusque exterior concealed a deep and stubborn kindness that he was reluctant to display."""),
    ("The Storm", """On the fifth night the barometer plummeted, and the sea turned sullen and grey. By midnight a ferocious gale was hammering the tower, and the whole structure seemed to shudder. Marta clung to the rail as she climbed to the lamp room, her heart pounding, while her uncle wrestled with a jammed shutter far below.
Through the streaming glass she glimpsed a faint glimmer on the horizon, a vessel struggling against the swell, its mast lurching wildly. She hesitated only for a moment. Recalling everything she had been taught, she cranked the mechanism and watched the great beam sweep across the water, steady and defiant.
Hours later, when the tempest finally subsided, her uncle appeared in the doorway, soaked and exhausted. He surveyed the blazing lamp, then his niece, and cleared his throat. "Not bad," he said gruffly. It was the most generous praise she would ever receive from him, and she treasured it for the rest of her life."""),
]

def xhtml(title, body):
    paras = "\n".join("<p>%s</p>" % p.strip().replace("&", "&amp;") for p in body.split("\n") if p.strip())
    return ('<?xml version="1.0" encoding="utf-8"?>\n<html xmlns="http://www.w3.org/1999/xhtml"><head><title>%s</title></head>'
            "<body><h1>%s</h1>\n%s\n</body></html>" % (title, title, paras))

def main(out):
    items = "".join('<item id="c%d" href="c%d.xhtml" media-type="application/xhtml+xml"/>' % (i, i) for i in range(1, 4))
    spine = "".join('<itemref idref="c%d"/>' % i for i in range(1, 4))
    nav = "".join('<li><a href="c%d.xhtml">%s</a></li>' % (i + 1, t) for i, (t, _) in enumerate(CHAPTERS))
    with zipfile.ZipFile(out, "w") as z:
        z.writestr(zipfile.ZipInfo("mimetype"), "application/epub+zip", compress_type=zipfile.ZIP_STORED)
        z.writestr("META-INF/container.xml", '<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>', zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/content.opf", '<?xml version="1.0" encoding="utf-8"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">sample-lighthouse</dc:identifier><dc:title>The Lighthouse Niece</dc:title><dc:language>en-GB</dc:language></metadata><manifest><item id="nav" href="nav.xhtml" properties="nav" media-type="application/xhtml+xml"/>%s</manifest><spine>%s</spine></package>' % (items, spine), zipfile.ZIP_DEFLATED)
        z.writestr("OEBPS/nav.xhtml", '<?xml version="1.0" encoding="utf-8"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><ol>%s</ol></nav></body></html>' % nav, zipfile.ZIP_DEFLATED)
        for i, (t, body) in enumerate(CHAPTERS, 1):
            z.writestr("OEBPS/c%d.xhtml" % i, xhtml(t, body), zipfile.ZIP_DEFLATED)

if __name__ == "__main__":
    out = pathlib.Path(__file__).with_name("lighthouse-sample.epub")
    main(out)
    print("wrote", out)
