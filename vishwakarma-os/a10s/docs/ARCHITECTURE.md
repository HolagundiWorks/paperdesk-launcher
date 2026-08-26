# PureAOSP CAD Reader Architecture

Status: v1.0 baseline  
Target: Samsung Galaxy A10s `SM-A107F/DS`

## Goals and constraints

PureAOSP CAD Reader is a dedicated offline drawing appliance. It must keep source drawings immutable, calculate CAD measurements from world geometry, retain auditable quantity provenance, run within A10s resource limits, fail safely on malformed files, and keep the domain engines reusable on desktop. It has no cloud, Google-account, analytics, advertising, or telemetry dependency.

## System context

```text
Windows workstation                 A10s appliance
┌──────────────────┐      USB      ┌────────────────────────┐
│ PureAOSP Sync    │◄─────────────►│ USB Sync Service       │
│ Project library  │               │ Reader applications    │
└──────────────────┘               │ Local project database │
                                   └────────────────────────┘
```

ADB is a development facility only. Production application sync requires device-side authorization and exposes no general-purpose shell.

## Layers and dependency rule

```text
Presentation: Launcher, Library, Viewer, MB, Sync
                         ↓
Use cases: inspect, measure, annotate, quantify
                         ↓
CADCore + Measurement | MB Engine | Sync Engine
                         ↓
Repository and platform interfaces
                         ↓
Android framework → kernel/vendor/device
```

Dependencies point toward domain interfaces. CADCore, Measurement, MB, Project Model, and Sync Protocol must not import Android UI classes. Android and desktop implementations sit behind rendering, storage, database, clock, and transport interfaces.

Target repository boundaries:

```text
android/{apps,services}/
core/{cad-ir,dxf-import,dwg-import,geometry,spatial-index,
      measurement,annotation,measurement-book,project-model,sync-protocol}/
desktop/sync-app/
device/samsung/a10s/
docs/  testdata/  tools/
```

The Samsung Root Manager is development tooling, not part of the production ROM.

## Ownership

| Component | Owns | Must not own |
|---|---|---|
| Importers | parsing, validation, conversion to CAD IR | UI and measurement records |
| CAD IR | stable entity/geometry vocabulary | Android or parser-specific objects |
| Spatial index | viewport/proximity candidate queries | exact snap selection |
| Renderer | culling and drawing | authoritative calculations |
| Measurement | exact geometry and provenance | pixel-scale guesses |
| Annotation | independent overlays | source-file modification |
| MB Engine | formulas, deductions, quantities, audit links | CAD parsing |
| Project Repository | atomic structured persistence | silent source mutation |
| Sync Engine | compare, stage, verify, commit | domain calculations |

## CAD import and IR

The versioned parser-neutral IR contains `Drawing`, `Layout`, `Layer`, `Block`, and `Entity`. An entity has stable ID, type, layer ID, geometry, style, optional source handle, bounds, and properties. Initial geometry includes line, polyline, arc, circle, ellipse, spline, text, insert, dimension, and hatch. Safe metadata for an unknown entity becomes an `Unsupported` record; it is not a fatal error.

```text
immutable source → type/hash → isolated bounded parser → validation
→ normalized IR stream → bounds/spatial index → drawing snapshot
→ viewport/layer query → renderer
```

Parsing is off the UI thread and moves to a separate process before production. Limits cover recursion, entities, allocation, decompression, and block expansion. The UI never creates one view object per CAD entity.

PDF uses a separate adapter and marks each page vector, raster, or mixed. Raster measurement requires explicit calibration and is recorded as calibrated-image provenance, never CAD-derived accuracy.

## Coordinates, snapping, and measurements

The viewport owns a reversible world/screen transform:

```text
touch → world point → world tolerance → spatial candidates
      → exact tests → ranked snap → measurement engine
```

Initial snaps: endpoint, midpoint, center, intersection, perpendicular, nearest. Ranking is deterministic by enabled-mode priority, geometric distance, draw order, and stable ID.

Persisted measurements retain drawing ID/revision/source hash, entity IDs/source handles, world points, operation parameters, result/unit, algorithm version, timestamps, and note. CAD results never originate from screen-pixel distance.

## Measurement Book

The MB engine uses decimal arithmetic, explicit units, and recorded rounding policy. It keeps operands, formula, deductions, and result.

```text
MB item → measurement → entity → drawing revision → source hash
```

A new drawing revision makes references stale. Rebinding is explicit and audited; historic quantities never change silently.

## Persistence and project container

SQLite stores transactional metadata; drawings and derived caches remain files. Foreign keys are enabled and migrations are ordered and transactional.

```text
PROJECT/
  manifest.json
  drawings/             # immutable originals
  metadata/project.db
  annotations/
  measurements/
  measurement-book/
  cache/                # disposable, never authoritative
```

The manifest records schema version, project ID, paths, sizes, SHA-256 hashes, media types, and drawing revisions. Import rejects traversal paths, links, absolute paths, duplicate normalized names, and oversized archives.

## USB sync

```text
DISCONNECTED → HELLO → AUTHORIZED → COMPARED → TRANSFERRING
             → VERIFIED → COMMITTED → ACKNOWLEDGED
```

Messages are length-delimited and versioned. Data writes to staging, verifies size/hash, validates the manifest/database, then commits atomically. An interrupted transfer leaves the prior project intact. Conflicts require keep-local, keep-remote, or duplicate-as-revision; last-write-wins is not the default. A development ADB transport may be used initially, but protocol semantics cannot depend on ADB.

## Resilience, performance, and security

- Parser errors return structured diagnostics and cannot crash the launcher.
- Transactions/checkpoints protect edits; caches are disposable.
- Memory pressure cancels decoding and evicts off-screen pages first.
- Loading, indexing, database access, and rendering work are cancellable.
- Viewport/layer culling occurs before tessellation; blocks expand lazily.
- Production uses enforcing SELinux, signed images, ADB off by default, restricted USB, and bounded isolated parsing.
- Hardware reduction is staged only after boot, display, touch, storage, USB, power, and recovery are stable.

## Verification gates

| Level | Evidence |
|---|---|
| Unit | geometry, snaps, units, formulas, deductions, migrations |
| Robustness | corrupt/truncated files, recursion/allocation limits, fuzz corpus |
| Integration | file→IR→measure; measurement→MB; stage→sync commit |
| Device | repeated boot, touch, USB, suspend, storage pressure, battery |
| Acceptance | architectural, structural, MEP, vector and scanned PDF |

Golden tests use known world-coordinate answers. Screenshots validate rendering, never measurement correctness.

## Decisions requiring prototypes

DWG library/license, Canvas versus GPU renderer, grid versus packed R-tree, parser-process boundary, production USB transport, and shared-core language must each receive an ADR backed by measurements on the physical A10s. No prototype becomes a dependency merely because it builds.
