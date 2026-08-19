# Contract changes

What the API sends changed in S09, and the Flutter client has not been updated. That is
deliberate: S09 rebuilt the domain model, S10 puts a DTO/VO boundary in front of it, and the
frontend is realigned in its own slice once the contract has settled. This file is what makes
that gap survivable — without it, realigning the client becomes an exercise in discovering
what broke rather than applying a list.

Every shape below was **measured** against a running server, not read off the entities.

## Why the client is not updated here

The entities are still the wire format. Changing the model therefore changes the JSON, and
there is no boundary yet at which to hold it steady. Adding one is exactly what the next slice
does — which is also why the awkward parts recorded here (see *Asymmetry* below) are worth
leaving visible rather than papering over: they are the argument for that boundary.

## Timestamps, everywhere

| Before | After |
| --- | --- |
| `"createAt": "2026-08-01 09:00:00"` | `"createAt": "2026-08-01T09:00:00"` |

The columns were `varchar` holding hand-formatted text; they are `LocalDateTime` now, so
Jackson emits ISO-8601. **Three fields keep the old format** because they carry an explicit
`@JsonFormat`: `Distribution.time`, `DistributionTrack.time`, `SystemLog.time`.

`updateAt` now appears on every entity. It was absent from `Admin`, `Sale`, `Vehicle`,
`Warehouse` and `InventoryRecord`, and both fields were absent from `Distribution` and
`Inventory`.

A separate defect was found and fixed while measuring this: the driver was shifting
wall-clock values by a time zone, so the API answered `10:00:00` for a row stored as
`09:00:00`. The client will have seen shifted times for as long as `system_log.time` has
existed. See the commit for the details.

## `GET /api/distribution`

| Before | After |
| --- | --- |
| `did: "seed-dr-1"` | `driver: { id, name, phone, … }` — the whole row |
| `vid: "seed-vh-1"` | `vehicle: { id, number, type, … }` |
| `wid: "東京江東倉庫"` (a **name**) | `warehouse: { id, name, lat, lng, … }` |
| `driver: "田中 三郎"` (a copied name) | gone — read `driver.name` |
| `number: "品川800へ12-34"` (copied plate) | gone — read `vehicle.number` |
| `status: 1` | `status: "REVIEW_SUCCESS"` |
| — | `createAt`, `updateAt` added |

Values for `status`: `REVIEWING`, `REVIEW_SUCCESS`, `END` (were `0`, `1`, `2`).

Note the client's apply form built `wid` from the warehouse **name**, not its id. The
migration resolves that by name; new requests must send the warehouse by id.

## `GET|POST /api/distribution/status`

| Before | After |
| --- | --- |
| `disId: "seed-dis-1"` | `distribution: "seed-dis-1"` — same id, new key |
| `status: 0` | `status: "REVIEWING"` |

## `GET /api/inventory/warehouse/{id}`

| Before | After |
| --- | --- |
| `wid: "seed-wh-tokyo"` | `warehouse: "seed-wh-tokyo"` |
| `cid: "seed-cm-1"` | `commodity: "seed-cm-1"` |
| — | `createAt`, `updateAt` added |

`name` is still the denormalised commodity name the UI renders.

## `POST /api/inventory/in` and `/out`

Same key changes as above, plus:

| Before | After |
| --- | --- |
| `type: 1` / `type: -1` | `type: "IN"` / `type: "OUT"` |

## `GET /api/inventory/analyze`

The query parameter changed with the enum: `?type=1` → `?type=IN`, `?type=-1` → `?type=OUT`.
The client currently sends `1` / `-1` from a boolean called `inOrOut`.

## `GET /api/systemlog`

| Before | After |
| --- | --- |
| `businessType: "查询"` | `businessType: "QUERY"` |

Values: `OTHER`, `QUERY`, `INSERT`, `UPDATE`, `DELETE`, `EXPORT`, `FORCE`. The Chinese labels
were being stored in the database; they are display text and belong in the UI, so the client
now owns that mapping.

## `Admin`

| Before | After |
| --- | --- |
| `roles: "ROLE_SUPER_ADMIN"` (semicolon-joined) | `roles: ["ROLE_SUPER_ADMIN"]` |

## `Sale`

| Before | After |
| --- | --- |
| `count: "3"` (a string) | `count: 3` |

`price` is unchanged on the wire — `BigDecimal` serialises as a JSON number, same as `double`.

## Asymmetry to be aware of

For the associations that serialise as a bare id — `DistributionTrack.distribution`,
`Inventory.warehouse` / `.commodity`, `InventoryRecord.warehouse` / `.commodity` — **reading
gives an id but writing requires an object**:

```
response:  { "distribution": "seed-dis-1", … }
request:   { "distribution": { "id": "seed-dis-1" }, … }
```

`@JsonIdentityReference` can write an association as an id but cannot read a lone id back; it
resolves ids only against objects already present in the same payload. This is not a decision
so much as the limit of using an entity as a wire format, and it is one of the things the DTO
boundary in the next slice exists to remove.

## Not changed

Paths, the response envelope (`{code, status, data, msg}`), HTTP verbs, authentication, and
the `Driver` / `Vehicle` / `Warehouse` / `LoginLog` payloads apart from the timestamp format
and the added `updateAt`.
