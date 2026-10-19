# Reference record

## Confirmed requirements from the user's gameplay description

- Two stages: timed catapult launches, then timed dives onto polar bears.
- Successful catapult launches determine the attack team.
- Space and left mouse perform the current action; Esc pauses/resumes.
- Remaining penguins, remaining targets and the objective are visible.
- Active dives settle before a result; retry and next navigation work.
- Snowy cartoon presentation; offline Windows desktop delivery.

No screenshots, gameplay videos, reference assets, or detailed gameplay notes were attached. These are confirmed requested mechanics, not independently observed original-game details.

## Unverified implementation assumptions

- Eight penguins approach one at a time, with a 0.48-second gap after each attempt. A press outside the cradle consumes that penguin; no press misses automatically.
- Launch speed is 250 px/s; the ±34 px launch window gives 272 ms total. Successful launch arcs are visual effects; success is determined by the cradle window.
- The camera pans horizontally to a fixed overview of the enemy field over 1.6 seconds. Reference camera tracking, zoom and framing remain unverified.
- Penguins fly left-to-right, 1.05 seconds apart, at 220 px/s and y=175. Input selects the oldest available visible flying penguin. Flight order and spacing are unverified.
- A dive begins with downward speed 400 px/s, forward speed 145 px/s and gravity 1500 px/s². Ground is y=570. Neither steering nor bouncing is implemented.
- Three bears have one-hit defeat; one diving penguin hits at most one bear. Bounds are an expanded rectangle; there are no armor, bonus, multi-hit, combo, power-up or rescue systems.
- Score is 10 per successful launch and 100 per bear. This is a placeholder and is not asserted to match the reference.
- When all bears are defeated, unused flights retire and every already-active dive resolves before victory. Defeat waits until no penguin has any flight/dive opportunity remaining.
- One level is supplied. “Next” leads to an explicit level-complete screen, not an invented campaign.

All numeric gameplay assumptions are in `levels/level_01.json`. Original vector-like drawings and generated audio are placeholders. Exact reference launch sequence, movement, collisions, camera, animation timing, palette and audio require supplied screenshots/footage for a fidelity pass.
