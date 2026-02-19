# AURA UX Interaction Map

## Navigation Layers

1. Root stack (`App.tsx`)
2. Main tab shell (`Main.tsx`)
3. Feature flows (AURA routes, profile routes, messaging detail)

## Primary Entry Points

1. `Onboarding` -> `Intake.Home`
2. `Search` tab -> `Matching.Home` (new quick action)
3. `YourProfile` tab -> `AURA Hub` (new quick actions)
4. Existing tab navigation:
   - `YourProfile`
   - `Chat`
   - `Search`
   - `Likes`
   - `Donate`

## AURA Flow Map

1. Intake
   - `Intake.Home`
   - `VideoIntro`
   - `Assessment.Home` -> `Assessment.Question` -> `Assessment.Results`
   - `VideoVerification`
2. Matching
   - `Matching.Home`
   - `Matching.Filter`
   - `Matching.Compatibility`
   - `Growth.Context` <-> `Values.Hierarchy`
   - `Bridge.Journey`
   - `MatchWindow.List`
3. Relationship progression
   - `MatchWindow.List` -> `Bridge.Journey`
   - `MatchWindow.List` -> `Calendar.Availability`
   - `MatchWindow.List` -> `Main (Chat tab)` for accepted windows
4. Video dates
   - `VideoDate.List`
   - `VideoDate.Schedule`
   - `VideoDate.Call`
   - `VideoDate.Feedback`
5. Trust/safety
   - `Report.User`
   - `Reputation.Score`

## Interaction Contracts

1. Match decision loop
   - Open match window
   - Read match reason and intro messages
   - Optional intro message
   - Accept / decline / skip
2. Compatibility deep dive
   - Open breakdown
   - Review dimensions, reliability, dealbreakers, guided prompts
   - Continue to full profile or back to matches
3. Growth profile loop
   - Save growth context (state + traits)
   - Save ranked values + tradeoffs
   - Return to matching with updated context
4. Journey loop
   - Milestone/check-in response
   - Generate/accept/dismiss date suggestions
   - Track relationship status changes

## Cohesion Fixes Applied

1. Added AURA quick entry in `Search` (`AURA Matches` action).
2. Added `AURA Hub` in `YourProfile` with direct access to:
   - `Intake.Home`
   - `Matching.Home`
   - `MatchWindow.List`
   - `VideoDate.List`
   - `Reputation.Score`
3. Wired `Matching.Filter` route to shared search-settings UX so filters are reachable from matching context.
4. Wired `Assessment.Category` route for category-specific assessment navigation.
5. Added direct `Report.User` action on compatibility breakdown.
6. Fixed shared-questions CTA in compatibility breakdown (`See All` now toggles full list).
7. Added automated route wiring validator:
   - `node /Users/tkhan/IdeaProjects/alovoa/scripts/validate-ui-wiring.mjs`
   - validates typed routes, registered routes, and AURA route entrypoints.
8. Fixed `Search` root container accidental red background to theme background.
9. Fixed broken accepted-window chat action:
   - from invalid `Messages.Detail` route
   - to `Main` chat tab navigation.
10. Added `Matching.Home` quick link to `MatchWindow.List`.

## Remaining UX Risks To Track

1. Mixed legacy (`Search` swipe deck) and AURA curated matching paradigms can confuse users without explicit explanation copy.
2. Some strings in AURA screens are hardcoded English instead of i18n keys.
3. `Intake` progress mapping depends on backend field consistency; if fields drift, completion visuals can become inaccurate.
