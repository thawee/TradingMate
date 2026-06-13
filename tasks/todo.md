# Plan - Clarify and Standardize Trading Vision

We need to align the codebase and AI Advisor with the user's defined trading playbooks:
1. **Swing Trade:** Target holding period is 2–4 weeks.
2. **Dividend Play:** Keep accumulating/compounding indefinitely unless fundamentals break (e.g., ROE < 15%) or the dividend yield drops below 3%.

## Tasks

- [x] **Phase 1: Update Educational Copy**
  - [x] Modify `edu_step3_content` in [strings.xml](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/res/values/strings.xml) to explicitly state the swing trade holding period of 2–4 weeks.
  - [x] Modify `edu_layer_3_content` in [strings.xml](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/res/values/strings.xml) to detail the dividend accumulation rule (hold/accumulate indefinitely unless quality breaks/ROE < 15% or yield drops below 3%).

- [x] **Phase 2: Update AI Advisor Master Prompts**
  - [x] Update the Swing Trade AI prompt in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to include the "Target holding period: 2–4 weeks" constraint.
  - [x] Update the Dividend AI prompt in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) to specify that candidates should be evaluated for indefinite accumulation/compounding, unless fundamentals break (ROE < 15%) or yield drops below 3%.

- [x] **Phase 3: Verification**
  - [x] Run a project compilation to verify build correctness.
  - [x] Verify that files are modified correctly and there are no syntax errors.

## Review & Results Summary
*   **Strings Updated:** Modified [strings.xml](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/res/values/strings.xml) to state that swing trades target a holding period of 2-4 weeks, and that dividend assets are held indefinitely unless quality deteriorates (ROE < 15% or high D/E) or dividend yield drops below 3%.
*   **Advisor Prompts Updated:** Refined the generated prompts in [DividendAdvisorScreen.kt](file:///Users/thawee.p/Workspaces/github/tradingMate/app/src/main/java/apincer/mobile/tradings/ui/DividendAdvisorScreen.kt) so the LLM evaluator applies the exact same 2-4 weeks constraint for swing plays and the indefinite accumulation check with the ROE < 15% / yield < 3% selling rule.
*   **Build Correctness:** Executed `./gradlew compileDebugKotlin` and verified that the project compiles cleanly (`BUILD SUCCESSFUL`).
