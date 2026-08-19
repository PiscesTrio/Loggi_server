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

---

# S10 changed it again, on purpose

S09 changed the wire because the entities *were* the wire. S10 puts a boundary in, so from
here the shape is a decision rather than a consequence. Where S10 differs from S09, S10 is
what the client should be written against.

## Login

The response was a `Map` holding "admin" and "token"; it is a declared type now, and the
administrator inside it carries only `id`, `email`, `roles` and `createAt`.

Requests are validated: a blank or malformed `email` comes back **400** with the reason in
`msg` (`邮箱不能为空`, `邮箱格式不正确`) instead of being reported as wrong credentials.

## `GET /api/distribution`

| S09 | S10 |
| --- | --- |
| `driver: {…the whole Driver row…}` | `driver: { id, name, phone }` |
| `vehicle: {…the whole Vehicle row…}` | `vehicle: { id, number, type }` |
| `warehouse: {…the whole Warehouse row…}` | `warehouse: { id, name }` |

Each reference is a summary carrying what a screen renders. The rest never leaves the server.

## `POST /api/distribution`

Takes ids, not objects:

```
{ "driverId": "...", "vehicleId": "...", "warehouseId": "...",
  "phone": "...", "address": "...", "urgent": false, "care": "...",
  "time": "2026-08-20 10:00:00", "status": "REVIEWING",
  "fromLat": 35.672, "fromLng": 139.817, "toLat": 33.620, "toLng": 130.427 }
```

**No `id`.** Sending one is what made creating an order fail for the entire life of the
project: Hibernate read a non-null id as "an existing row to update".

Validated: driver, vehicle, phone, address, time and status are required; coordinates are
bounded to real latitudes and longitudes, which is what an origin of `0,0` was not.

## `GET|POST /api/distribution/status`

`distribution` becomes **`distributionId`**, and — the part that matters — the request now
uses the same shape as the response. Under S09 the response carried a bare id while the
request had to send an object.

`POST` no longer accepts `time`. The server records when the sighting arrived; a
client-supplied timestamp on a tracking record is the client asserting where a vehicle was
and when, which is the one thing the record exists to state independently.

## `GET /api/distribution/can`

Was a map with "drivers" and "vehicles". Now a declared type with the same two fields, each
a list of summaries — the lists are always present, so `available.drivers!.isEmpty` has
nothing left to force-unwrap.

## Everything else, in one table

Every endpoint now speaks in request and view types. Beyond the transport ones above:

| Resource | Change |
| --- | --- |
| all lists | entities replaced by view types; no field reaches a client unless it is declared |
| `Driver`, `Employee` | **`idCard` removed.** A personal identification number was being sent to every authenticated caller for a field no screen displays |
| `Driver`, `Vehicle` | `driving` is no longer accepted on create - it follows from approving and completing orders |
| all creates | **no `id` in the request body.** An id on a create is read by Hibernate as an existing row to update |
| all creates | no `createAt` / `updateAt` in the request - auditing overwrites them |
| `Commodity`, `Employee` | `PUT /{id}` instead of `PUT` with the id inside the body |
| all deletes | `DELETE /{id}` instead of `DELETE?id=x` |
| `Sale.count` | already an integer since S09; the request now requires it to be positive |

## Status codes

| Operation | Before | After |
| --- | --- | --- |
| create | `200` | **`201`**, and the envelope's `code` says 201 too |
| delete | `200` with an envelope | **`204`, with no body at all** |
| validation failure | (nothing was validated) | **`400`** with the field's own message in `msg` |

The envelope's `code` used to be hardcoded to 200 on every success. It follows the real
status now. `204` is the one response with no envelope, because a body contradicting a No
Content status is worse than an inconsistency.

## Pagination

`GET /api/loginlog` and `GET /api/systemlog` return a page, not a list:

```
{ "items": [ … ], "page": 0, "size": 20, "totalItems": 137, "totalPages": 7 }
```

They accept `page` and `size`, and default to twenty, newest first. **These two only** —
the other lists are bounded and wrapping them would make every caller unwrap something to
find what it already had.

## `businessType` again

S09 changed it from the Chinese label to the enum name in the database. S10 found the API
was still sending the label: Jackson 3 serialises enums through `toString()` where Jackson
2 used `name()`, and the enum had a `toString()` returning its label. The override is gone,
so the wire and the database finally agree — `QUERY`, not `查询`.

## Not changed

Paths, the response envelope (`{code, status, data, msg}`), HTTP verbs, authentication, and
the `Driver` / `Vehicle` / `Warehouse` / `LoginLog` payloads apart from the timestamp format
and the added `updateAt`.
