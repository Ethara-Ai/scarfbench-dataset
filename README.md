# scarfbench-dataset

Public source-of-truth for [Harbor](https://github.com/harbor-framework)
ScarfBench task apps. The Harbor `scarfbench` adapter clones this repo at
Docker build time and stages three paths inside each task container:

- `/workspace/app` — SOURCE-framework app (agent workspace, harness stripped)
- `/verifier`      — TARGET-framework grader (`smoke.py`, `smoke/`, `test.sh`)
- `/reference`     — TARGET-framework full app (oracle-only, harness stripped)

## Layout

```
apps/
└── <layer>__<app>/
    ├── <framework-A>/     # full upstream app source
    ├── <framework-B>/     # ... one dir per framework the app ships in
    └── <framework-C>/
```

Each `<framework>/` directory is a complete upstream ScarfBench app, including
its harness files (`Dockerfile`, `Makefile`, `test.sh`, `smoke.py`, `smoke/`).
The task Dockerfile strips harness files from `/workspace/app` and `/reference`
at copy time — do NOT strip them here.

## Contributing a new app

1. Copy the full framework directory tree from your local ScarfBench checkout
   into `apps/<layer>__<app>/<framework>/`.
2. Commit and push.
3. Bump `MIRROR_COMMIT_NOTE` in the adapter's `adapter.py` (documentation only —
   the Dockerfile clones HEAD).
4. Regenerate tasks with `uv run scarfbench --overwrite`.

## Consumer

Harbor adapter: `harbor/adapters/scarfbench/` (see its `README.md` for the full
task-generation pipeline).
