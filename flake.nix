{
  description = "Forge 1.20.1 development environment for Ex Mekanism Deorum";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          jdk = pkgs.jdk17;
          # Native libraries the Minecraft client needs (GLFW/GLX, X11, audio).
          # NixOS keeps the actual GPU drivers in /run/opengl-driver/lib, while
          # libGL/libGLX themselves come from libglvnd; both must be on
          # LD_LIBRARY_PATH for the dev client to start.
          graphicsLibs =
            with pkgs;
            [
              libglvnd
              mesa
              libX11
              libXext
              libXcursor
              libXrandr
              libXxf86vm
              libXi
              libXinerama
              libxkbcommon
              alsa-lib
              libpulseaudio
            ];
        in
        {
          default = pkgs.mkShell {
            packages = [
              jdk
              pkgs.gradle
              pkgs.git
            ] ++ graphicsLibs;

            shellHook = ''
              export JAVA_HOME="${jdk}"
              export PATH="$JAVA_HOME/bin:$PATH"

              # Keep Gradle's caches inside the project so sandboxed/read-only $HOME
              # setups still work.
              export GRADLE_USER_HOME="$PWD/.gradle-user-home"
              export TMPDIR="$GRADLE_USER_HOME/tmp"
              mkdir -p "$TMPDIR"

              # Minecraft's GLFW needs libGLX/libGL (libglvnd) plus the Mesa
              # vendor libraries. Without this, runClient fails with
              # "GLFW error: [0x10006]GLX: Failed to load GLX".
              export LD_LIBRARY_PATH="/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath graphicsLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

              echo "Ex Mekanism Deorum dev shell (all mod dependencies resolve from CurseForge Maven)"
              echo "  build: gradle build | client: gradle runClient | server: gradle runServer"
            '';
          };
        }
      );
    };
}
