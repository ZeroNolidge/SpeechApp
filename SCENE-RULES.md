# Scene rules (Rory's Kitchen and every story scene)

Every step in portrait and landscape is checked against these rules on a screenshot before a test link is shared.

## Ruby's rules

1. **Nothing important gets covered.** Overlap is fine only when it's a real-world "in" or "on" (berries in the bowl, butter on the pancake). Props wait beside the thing they act on, then move in.
2. **Characters always stay visible.** Rory and Bear keep their faces and at least two-thirds of their bodies showing. Props never cover a face, and Bear always sits on something.
3. **Keep real-life sizes.** Rory is the biggest thing in the scene. Bear is about half Rory's height, and a plate is about as wide as Rory's body. Food and tools fit in a character's hands.
4. **One focus at a time.** The thing for the current step sits in the middle of the scene at full size, everything else is quieter, and there are no identical repeated tiles.
5. **Fill the frame.** The scene has a back wall, a counter and a floor, with no empty top half and nothing stuck to the screen edges.
6. **Ground every object on the counter or floor.** Nothing from the scene goes into the answer strip. Every counter prop's drawn bottom (`BOWL`, `BATTER`, `MILK`, `BERRIES`, `BUTTER` dish, `PAN`, `PANCAKE`) must touch its box's bottom (`1.0f`) so it rests on the counter surface (`counterTop + PROPS_MARGIN`) with zero floating gap. Bear's stool stands upright (zero tilt) with all three legs grounded on the floor/skirting line (`BEAR_FLOOR = 1.0f`).
7. **Check before every link.** Every step is checked on a screenshot in portrait and landscape (including 4:3 tablet viewports like `1024x660` and `1024x768`) to confirm nothing is covered, nothing floats, and the sizes look right.

## Ruby's checklist (per step, portrait and landscape)

- [ ] Whatever Rory is talking about is fully visible and clearly separate from everything else.
- [ ] Things overlap only when it makes sense in real life, and the sizes are right.
- [ ] Rory and Bear are fully visible and not cut off. Rory's eyes and mouth are clear on talking steps, and his speech bubble sits inside the visible frame without clipping at the top edge.
- [ ] Sizes match real life: Rory is biggest, then Bear and the bowl, then the pan/pancake and plate, then a berry. Food sits on a plate or the counter, and Bear sits on his stool with all three legs touching the floor line.
- [ ] Every counter prop (`BOWL`, `BATTER`, `MILK`, `BERRIES`, `BUTTER` dish, `PAN`, `PANCAKE`) rests on the counter surface (`y >= counterTop`) and never floats in mid-air above the counter line.
- [ ] When berries are tapped into the bowl (1, 2, 3), each tapped berry stays clearly visible on top of the batter inside the bowl.
- [ ] The main object for the step stays where it is, and new items show up beside it before moving in.
- [ ] There's only one main thing to look at, with no copies of the same picture next to each other.
- [ ] The scene fills the screen: a wall, the counter, no empty top half, and nothing cut off at the edges.
- [ ] Nothing from the scene goes into the answer strip, and Bear's stool legs touch the floor line cleanly right above the strip.

## Earlier rules (Aida and Cerebral)

- Props and images don't overlap. Where they do (berries in the bowl, butter on the pancake) they are in true proportion, and the thing in front looks like it belongs there.
- A character (Rory, Bear) is never fully hidden, and their face is always visible.
- Whatever Rory names or asks "How many?" about is fully visible and clearly separate from everything else.
- Sizes are realistic: a berry is smaller than the bowl, the bowl and the pancake are smaller than Rory, food sits on a plate or the counter, and Bear sits on his stool.
- Rory, Bear and any host are never hidden or cut off; on talk steps Rory's eyes and mouth are never covered.
- The main object for each step stays in view, and new items go beside it, not over it.
- Every step is tap OR talk, never both, and is led by Rory's voice (no reading needed).
- Single-try voice turns: the child never has to repeat the same word 3 times in a row, and after a warm listening window (~3.5 s) Rory celebrates and advances gently without nagging.

## Automated check

`KitchenLayoutTest` and `KitchenSceneRulesTest` check the pure layout maths for every kitchen beat at 1024x768 and 768x1024: resting props don't overlap except allowed in/on pairs, every prop's drawn bottom touches `1.0f` on the counter surface, Bear's stool legs touch `BEAR_FLOOR = 1.0f`, no prop covers Rory's or Bear's face, Rory is at least 1.6x Bear's height, and at least two-thirds of each character is on screen. Screenshots are still reviewed by eye across `1280x720`, `1024x660` / `1024x768`, and `768x1024`.
