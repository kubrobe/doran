# Doran Alley (도란 골목)

An Android pilot for practising spoken Korean before an exchange semester in Korea (spring 2027).
The learner walks a cozy, hand-drawn fictional Seoul campus neighbourhood and talks his way through
small everyday problems with recurring characters. Each episode gives him a reason to speak Korean
and someone who listens to the answer.

**Pilot scope:** Episode 1, *The classroom moved* (강의실이 바뀌었어요), plus a next-day recall scene,
pronunciation mirror, voice diary and 5-minute side quest.

- Kotlin + Jetpack Compose, Android only, `minSdk 26`, `targetSdk 35`
- **No paid API.** Bundled recordings or an on-device Korean voice; on-device recording; the phone's own speech recognizer where it supports Korean
- Interface, hints and explanations in **Uzbek (Latin), Russian and English** — switchable at any moment without losing your place. Korean stays the language of every scene
- Everything is stored on the phone (no account, no uploads)

## Build and run

1. Open the folder in Android Studio (Ladybug or newer) and let Gradle sync, **or** run `./gradlew assembleDebug`.
2. Install `app/build/outputs/apk/debug/app-debug.apk` on a phone or emulator (API 26+).
3. Unit tests: `./gradlew testDebugUnitTest`.

Every push to `main` builds the APK and runs the tests in GitHub Actions — download the APK from the
run's **Artifacts** (`doran-alley-debug-apk`).

## What's in the pilot

| Area | What it does |
|---|---|
| **Campus scene** | Canvas-drawn street (인문관 · 학생회관 · 도서관 · 골목 카페) and corridors with a readable room sign (`201호 강의실`) and wall map. Characters walk when the conversation moves them — e.g. send Seoyeon to the wrong building and she walks there and comes back. Reduced-motion option (also follows the system setting). |
| **Episode 1** | Four beats from the brief: closed door → hallway map → explaining to a second student → the project meeting moves to the café. Branching partner with natural clarifications (wrong room number, wrong building, missing building or number, "say that again"). |
| **Speaking first** | Mic button at the bottom (one-handed), with a live level meter so he can see the mic hears him. With Korean recognition: he confirms what the app heard (plus a *clarity* score against the closest model reply); ambiguous readings ask which he meant. If the in-app recognizer fails, the app tries the phone's other recognition services, then the phone's own speech-input screen, and only then falls back to recording: he replays his reply and confirms what he meant — labelled **Self-confirmed**. Mic declined: **Listening only** route (no speaking credit). |
| **Graduated help** | Hint 1 = meaning cue in his language, 2 = beginning of the Korean expression, 3 = model line with audio. The highest help used stays on record for that turn even if the next attempt succeeds. |
| **Communication vs recall** | Kept separate. A natural paraphrase moves the story on; the target expression is only credited when he actually produced it, and otherwise stays scheduled for a later chance. |
| **Words after the exchange** | Partner lines unlock after he replies: tap to hear again, see meanings, and save a word together with the sentence he heard. |
| **Reviews** | Later that day → next day → 3 days → a week (editable in Settings). Peeked or "not yet" items return after an hour. Each item tracks lesson audio, contexts, last practice, next review, help history and independent uses on distinct days. Items are keyed by a language-free id, so switching language never changes what is due. |
| **Next-day recall** | Due expressions first (produce before revealing), then *Today's twist*: the meeting moves again to a different place (Student Union / cafeteria / study room) — no hints unless he asks. |
| **Reflection** | End of episode: said on his own, needed help with, not yet said, when each item returns, one practice point, a takeaway expression, and what was unlocked (Seoyeon + Golmok Café). |
| **Pronunciation mirror** | Reference audio (normal and slow), **Record & rate**, A/B (model then him). Each take gets a **sound-match score** (0–100) against the model, syllables coloured by how closely they matched, and tips (louder, slower/faster, which syllable to focus on). **Check with recognition** shows which syllables Korean speech recognition caught. Takes are kept only if he chooses. Speaking ladder: hear → imitate → from meaning → new scene. |
| **Voice diary** | Baseline story, weekly retell prompts, side-by-side playback, delete. Shows independent expressions and successful clarifications next to the clips. |
| **5-minute side quest** | Three recall cards and one mirror phrase, then a clear stopping point with a takeaway expression. |
| **Daily rhythm** | The optional two-hour plan (20 / 35 / 20 / 35 / 10 min) on the home screen, each block linked to its activity. |
| **Playtest tools** | Settings → shift the app clock by +4 h / +1 day to test next-day recall today; device check for Korean voice, recognition, mic and bundled recordings. |

