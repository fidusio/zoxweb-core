# META-ENCRYPTED-DATA — `EncryptedData`, `EncapsulatedKey` and the key maker

**Home:** `org.zoxweb.shared.crypto` (the records: `EncryptedData`, `EncapsulatedKey`,
`KeyLockType`, `CryptoConst`), `org.zoxweb.shared.security.KeyMaker` (the contract),
`org.zoxweb.server.security` (`CryptoUtil` the cryptography, `KeyMakerProvider` the key maker,
`CipherCodecs` the binary storage form).
**Status:** the authoritative description of the design. Read it before reviewing, explaining or
changing any of these classes.

---

## 1. Purpose and scope

The subsystem answers one question: **how does a field value, a file or a key get sealed so that
only the right subject can open it, and so that a stored copy cannot be re-pointed, edited or
revived?** Three pieces answer it:

| Piece | Class | One instance means |
|---|---|---|
| Record | `EncryptedData` | **one sealed value**: AES-256-GCM ciphertext plus the authenticated attributes that describe it |
| Wrapped key | `EncapsulatedKey extends EncryptedData` | **one 32-byte AES key sealed under another key**, plus who owns it and what it protects |
| Key maker | `KeyMaker` / `KeyMakerProvider` | the service that mints, stores, looks up and unwraps keys along a **chain** that ends at the master key |

Everything else is a helper: `CryptoUtil` holds the one sealing path and the one opening path,
`CipherCodecs` packs a record into bytes for a datastore column, `SharedBase64` renders binary
attributes as text, and `SecUtil` supplies the random source (§4.4).

What the subsystem deliberately is **not**:

- Not a key-management server. There is no key registry, no key rotation schedule, no revocation
  list. Rotation is `rekey`, a per-row operation. Revocation is deleting the row.
- Not a per-version file record. A record seals bytes; what those bytes belong to is the caller's
  business (see `data_type`, §2.2).
- Not JSON on the wire. The associated data is a fixed-order `|`-joined attribute string built
  by the record itself; the persisted form is whatever a store chooses (§5).

---

## 2. `EncryptedData` — the record

### 2.1 Shape

`EncryptedData` is a `PropertyDAO` (so the meta-model, `GSONUtil` and `APIDataStore` can carry
it) whose ten `Param` fields are the record. The entity fields it inherits (GUID, subject GUID,
name, description, timestamps, properties) are **not part of the record and are not
authenticated**; only the ten below are.

```
#   field         type    meaning
1   v             int     format version, always 2 (EncryptedData.VERSION)
2   alg           string  cipher: A256GCM for a plain record; a KEM name for a KEM-wrapped key (§3.4)
3   kdf           string  HKDF-SHA256: the cipher key is derived from the wrapping key, never used raw
4   iv            bytes   12-byte random GCM nonce, fresh per seal
5   data_length   long    plaintext length in bytes
6   mask          string  optional display fragment for ENCRYPT_MASK fields, e.g. "****1234"
7   exp           long    optional expiry, epoch millis; 0 or absent never expires
8   hint          string  optional free text
9   data_type     string  what the plaintext is; a label the record carries and authenticates but never interprets
10  cipher_data   bytes   ciphertext with the 16-byte GCM tag appended
```

Text attributes go raw into the associated data string (§2.2), so the setters refuse a `|` in
them (`checkText`). `exp` clamps negatives to 0. `data_length` refuses negatives. The numeric
fields are initialised by the meta-model when the entity is constructed, so a fresh record reads
`v = 0`, `data_length = 0` and `exp = 0`; the getters never return null and `isExpired` is safe
on a record that has not been sealed yet.

### 2.1.1 `iv` and `kdf`: how the cipher key comes about

The two fields work together and both are set by `sealRecord`, never by the caller.

**`iv`** is 12 random bytes drawn fresh from `SecUtil.randomBytes` on every seal. It serves
three roles:

- the GCM nonce: AES-GCM runs AES in counter mode and the counter blocks start from it. The same
  key with the same nonce twice would leak the XOR of the two plaintexts and allow tag forgery,
  so it is never reused;
- the HKDF salt (below), which is why it must be stored in clear. It is not secret, only unique;
- an authenticated attribute: it sits in the associated data, so a flipped IV byte fails the tag
  instead of decrypting to garbage.

