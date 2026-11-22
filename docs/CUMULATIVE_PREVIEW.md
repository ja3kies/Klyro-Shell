# Cumulative Preview policy

The canonical Preview is built only from `integration/senior-release-20261121`. Changes approved for distribution must be integrated here before the next cumulative build; never start from an older feature branch. All other branches may retain isolated signed artifacts and their branch-scoped Preview, but they must not replace the main in-app Preview. Stable and main are unchanged.

Before publication, review every active branch/PR updated during at least the previous seven days and retain all earlier approved functionality. Compare its head to the cumulative branch and classify it as ancestor, ported, pending, superseded, or intentionally excluded. Do not equate recent timestamps with merged code. Do not merge unfinished experiments merely because they are recent. Verify full CI and signed artifact contents; record the build run, source commit and actual release publication. If tests fail, keep the last successful cumulative build and say explicitly what is not yet published.

## Audit 2026-11-22

Baseline: `9705735f5a654df8625411c9d3d4b3a699ff9172`. GitHub comparisons for the 20 open PR heads #31–#54 (opened/updated since Nov 15, excluding the cumulative PR itself):

- 17 heads are ancestors: #31–#39, #42, #45–#51. This verifies ancestry, not proof that every behavior is unchanged.
- #40 diverged by one commit; the state-specific Enable/Disable semantics and decorative Switch accessibility fix are already present in the canonical source. Do not merge the older source over it.
- #52 and #53 diverged by 14 and 16 commits. The current motion ports and visibility fix supersede those implementations. The missing `KlyroMotionUiTest` is carried forward in this batch; physical execution remains a separate gate.
- #54 is the active cumulative integration PR. No merge into main is implied.

Publishing safeguards: check the cumulative baseline ancestry, reject stale builds and non-descendant publications with new commit metadata, validate signed asset digest, keep version codes monotonic. New clients only read the cumulative branch tag and a matching global tag; old workflows may still publish a legacy global tag until upgraded, so the canonical branch tag is authoritative. The last successful build remains available during a failed CI.