## Content files (no code changes needed)

```
app/src/main/assets/
├── config/campus.json        campus, buildings, places — rename here; lines use {b:haksaeng} placeholders
├── config/characters.json    recurring partners (classmate Minji, staff Mr. Park, coworker Junho) + Seoyeon
├── config/vocab.json         expressions, meanings, which surface forms count as "used"
├── config/app.json           review timings, daily rhythm, diary prompts
├── i18n/ui.json              every interface string in uz / ru / en
├── episodes/index.json       episode list (planned episodes appear as "coming soon")
├── episodes/ep01_classroom_moved.json   scenes, Korean lines, hints, accepted intents, recall variants
└── audio/                    reviewed recordings (see audio/README.md)
```

**Adding episode 2:** write `episodes/ep02_….json` in the same format, set its entry in `index.json`
to `"status": "playable"` with a `"file"`. The home screen, player, reviews and mirror pick it up — no
navigation changes. Run the unit tests: `ContentTest` validates every node link, speaker, word,
location and translation.

### How replies are matched

Each turn lists **intents** — what the learner might mean — with token groups, room-number rules
and examples. `강의실이 어디예요?`, `어디로 가면 돼요?` and `몇 호예요?` all reach the same outcome.
Room numbers are read from digits and spoken Sino-Korean (`삼백이 호`, `삼, 공, 이`). A wrong number
or building leads to a natural clarification from the partner, not an error message.

### How pronunciation is rated

Two independent, offline-capable measures (`app/src/main/java/com/doranalley/score/`, plain Java with JVM unit tests):

- **Sound match** (`Acoustic`): both takes are trimmed to the speech, turned into normalized MFCC frames with a
  vocal-tract-length search (so a man imitating a female model voice isn't penalized), and aligned with dynamic
  time warping. The cost is compared with aligning against the model played backwards, so the score
  self-calibrates. Korean is syllable-timed, so the model is split evenly into syllables to show roughly which one
  drifted. It judges overall sound shape and rhythm — not individual phonemes — and says so in the app.
- **Understood by recognition** (`Hangul`): what the phone's recognizer heard is aligned with the phrase syllable by
  syllable, comparing jamo (initial/vowel/final) with credit for common near-misses (ㅐ/ㅔ, plain/tense/aspirated,
  final-consonant neutralization). Digits ("302호") are read as Sino-Korean.

The model sound is the bundled recording when there is one, otherwise the Korean TTS voice rendered to a cached WAV.

## Before treating it as teaching material

- **Native-speaker review.** All Korean dialogue is marked *draft* in the app until a proficient Korean
  speaker reviews the lines and recordings (`"review": {"status": "draft"}` in the episode file).
- **Recordings.** No reviewed recordings are bundled yet. Until they are, lines play with a clearly
  labelled *synthetic preview* where the phone has a Korean voice, or as text only.

## Status and known gaps

- Core logic (engine, matching, scheduler, content validation, save/resume) is covered by JVM unit tests.
- On-device checks still to run on a real phone/emulator: whole episode, denied microphone, missing Korean
  voice, closing/reopening mid-lesson, switching all three languages, replay of his own recording.
- Speech recognition and recording can't run at the same time on Android, so replies captured through
  recognition are not saved as audio (record mode keeps the audio). In the mirror he records first (sound match),
  then optionally checks with recognition.
- The sound-match thresholds are calibrated on synthetic voices; tune `Acoustic.map()` after the first real playtest.
- Open-ended voice conversation is a later milestone, once there is a runtime budget.

## First playtest

Give the phone to the learner with one instruction: *"Find the room and help your classmate."*
The next day (or Settings → +1 day), open **Spoken recall → Today's twist** and see what he can say
without a hint. Watch where he hesitates before writing more episodes.