**`kdf`** is the string `HKDF-SHA256` and names how the real cipher key is obtained from the
wrapping key. The wrapping key handed to `encryptData` or `wrapKey` is never fed to AES. Instead:

```
recordKey = HKDF-SHA256(ikm = wrappingKey, salt = iv, info = "enc", length = 32)
```

Each record is therefore encrypted under its own derived key, tied to its own nonce. One
wrapping key can seal millions of records and no two ever share a (key, nonce) pair; the 96-bit
random nonce does not carry the whole collision-safety burden alone. The derived key lives for
the duration of the call and is zeroed in a `finally`.

`kdf` is a version marker for the derivation, not a caller's choice: `openRecord` refuses any
other value, and because it is authenticated it cannot be swapped after sealing. A future format
with another derivation would announce itself here.

HKDF extracts and expands, it does not iterate. That is deliberate: the input is always a random
32-byte key, never a password, so `checkRecordKey` enforces `MIN_KEY_BYTES` of 32 and the record
path never pays PBKDF2-style cost. Password handling stays in `CIPassword` and `SecUtil`.

On open the same two fields drive the reverse: read the stored `iv`, check `kdf` is the
supported one, re-derive the record key with the same HKDF call, run GCM decrypt with the same
nonce and the same associated data. Wrong wrapping key, altered IV or altered `kdf` all end in
the tag mismatch.

### 2.2 The associated data: the seal over the attributes

`toAssociatedData()` joins fields 1 to 9 above, everything except the ciphertext, in that fixed
order with `|`: binary as base64url (`SharedBase64`, URL alphabet, `=` padded), numbers as
digits, absent as empty. That string is the GCM associated data. The record builds it itself
from its attributes and it exists **for the tag**: it is never read back from storage. Whichever
form a record arrives in (§5), the attributes are restored first and the string is rebuilt from
them at open time. The canonical text form is this same string with `|` and the ciphertext
appended; that is a convenience of the encoding, not a dependency, and a parsed record gets its
associated data recomputed, never copied.

This is the successor of the earlier separate HMAC over ciphertext plus metadata. GCM is an
AEAD mode: the 16-byte tag appended to `cipher_data` is a MAC over the associated data and the
ciphertext, keyed by the same derived key. One pass, one tag, no second key. So **every
attribute except the ciphertext is authenticated by the tag**, and

- editing `mask`, `hint`, `exp`, `data_type` or `data_length` after sealing fails the tag;
- dropping an optional attribute fails the tag (empty ≠ the value that was there);
- extending or clearing an expiry to revive a record fails the tag;
- flipping a bit in the IV, the ciphertext or the tag fails the tag.

Consequently `mask`, `exp`, `hint` and `data_type` must be set **before** sealing. The
`EncryptedDataTest.everyAuthenticatedAttributeIsBound` test walks each of these cases.

`data_type` deserves one more sentence: it says what the plaintext is (by default the class
name of its Java type) so a reader that has opened the record can trust the label and pick the
mapper from bytes to object. The record never interprets it, and the binary codec (§5) requires
it.

### 2.3 Expiry

`isExpired(now)` is true when `exp > 0 && exp <= now`; a record without expiry never expires.
The open path (§4.2) checks expiry **after** the tag is verified, so:

- a genuinely expired record opens the tag, then is refused with `AccessSecurityException`;
- the wrong key on an expired record is `SignatureException` (tampering), not expiry;
- an expiry edited into the past on a live record is `SignatureException`, not expiry.

The verdict survives every storage form: packed bytes, entity JSON.

---

## 3. `EncapsulatedKey` — the wrapped key

### 3.1 Shape

`EncapsulatedKey` **is** an `EncryptedData` whose plaintext is a random 32-byte AES key. The row is
itself the record: its `iv`, `data_length` and `cipher_data` are the sealed key. It adds five
fields on top of the ten:

```
field           bound?  meaning
subject_guid    yes     owning subject (inherited entity field, but here it is bound)
reference_guid  yes     the entity this key protects, or the subject itself for a subject key
key_guid        yes     the key that wrapped this one: parent key for a symmetric wrap, the public
                        key's registry id for a KEM wrap, null under the master key
key_size        yes     outer key size in bytes (32) for a symmetric wrap; for a KEM wrap the
                        encapsulated key's size, the split point inside cipher_data
reference_type  no      class name of the referenced entity, a label
key_lock_type   no      KeyLockType.SUBJECT_ID or NVENTITY, a label
```

