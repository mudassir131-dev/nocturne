# Runbook 03: Android Keystore Key Rotation & Recovery

## Objective
Guidelines for managing hardware-backed cryptographic keys used for YouTube session cookie vault encryption.

## Key Rotation Procedure
1. Generate new Master Key alias `nocturne_vault_master_key_v2` in Android Keystore with `KeyGenParameterSpec.PURPOSE_ENCRYPT | KeyGenParameterSpec.PURPOSE_DECRYPT`.
2. Decrypt existing cookie payloads using `v1` key, re-encrypt under `v2`, and update shared preferences record.
3. Verify integrity with HMAC validation before wiping `v1` alias from Keystore.

## Failure Fallback
- If Keystore becomes corrupted due to device PIN/biometric change, prompt user for clean re-authentication rather than throwing unhandled `KeyPermanentlyInvalidatedException`.
