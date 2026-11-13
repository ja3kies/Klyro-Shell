# Registre des actions comportementales P0

Chaque ligne correspond à une exécution de test comportemental ciblé, pas à une lecture de fichier, un commentaire ou un commit. Les scénarios décrits dans le rapport complètent ce registre. Les tests purs de dispatch ne sont pas des tests de recomposition Compose.

## Cycle 1 — 15 vérifications

- `OutputUpdateBatchTest.outputDuringPublicationIsNotLost` : PASS
- `OutputUpdateBatchTest.publicationFailureCannotWedgeNextBurst` : PASS
- `OutputUpdateBatchTest.closedTabCannotPublish` : PASS
- `OutputUpdateBatchTest.finalChunkAfterFlushGetsItsOwnTrailingUpdate` : PASS
- `OutputUpdateBatchTest.deadlineDoesNotPublishEarly` : PASS
- `OutputUpdateBatchTest.removalDoesNotScheduleAnExtraTimerForNewTab` : PASS
- `OutputUpdateBatchTest.tenThousandChunksAreOneUiPublication` : PASS
- `OutputUpdateBatchTest.tabsAreDeduplicatedAndIndependent` : PASS
- `OutputUpdateBatchTest.finalChunkBeforeDeadlineIsIncluded` : PASS
- `OutputUpdateBatchTest.disposeCancelsAndRejectsFutureUiWork` : PASS
- `OutputBoundsTest.combiningFloodCannotGrowACellWithoutBound` : PASS
- `OutputBoundsTest.saturatedHistorySelectionIsBoundedAndKeepsNewestOutput` : PASS
- `OutputBoundsTest.normalGraphemeAndFollowingColumnsSurviveTheBound` : PASS
- `OutputBoundsTest.smallTranscriptMatchesExistingExtraction` : PASS
- `OutputBoundsTest.selectionBoundaryNeverSplitsUtf16Pair` : PASS

## Cycle 2 — 15 vérifications

- `TerminalDisplayActionsTest.emptyPromptRetainsItsCursor` : PASS
- `TerminalDisplayActionsTest.missingEmulatorIsUnavailable` : PASS
- `TerminalDisplayActionsTest.clearIsIsolatedAndResizeStillWorks` : PASS
- `TerminalDisplayActionsTest.pendingCsiRemainsTheProgramsSequence` : PASS
- `TerminalDisplayActionsTest.programOutputThatImitatesPromptCannotTriggerReplay` : PASS
- `TerminalDisplayActionsTest.partialCommandNeverDisappears` : PASS
- `TerminalDisplayActionsTest.splitUtf8Survives` : PASS
- `TerminalDisplayActionsTest.missingSessionIsUnavailable` : PASS
- `TerminalDisplayActionsTest.pendingOscTitleSurvives` : PASS
- `TerminalDisplayActionsTest.saturatedHistoryPurgesOnlyHistory` : PASS
- `TerminalDisplayActionsTest.wrappedLongCommandKeepsAllRows` : PASS
- `TerminalDisplayActionsTest.cursorInsideEditBufferIsRetained` : PASS
- `TerminalDisplayActionsTest.alternateScreenIsRefused` : PASS
- `TerminalDisplayActionsTest.multilineInputIsNotReconstructed` : PASS
- `TerminalDisplayActionsTest.bracketedPasteModeAndStdinAreUntouched` : PASS

## Cycle 3 — 15 vérifications

- `BootstrapAdversarialTest.linkedPrefixCannotBeReplaced` : PASS
- `BootstrapAdversarialTest.parentTraversalArchivePath` : PASS
- `BootstrapAdversarialTest.interruptedArchiveFailsWithoutActivatingPrefix` : PASS
- `BootstrapAdversarialTest.nulArchiveName` : PASS
- `BootstrapAdversarialTest.windowsAbsoluteArchivePath` : PASS
- `BootstrapAdversarialTest.previousBackupIsNeverOverwritten` : PASS
- `BootstrapAdversarialTest.ordinaryDataDoesNotBecomeExecutable` : PASS
- `BootstrapAdversarialTest.absoluteArchivePath` : PASS
- `BootstrapAdversarialTest.unicodeExecutableRetainsNameAndMode` : PASS
- `BootstrapAdversarialTest.linkedStagingCannotBeActivated` : PASS
- `BootstrapAdversarialTest.stageFileIsRejectedBeforeMovingUserData` : PASS
- `BootstrapAdversarialTest.backslashTraversalArchivePath` : PASS
- `BootstrapAdversarialTest.missingStagingLeavesOriginalAndNoBackupRename` : PASS
- `BootstrapAdversarialTest.danglingBackupCannotBeUsedForRecovery` : PASS
- `BootstrapAdversarialTest.existingSymlinkParentCannotEscapeExtraction` : PASS

