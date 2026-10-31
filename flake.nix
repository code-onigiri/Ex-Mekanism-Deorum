{
  description = "NeoForge 1.21.1 mod development environment for a mod that requires Ex Deorum and Mekanism";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { self, nixpkgs }:
    let
      lib = nixpkgs.lib;
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];
      forAllSystems = lib.genAttrs systems;

      spec = import ./mods.nix;

      # Each pinned jar becomes a Nix store path; the very same path is used by
      # `nix build .#mods` and by the dev shell (EXDEORUM_JAR / MEKANISM_JAR).
      modJars =
        pkgs:
        lib.listToAttrs (
          map (mod: {
            name = mod.id;
            value = pkgs.fetchurl {
              name = mod.fileName;
              url = mod.url;
              hash = mod.hash;
            };
          }) spec.mods
        );
    in
    {
      packages = forAllSystems (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          jars = modJars pkgs;
          mods = pkgs.linkFarm "ex-mekanism-deorum-mods" (
            map (mod: {
              name = mod.fileName;
              path = jars.${mod.id};
            }) spec.mods
          );
        in
        jars // { inherit mods; default = mods; }
      );

      devShells = forAllSystems (
        system:
        let
          pkgs = nixpkgs.legacyPackages.${system};
          jars = modJars pkgs;
          jdk = pkgs.jdk21;
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

              # Gradle caches live inside the project: sandboxed builds (and
              # read-only $HOME setups in general) cannot write to ~/.gradle.
              export GRADLE_USER_HOME="$PWD/.gradle-user-home"
              export TMPDIR="$GRADLE_USER_HOME/tmp"
              mkdir -p "$TMPDIR"

              # Read by build.gradle as the required mod dependencies.
              export EXDEORUM_JAR="${jars.exdeorum}"
              export MEKANISM_JAR="${jars.mekanism}"
              # Development-only: lets the dev client display Ex Deorum recipes
              # and our machines' JEI catalyst entries.
              export JEI_JAR="${jars.jei}"

              # Minecraft's GLFW needs libGLX/libGL (libglvnd) plus the Mesa
              # vendor libraries. Without this, runClient fails with
              # "GLFW error: [0x10006]GLX: Failed to load GLX".
              export LD_LIBRARY_PATH="/run/opengl-driver/lib:${pkgs.lib.makeLibraryPath graphicsLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"

              echo "Ex Mekanism Deorum mod dev shell"
              echo "  Minecraft ${spec.minecraftVersion} / NeoForge ${spec.neoforgeVersion}"
              echo "  Ex Deorum: $EXDEORUM_JAR"
              echo "  Mekanism:  $MEKANISM_JAR"
              echo "  JEI:       $JEI_JAR (dev only)"
              echo "  build: gradle build | client: gradle runClient | server: gradle runServer"
            '';
          };
        }
      );

      checks = forAllSystems (system: {
        mods = self.packages.${system}.mods;
      });
    };
}