It implements `DoNotExpose`, a marker interface (no consumer in this repo) that sibling
projects use to keep a row out of anything they expose.

### 3.2 Binding data: what "bound" means

`toBindingData()` is `subject_guid | reference_guid | key_guid | key_size`, absent as empty.
It is passed as **extra associated data** after the record's own associated data, on both wrap
and unwrap. So on top of everything §2.2 authenticates, a wrapped key also refuses to open when
it has been

- re-pointed at another subject,
- re-pointed at another entity,
- claimed to be wrapped by another key,
- split at another offset (KEM case).

And the sealed record copied verbatim into another key row with a different identity fails too.

The two labels, `reference_type` and `key_lock_type`, are **deliberately not bound** so a
renamed class or a corrected lock type never invalidates stored keys. The algorithm needs no
place in the binding: it is a record attribute (§2.1) and already authenticated.

Of the four bound fields only `key_guid` runs `checkText` and refuses a `|`; `reference_guid`
and the inherited `subject_guid` are stored as given. They are UUIDs, which never contain the
separator, so the binding string cannot be made ambiguous in practice; extending the guard to
the other two setters is listed in `PENDING.md` as a defensive change with no stored-format
impact.

The **entity GUID plays no part in the crypto**. It is the datastore's identity; the datastore may
assign or change it at any time without effect on unwrapping. `KeyMakerProvider` happens to set
the row GUID to the referenced entity's GUID as a convenience, nothing depends on it.

Binding fields must be set **before** wrapping and must never change afterwards. Setting a
binding field on an already-wrapped key breaks it, as it must.

### 3.3 Symmetric wrap

`CryptoUtil.wrapKey(ek, wrappingKey, keyMaterial)`:

1. `keyMaterial` null means a fresh random 32-byte key; supplied material must be ≥ 32 bytes.
2. `key_size := wrappingKey.length`.
3. Seal `keyMaterial` into `ek` (§4.1) under `wrappingKey`, with `toBindingData()` as extra AAD,
   `alg = A256GCM`.

`createEncryptedKey(ek, wrappingKey)` is `wrapKey` with null material. `createEncryptedKey(wrappingKey)`
builds a bare row with no binding fields (`"|||32"`); prefer the form that takes a prepared row.

`unwrapKey(ek, wrappingKey)` opens it with the same binding AAD and returns the 32 bytes.
Handing the row to the plain `decryptEncryptedData` without the binding fails closed.

`rekeyEncryptedKey(ek, originalKey, newKey)` unwraps under the old key and wraps the **same
material** under the new key: a fresh nonce and ciphertext, unchanged key material, unchanged
binding. Everything sealed under this key's material keeps opening. The material is zeroed after
the rewrap.

### 3.4 KEM wrap (ML-KEM)