## Cycle 4 — 15 vérifications

- `FileTransferAdversarialTest.foreignTemporaryFileCannotBeMoved` : PASS
- `FileTransferAdversarialTest.cancelledBeforeReadWritesNothing` : PASS
- `FileTransferAdversarialTest.revokedReadPropagatesFailure` : PASS
- `FileTransferAdversarialTest.inaccessibleExportNeverCreatesAnOutput` : PASS
- `FileTransferAdversarialTest.unicodeAndHiddenNamesRoundTripThroughCommit` : PASS
- `FileTransferAdversarialTest.emptyDocumentCopiesZeroBytes` : PASS
- `FileTransferAdversarialTest.diskFullPropagatesFailure` : PASS
- `FileTransferAdversarialTest.cancelledAfterReadCannotWriteChunk` : PASS
- `FileTransferAdversarialTest.preexistingUserFileSurvivesImport` : PASS
- `FileTransferAdversarialTest.symlinkTemporaryCannotBeCommitted` : PASS
- `FileTransferAdversarialTest.stalledProviderCannotSpinForever` : PASS
- `FileTransferAdversarialTest.shortReadsPreserveBinaryData` : PASS
- `FileTransferAdversarialTest.simultaneousImportsNeverOverwriteEachOther` : PASS
- `FileTransferAdversarialTest.interruptedWorkerRetainsInterruptStatus` : PASS
- `FileTransferAdversarialTest.missingTemporaryDoesNotCreateTarget` : PASS

## Cycle 5 — 15 vérifications

- `SessionDispatchTest.confirmedSnapshotDoesNotReadNewClipboard` : PASS
- `SessionDispatchTest.lastTabClosedRejects` : PASS
- `SessionDispatchTest.invisibleNewlineCannotReachConsumer` : PASS
- `SessionDispatchTest.liveTargetReceivesExactPayload` : PASS
- `SessionDispatchTest.switchedTabNeverReceivesOldPanePaste` : PASS
- `SessionDispatchTest.byteLimitAppliesBeforeConsumer` : PASS
- `SessionDispatchTest.closedTargetCannotInvokeDestructiveCallback` : PASS
- `SessionDispatchTest.malformedUtf16CannotReachConsumer` : PASS
- `SessionDispatchTest.ansiControlCannotReachConsumer` : PASS
- `SessionDispatchTest.negativeIdRejects` : PASS
- `SessionDispatchTest.endedSessionRejectsPaste` : PASS
- `SessionDispatchTest.staleCloseCannotSelectCurrentTab` : PASS
- `SessionDispatchTest.longIdsAreNotTruncated` : PASS
- `SessionDispatchTest.absentExpectedIdRejects` : PASS
- `SessionDispatchTest.invalidZeroIdRejects` : PASS

## Cycle 6 — 18 vérifications

- `SessionDispatchTest.renamedTargetKeepsItsIdentity` : PASS
- `SessionDispatchTest.refusedInputIsNotReportedAsInserted` : PASS
- `SessionDispatchTest.equalTitlesCannotAuthorizeDifferentSessionIds` : PASS
- `PasteDeliveryTest.testNormalModeDoesNotAddTerminalControls` : PASS
- `PasteDeliveryTest.testMultibyteInputIsOneExactTransaction` : PASS
- `PasteDeliveryTest.testUnsafeEscapesRemainFiltered` : PASS
- `PasteDeliveryTest.testBracketEnvelopeIsOneAcceptedWrite` : PASS
- `PasteDeliveryTest.testRejectedPasteDoesNotLeaveHalfEnvelope` : PASS
- `PasteDeliveryTest.testCrLfNormalizationIsUnchanged` : PASS
- `QueueAdversarialTest.invalidRangeCannotModifyQueue` : PASS
- `QueueAdversarialTest.ringWrapKeepsUnreadInputInOrder` : PASS
- `QueueAdversarialTest.closeReleasesWaitingConsumer` : PASS
- `QueueAdversarialTest.oversizedOfferRejectsWithoutPartialPrefix` : PASS
- `QueueAdversarialTest.closeReleasesBlockedStdoutProducer` : PASS
- `QueueAdversarialTest.fourMegabyteOutputIsLosslessUnderBackpressure` : PASS
- `QueueAdversarialTest.closedQueueRejectsAllNewInputButDrainsFinalBytes` : PASS
- `QueueAdversarialTest.concurrentOffersAreWholeTransactions` : PASS
- `QueueAdversarialTest.rejectionDoesNotDestroyPendingInput` : PASS

Les extraits .log versionnés normalisent les retours chariot/progrès et espaces
finaux ; les journaux bruts restent dans l'archive locale de preuves.
