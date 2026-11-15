# AGENTS.md — RISOPAY Development Instructions

## Project context

Before making changes, read `RISOPAY_MASTER_CONTEXT.md`.

It contains the project's architecture, requirements, existing implementation baseline, agreed decisions, and implementation roadmap.

## Mandatory rules

- Work on the existing RISOPAY codebase.
- Do not rebuild or replace the project.
- Inspect the current implementation before editing.
- Preserve existing working functionality.
- Do not remove existing features, tests, or fixtures without approval.
- Do not discard or overwrite uncommitted user changes.
- Preserve existing API contracts, DTOs, identifier formats, validation behaviour, and error responses unless explicitly approved.
- Avoid unnecessary dependencies.
- Keep the core architecture independent of AWS-specific implementations.
- Do not implement future roadmap stages prematurely.

## Development approach

Work on one stage at a time.

Before editing:
1. Inspect the relevant source files.
2. Explain the existing behaviour.
3. Explain the proposed modification.
4. List the files expected to change.
5. Identify risks and compatibility considerations.

After editing:
1. Compile the project.
2. Run relevant tests.
3. Run the full test suite when appropriate.
4. Report actual results.
5. Identify failures and unresolved issues.
6. Summarize all file changes.

Do not claim that a test passed unless it was executed successfully.

Do not commit or push changes without explicit approval.

Wait for the developer's confirmation before proceeding to another implementation stage.

## Current priority

Only Stage 1 of the roadmap in `RISOPAY_MASTER_CONTEXT.md` is authorized:

Implement bounded, replayable, temporary-file-backed XML request input while preserving existing endpoint behaviour, request-size enforcement, HTTP errors, and current processing results.

Do not start streaming XSD validation or StAX extraction until Stage 1 has been verified and approved.

# Token & Context Rules
- Read only the specific line ranges needed instead of entire large files.
- Run targeted single-file tests instead of full test suites unless asked.
- Keep responses brief: skip preambles, summaries of diffs I can already see, and full-file rewrites.
