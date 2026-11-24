# German glossary (Deutsch, with a bit of ich_iel)

Faden's German is ordinary, everyday app German with a light sprinkle of r/ich_iel's "Zangendeutsch": the handful
of word-for-word coinages that German Reddit actually uses all the time. Terms marked **ZD** come from the community
dictionary [zangendeutsch.de](https://github.com/philippschuetz/zangendeutsch.de).

The app's name is itself one of these words: a Reddit *thread* is a **Faden** on ich_iel.

## Tone

- Address the reader as **du** (informal), lowercase "du/dein" in running text, as on r/de and r/ich_iel.
- Keep labels short; German runs about 30% longer than English, and buttons and chips don't wrap well.
- **Don't force it.** Only the few ich_iel words below get the Zangendeutsch treatment. Everything else uses the
  word a German Reddit user would actually type: "Feed", "Backup", "Flair", "Link", "Browser", "Spoiler", "NSFW".
  If a coinage needs explaining, or would make a setting harder to find, use the normal word.
- Use `…` and `„…"` (German quotation marks) in prose. Keep English quotes inside code or keys.
- Use "Bitte" sparingly. Errors are plain: "Konnte … nicht laden".

## ich_iel words (use these)

| English | German | Notes |
|---|---|---|
| post (noun) | **Pfosten** (pl. Pfosten) | ZD. "a post" → "ein Pfosten", "posts" → "Pfosten" |
| to post | **pfostieren** | ZD. "Post" (button) → "Pfostieren", "posted by" → "pfostiert von" |
| thread (comment thread) | **Faden** (pl. Fäden) | ZD |
| upvote | **Hochwähli** / **hochwählen** | ZD. "Upvoted" → "Hochgewählt" |
| downvote | **Runterwähli** / **runterwählen** | ZD. "Downvoted" → "Runtergewählt" |
| Reddit | **Lases** | ZD (read it → las es). Not in names or addresses: "reddit.com", "Boost for Reddit" stay |
| edited (marker on comments) | **Edith** | ZD. Verb "edit" (button) → "Bearbeiten" |
| meme | **Maimai** | ZD (if it ever appears) |
| cake day | **Kuchentag** | ZD (if it ever appears) |

## Reddit terms (plain German)

| English | German | Notes |
|---|---|---|
| subreddit, community | **Sub** (der, pl. Subs) | "r/…" prefix stays. "Communities" in lists → "Subs" |
| submission | Pfosten | |
| crosspost | Crosspost / crossgepostet | |
| comment | Kommentar | |
| reply | Antwort / antworten | |
| vote / score | Stimme / Punkte | "Hide scores" → "Punkte ausblenden" |
| OP | OP | Keep the "OP" badge as is |
| Redditor, user | Nutzer | |
| u/name, r/name | unchanged | |
| inbox | Posteingang | |
| message(s) | Nachricht(en) | |
| flair | Flair (das) | |
| NSFW, spoiler | NSFW, Spoiler | |
| karma, award | Karma, Auszeichnung | |
| moderator / mods | Moderator / Mods | "Modmail" → "Modpost" |
| multireddit / multi | Multireddit / Multi | |
| feed | Feed (der) | |
| front page | Titelseite | |
| popular / all | Beliebt / Alle | r/popular, r/all stay as names |
| sidebar (community info) | Seitenleiste | |
| watch (a community/post) | beobachten | |
| hide / save / report / block / share | ausblenden / speichern / melden / blockieren / teilen | |
| subscribe / join | abonnieren / beitreten | |
| link | Link | |

### Sorts

| English | German |
|---|---|
| Best | Beste |
| Hot | Heiß |
| New | Neu |
| Top | Top |
| Rising | Aufsteigend |
| Controversial | Kontrovers |
| Old | Alt |
| Q&A | F&A |
| Past hour / day / week / month / year / All time | Letzte Stunde / Heute / Diese Woche / Dieser Monat / Dieses Jahr / Allzeit |

## Internet and app words

Use the everyday word: Screenshot, Download / herunterladen, Upload / hochladen, Update / aktualisieren, Backup,
Browser, online / offline, Bot, Emoji, Spam, Feedback, Zoomen, Tastatur, GIF, Video, Bild, Galerie, Einstellungen,
Thema ("Dark mode" → "Dunkelmodus"), Ansicht / Karte / Kompakt, Konto / Anmelden / Abmelden, Benachrichtigungen,
Filter, Entwurf, kopieren / Zwischenablage, Suche, Abbrechen / OK / Fertig, Rückgängig, Nochmal.

| English | German |
|---|---|
| swipe / swipe actions / swipe mode | wischen / Wischgesten / Wischmodus |

## Numbers and time

- Relative times: "now" → "jetzt", "5m" → "5 Min.", "3h" → "3 Std.", "2d" → "2 T.", "4mo" → "4 Mon.", "1y" →
  "1 J."; in sentences "vor 3 Stunden".
- Decimal comma: "1,2k" (k for thousand stays, as on r/de).

## Also settled while translating

| English | German |
|---|---|
| Explore | Entdecken |
| custom feed | Eigener Feed |
| subscriptions / subs | Abos |
| DMs | PNs |
| submitted (profile tab) | Pfostiert |
| mod mail | Modpost |
| modqueue | Mod-Warteschlange |
| tag (a user) | markieren / Markierung |
| favorite | Favorisieren |
| recents | Zuletzt angesehen |
| theme management | Themenverwaltung |
| "Faden was updated" | Faden wurde aktualisiert |
| "{0}% upvoted" | {0} % hochgewählt |
| "(last edited {0})" | (Edith vor {0}) |

## One English string, two meanings

When the same English needs two German words, use `tc(context, english)` with the catalog key
`"context:English"`. "Post" the button is `t("Post")` → Pfostieren; "Post" the thing is `tc("noun", "Post")` →
Pfosten.
