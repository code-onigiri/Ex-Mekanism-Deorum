#!/usr/bin/env python3
"""End-to-end smoke test for the Ex Mekanism Deorum machines.

Starts a dedicated dev server (nix dev shell), places one machine of each family, feeds it
via /data merge block with the flat Mekanism container NBT format, waits, and prints the
resulting block NBT.  Requires `nix develop` and the mod jars pinned in mods.nix.

Usage:
    python3 tools/smoke_test.py [--keep-world]

The test world is deleted first, so every run starts clean.
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
import threading
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORLD = os.path.join(ROOT, "run", "world")


class Server:
    def __init__(self) -> None:
        env = os.environ.copy()
        env["XDG_CACHE_HOME"] = os.path.join(ROOT, ".nix-cache")
        self.proc = subprocess.Popen(
            ["nix", "develop", "--command", "gradle", "--no-daemon", "runServer", "--console=plain"],
            cwd=ROOT,
            env=env,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        self.lines: list[str] = []
        self.cond = threading.Condition()
        self.reader = threading.Thread(target=self._read, daemon=True)
        self.reader.start()

    def _read(self) -> None:
        assert self.proc.stdout is not None
        for line in self.proc.stdout:
            with self.cond:
                self.lines.append(line.rstrip("\n"))
                self.cond.notify_all()

    def wait_for(self, pattern: str, timeout: float = 300.0) -> bool:
        deadline = time.time() + timeout
        rx = re.compile(pattern)
        with self.cond:
            while time.time() < deadline:
                if any(rx.search(line) for line in self.lines):
                    return True
                self.cond.wait(timeout=1.0)
        return False

    def send(self, command: str, wait: float = 1.0) -> None:
        assert self.proc.stdin is not None
        self.proc.stdin.write(command + "\n")
        self.proc.stdin.flush()
        time.sleep(wait)

    def stop(self) -> None:
        try:
            self.send("stop", wait=5)
            self.proc.wait(timeout=120)
        except Exception:
            self.proc.kill()

    def dump(self, path: str) -> None:
        with self.cond:
            text = "\n".join(self.lines)
        with open(path, "w", encoding="utf-8") as handler:
            handler.write(text)
        print(text[-20000:])


def main() -> int:
    if "--keep-world" not in sys.argv:
        shutil.rmtree(WORLD, ignore_errors=True)

    server = Server()
    if not server.wait_for(r"Done \("):
        print("server did not finish starting")
        server.stop()
        server.dump("/tmp/smoke_test.log")
        return 1

    server.send("forceload add -16 -16 32 32", 2)
    server.send("time set day", 0.5)
    server.send("gamerule doDaylightCycle false", 0.5)

    # hammer (basic): 2 cobblestone -> gravel
    server.send("setblock 0 100 0 exmekdeorum:hammer_machine[facing=north]", 0.5)
    server.send('data merge block 0 100 0 {energy_containers:[{container:0b,stored:40000L}],'
                'items:[{slot:0b,item:{id:"minecraft:cobblestone",count:2}}]}', 0.5)
    # sieve (basic): 2 gravel + string mesh in slot 22
    server.send("setblock 5 100 0 exmekdeorum:sieve_machine[facing=north]", 0.5)
    server.send('data merge block 5 100 0 {energy_containers:[{container:0b,stored:40000L}],'
                'items:[{slot:0b,item:{id:"minecraft:gravel",count:2}},'
                '{slot:22b,item:{id:"exdeorum:string_mesh"}}]}', 0.5)
    # barrel (basic): water 1000 + snowball -> ice
    server.send("setblock 10 100 0 exmekdeorum:barrel_machine[facing=north]", 0.5)
    server.send('data merge block 10 100 0 {energy_containers:[{container:0b,stored:40000L}],'
                'items:[{slot:0b,item:{id:"minecraft:snowball",count:1}}],'
                'fluid_tanks:[{tank:0b,stored:{id:"minecraft:water",amount:1000}}]}', 0.5)
    # sieve factory (basic, 3 parallel): 3 gravel + string mesh -> 3 results
    server.send("setblock 20 100 0 exmekdeorum:basic_sieve_factory[facing=north]", 0.5)
    server.send('data merge block 20 100 0 {energy_containers:[{container:0b,stored:40000L}],'
                'items:[{slot:0b,item:{id:"minecraft:gravel",count:3}},'
                '{slot:22b,item:{id:"exdeorum:string_mesh"}}]}', 0.5)
    # crucible (basic): 2 cobblestone + fire below -> lava
    server.send("setblock 15 99 0 minecraft:fire", 0.5)
    server.send("setblock 15 100 0 exmekdeorum:crucible_machine[facing=north]", 0.5)
    server.send('data merge block 15 100 0 {energy_containers:[{container:0b,stored:40000L}],'
                'items:[{slot:0b,item:{id:"minecraft:cobblestone",count:2}}]}', 0.5)

    print("waiting 30 s for the machines to run...")
    time.sleep(30)
    for pos in ("0 100 0", "5 100 0", "10 100 0", "15 100 0", "20 100 0"):
        server.send(f"data get block {pos}", 1.0)

    server.stop()
    server.dump("/tmp/smoke_test.log")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