The same row can be sealed for the **holder of an ML-KEM private key**, using only the public
key. `alg` then names the parameter set (`ML-KEM-512`, `ML-KEM-768`, `ML-KEM-1024`, the
library's own names) instead of `A256GCM`, and `isKEMWrapped()` is true.

`wrapKeyMLKEM(ek, algorithm, publicKey, keyMaterial)`:

1. Encapsulate to `publicKey`: the KEM yields a 32-byte shared secret (the **outer key**) and an
   encapsulated key (the KEM ciphertext).
2. `key_size := encapsulatedKey.length` (768 / 1088 / 1568 for the three sets).
3. Seal `keyMaterial` under the outer key with the binding AAD and `alg = <parameter set>`.
4. `cipher_data := encapsulatedKey ‖ sealedKey`. The outer key is zeroed and **never stored**.

`unwrapKeyMLKEM(ek, privateKey)`:

1. Refuse a row that is not KEM-wrapped or names a set the library does not know.
2. Split `cipher_data` at `key_size`; `key_size` must equal the parameter set's encapsulation
   length, else "malformed".
3. Decapsulate the front part with `privateKey` to recover the outer key.
4. Open the rest (§4.2) under the outer key, with the binding AAD and the expected `alg`.

ML-KEM **never fails to decapsulate**: a wrong private key, or an encapsulated key altered in
place, yields an unrelated secret, and the GCM tag refuses it. A private key from the wrong
parameter set is refused as malformed (`IllegalArgumentException`) before any cryptography.

`getKEMCiphertext()` returns a copy of the front `key_size` bytes, null for a symmetric wrap.
`rekeyEncryptedKeyMLKEM` re-seals the same material for a new public key, possibly of another
parameter set.

Readers split on `key_size`, not on a hard-coded number, so any parameter set the KEM library
knows works without a change to this code. `MLKEMKeyWrapTest.everyParameterSet` runs all three.

### 3.5 What a key row looks like

Symmetric wrap under a 32-byte parent, JSON view (entity fields omitted):

```
v=2  alg=A256GCM  kdf=HKDF-SHA256  iv=<12 B>  data_length=32  data_type=<unset>
cipher_data=<48 B: 32 sealed + 16 tag>
subject_guid=S  reference_guid=R  key_guid=P  key_size=32
reference_type=org.zoxweb.shared.data.FileInfoDAO  key_lock_type=NVENTITY
binding data = "S|R|P|32"
```

ML-KEM-768 wrap: `alg=ML-KEM-768`, `key_size=1088`, `cipher_data` is 1088 + 48 bytes,
binding data `"S|R|<pk registry id>|1088"`.

---

## 4. `CryptoUtil` — the one seal path and the one open path

### 4.1 `sealRecord` (private; reached through `encryptData` and the wrap methods)

```
checkRecordKey(key)                       key must be >= MIN_KEY_BYTES (32)
data == null  ->  data = random 32 bytes  (this is how a fresh key is minted)
iv = random 12 bytes
record.v = 2, alg = <given>, kdf = HKDF-SHA256, iv, data_length = data.length, cipher_data = null
recordKey = HKDF-SHA256(ikm = key, salt = iv, info = "enc", 32)
AES/GCM/NoPadding, 128-bit tag, SunJCE if present
AAD = record.toAssociatedData()  (+ extraAssociatedData when given)
cipher_data = ciphertext ‖ tag
zero recordKey
```

Points that matter:

- **The wrapping key is never used directly.** Each seal derives a one-off cipher key from the
  wrapping key and the fresh nonce, so the same wrapping key across millions of records never
  reuses (key, nonce). This is why `MIN_KEY_BYTES` is 32 and why keys must be random and full
  size: HKDF adds no stretching. Passwords never reach this path.
- `alg` is written into the record **before** sealing, so it is authenticated; a KEM name in a
  row cannot be swapped for `A256GCM` later.
- The AAD never includes `cipher_data`, so a row can be resealed in place: `rekey` overwrites
  the nonce and ciphertext of the same row and the new tag covers the new attributes only.
- `hkdfSHA256` is RFC 5869 over HMAC-SHA256, with an RFC test vector in `EncryptedDataTest`.

### 4.2 `openRecord` (private; reached through `decryptEncryptedData` and the unwrap methods)

```
checkRecordKey(key)
version != 2                       -> IllegalArgumentException
alg != expected                    -> IllegalArgumentException   (A256GCM, or the KEM name)
kdf != HKDF-SHA256                 -> IllegalArgumentException
iv missing / not 12 B, ciphertext missing / shorter than the tag -> SignatureException
recordKey = HKDF-SHA256(key, iv, "enc", 32)
GCM decrypt with the same AAD      bad tag -> SignatureException("Data tampered with")
plain.length != data_length        -> SignatureException (plain zeroed)
record.isExpired()                 -> AccessSecurityException (plain zeroed)
return plain
```

Order matters and is the contract: format checks, then the tag, then the length, **then**
expiry. Every open path ends here, so the expiry refusal applies to plain records and to wrapped
keys alike, and a forged expiry always surfaces as tampering.

`openRecord` takes the ciphertext as an argument rather than reading it from the record, so the
KEM path can hand over the slice after the encapsulated key while the AAD still comes from the
row.

### 4.3 Exception contract

| Situation | Exception |
|---|---|
| key shorter than 32 bytes, unknown version / cipher / KDF, unknown KEM set, negative size, `|` in a text attribute | `IllegalArgumentException` |
| wrong key, wrong private key, altered attribute, altered IV / ciphertext / tag, re-pointed binding, nothing wrapped yet, malformed KEM split | `SignatureException` |
| tag verified but expiry passed | `AccessSecurityException` |

`KeyMakerProvider` folds all of the JCE checked exceptions and `SignatureException` into
`AccessSecurityException` at its boundary.

### 4.4 The random source

Every nonce, every minted key and every KEM key pair draws from `SecUtil.defaultSecureRandom()`,
one process-wide `SecureRandom` created on first use under `SEC_LOCK`. When a caller has preset
`SecUtil.SECURE_RANDOM_ALGO` that type is the only candidate; otherwise the
`CryptoConst.SecureRandomType` values are tried in declaration order and the first the JVM
supports wins and is written back into `SECURE_RANDOM_ALGO`. If no candidate is available the
call throws `IllegalStateException` rather than returning null. `SecUtil.randomBytes(n)` is the
convenience over it and refuses `n < 1`. Tests that need providers loaded call `SecUtil.init()`
once (`@BeforeAll`); the random source itself needs no init.

---

## 5. Storage forms

A sealed record has three interchangeable storage forms. All carry the same ten attributes;
opening judges the content, the forms only move it.

| Form | Producer / consumer | Use |
|---|---|---|
| Packed bytes | `CipherCodecs.EDEncoder` / `EDDecoder` | binary column; big-endian, length-prefixed text, ciphertext last with no length (takes the rest) |
| Canonical text | `toCanonicalID()` / `fromCanonicalID()` | text column; the ten fields `\|`-joined in the order of §2.1, binary as base64url, ciphertext last |
| Entity | `GSONUtil` / `APIDataStore` | JSON transport; datastore row field by field |

`CipherCodecs` is content-blind: no key, no decryption, no base64. The decoder checks the
**layout only** (version byte, lengths fit, `data_type` present, at least one ciphertext byte);
an altered byte decodes fine and then fails at open. The encoder **refuses an
`EncapsulatedKey`**: a key row is persisted as an entity, field by field, because packing it
would silently drop the binding fields.

The canonical text form is the associated data string (§2.2) followed by `|` and the base64url
ciphertext. The parser demands exactly ten fields and a non-empty version, refuses anything
else as `IllegalArgumentException`, and decodes the two binary fields with `SharedBase64`
alphabet detection, so a record written with the URL alphabet reads back whichever alphabet a
store echoed. It is a convenience of the record class, not something the subsystem depends on:
`CryptoUtil` and `KeyMakerProvider` never call it, and removing it would change nothing else.
On an `EncapsulatedKey` it is the **inherited ten-field form**: the five key fields, binding
included, are not in it, so it is no more a storage form for a key row than packed bytes are.

No storage form is the associated data; the AAD is rebuilt from the attributes at open time
whichever form the record came from.

---

## 6. The key maker — `KeyMaker` and `KeyMakerProvider`

### 6.1 The chain

Keys form a tree rooted at one **master key**:

```
master key (SecretKey in a KeyStore, loaded once into KeyMakerProvider.SINGLETON)
  └─ subject key      EncapsulatedKey, SUBJECT_ID, reference = the subject itself, key_guid = null
       └─ entity key  EncapsulatedKey, reference = the protected NVEntity, wrapped under the subject key
            └─ EncryptedData records   field values, files, sealed under the entity key's material
```

Each `EncapsulatedKey` is wrapped under the **material** of the key above it, and the master key
wraps the top. A record is opened by walking the chain down: unwrap the subject key under the
master key, unwrap the entity key under the subject key's material, open the record under the
entity key's material. Nothing below the master key is ever stored in clear.

A KEM-wrapped key (§3.4) is a leaf of the same tree whose parent is not a stored key but a
public key: it lets a **third party** mint a key for a subject without holding any of that
subject's chain.

### 6.2 `KeyMakerProvider`

A singleton (`KeyMakerProvider.SINGLETON`) holding the master `SecretKey` and a lookup cache.

| Method | Does |
|---|---|
| `setMasterSecretKey(KeyStore, alias, password)` / `setMasterSecretKey(SecretKey)` | loads the master key once; `getMasterKey()` throws `AccessSecurityException` until set |
| `createSubjectIDKey(subjectID, wrappingKey)` | new row: `subject_guid` and reference = the subject, lock `SUBJECT_ID`, wraps a fresh key under `wrappingKey` (the master key for a top-level subject). Does **not** insert; the caller does |
| `createNVEntityKey(dataStore, nve, wrappingKey)` | looks the entity's key up first (by `reference_guid` + `subject_guid`) and returns it if it exists; otherwise mints a row bound to the entity, wraps under `wrappingKey`, **inserts** it and returns the stored row. One key per entity, minted once. The row is labelled `SUBJECT_ID`, not `NVENTITY`; the label is unbound (§3.2) so this is cosmetic |
| `getKey(dataStore, key, chainedIDs...)` | walks the chain: starting from `key` (or the master key when `key` is null), for each reference GUID in order look the row up and unwrap it under the current material; returns the last material. A missing row is `AccessSecurityException("No key for <id>")` |
| `lookupEncapsulatedKey(dataStore, nve)` / `(dataStore, refGUID, subjectGUID)` / `(dataStore, refGUID)` | cache first, then a meta-driven `dataStore.search` on `EncapsulatedKey.NVCE_ENCAPSULATED_KEY`; exactly one match or null |

Typical call to open a field of entity `E` owned by subject `S`:

```
byte[] entityKey = KeyMakerProvider.SINGLETON.getKey(dataStore, null, S.getGUID(), E.getGUID());
byte[] plain     = CryptoUtil.decryptEncryptedData(record, entityKey);
```

`null` selects the master key as the root; the two GUIDs are the rows to unwrap in order.

### 6.3 The lookup cache

`keyMap` caches rows by `refGUID:subjectGUID` (or `refGUID` alone) after the first datastore
hit. It stores the **wrapped** rows, never material. It is a speed feature: one key per entity is
minted once and never changes identity, and `rekey` rewraps in place, so a cached row stays
valid. It is not a revocation mechanism and must not be described as a revocation bug; a deleted
row simply stops being findable on the next cold lookup.

### 6.4 The contract, `KeyMaker`

The interface lives in `shared` so client-safe code can name it; the only implementation is the
server-side provider. Its method set is exactly §6.2 minus the master-key setters.

---

## 7. Invariants (the list to check before flagging anything)

1. Wrapping and content keys are **≥ 32 random bytes**. There is no password path into the
   record; password handling is `CIPassword` / `SecUtil`, elsewhere.
2. The wrapping key is never used directly; a per-record key is derived by HKDF with the nonce
   as salt. This is why nonce reuse across records is not a concern.
3. Every attribute except the ciphertext is authenticated. Set `mask`, `exp`, `hint`,
   `data_type` before sealing; never edit them after.
4. For a key row, `subject_guid`, `reference_guid`, `key_guid`, `key_size` are additionally
   bound. Set them before wrapping; never edit after. `reference_type` and `key_lock_type` are
   free to change.
5. The entity GUID is never part of any cryptography.
6. Opening order is version/cipher/KDF, tag, length, expiry. A wrong key or an edited expiry
   is tampering; only a genuine expiry is "expired".
7. The associated data is the fixed-order `|`-joined attribute string from `toAssociatedData()`.
   Not JSON, not a map; the field order is the seal. Storage forms come and go around it.
8. Key rows are persisted as entities, never through `CipherCodecs` (refused) or the canonical
   text form (drops the binding fields).
9. AES material is minted once per entity and only rewrapped on `rekey`; the material never
   changes, so records under it never need re-encryption.
10. ML-KEM decapsulation cannot fail; the GCM tag is the only check of a KEM-wrapped key. A
    wrong parameter set is refused by size before any cryptography.
11. `key_size` is the KEM split point; readers split on it, never on a constant.

---

## 8. Tests that pin this design

| Test | Pins |
|---|---|
| `EncryptedDataTest` | record round trip, every attribute bound, expiry ordering, canonical text / packed / JSON forms, wrapped-key round trip, binding vs labels, rekey, HKDF RFC vector |
| `MLKEMKeyWrapTest` | KEM by hand against the DEM, all three parameter sets through the row API, wrong key / wrong set / tampered encapsulation, JSON survival |
| `CryptoUtilTest`, `EncryptedContentTest` | broader `CryptoUtil` surface and content sealing under a wrapped key |
| `CipherCodecsPerfTest` | packed form cost |

Run them through IntelliJ with `-DskipTests=false` semantics (see CLAUDE.md).
