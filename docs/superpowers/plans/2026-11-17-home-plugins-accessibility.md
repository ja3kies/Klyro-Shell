# Home and Plugins accessibility closure

## Goal
Close the three current CI failures while preserving the established Klyro UI, current terminal keyboard, and the existing Home/Plugins behavior.

## Findings
- Home's Recheck button already exposes an accessible name and 48dp minimum target; its source-contract test incorrectly slices the Home code after the separately declared button lambda.
- Installed plugin rows expose the plugin name and Switch role but omit the state-specific action name; the passive visual Switch also retains a second accessibility node.
- The same three contracts fail in both the fast gate and the robot workflow; the current branch's UI state and dialog logic otherwise remain untouched.

## Tasks
1. Verify Home Recheck semantics and the current TalkBack target against Compose semantics and Klyro's 48dp rule.
2. Correct the Home contract test to inspect the actual Recheck lambda without changing its already-correct secondary TextButton.
3. Give each plugin toggle a state-specific accessible name and remove the decorative Switch from accessibility traversal; retain one row action and callback.
4. Update the source contracts to verify the one-node Switch semantics and Recheck label/target contract.
5. Run targeted regressions and the available CI gates; inspect final CI for this SHA.

## Acceptance
- TalkBack can distinguish Recheck/Checking and Enable/Disable for each plugin, with checked state announced by the native Switch semantics.
- Home CTA hierarchy, animation policy, plugin navigation, confirmation flow, and terminal keyboard remain unchanged.
- All applicable CI gates pass; actual Pixel visual/TalkBack validation is reported separately if unavailable.
