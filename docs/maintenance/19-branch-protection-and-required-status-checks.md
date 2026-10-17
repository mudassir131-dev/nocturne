# Maintenance 19: Protected Branch Rules and Mandatory CI Matrix Passes

## Branch Protection
- Requires signed commits, up-to-date branch status, and linear git commit history.
- Blocks merging into main until lint, unit tests, and assembleRelease jobs pass 100%.

