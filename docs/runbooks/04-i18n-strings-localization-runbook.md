# Runbook 04: Crowdsourced Localization & String Extraction

## Objective
Standardized workflow for extracting translatable UI strings, validating syntax placeholders, and integrating community translations.

## Extraction Workflow
1. Ensure all hardcoded UI strings in Compose components reference `stringResource(R.string.*)`.
2. Run string linter to check for missing format specifiers (`%1$s`, `%2$d`).
3. Merge translated `strings.xml` PRs into matching `res/values-<lang>/` directory.
4. Validate right-to-left (RTL) layout mirroring on Arabic and Urdu configurations.
