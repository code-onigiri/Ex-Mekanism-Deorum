# Optional pinned dependency jars for offline / reproducible builds.
#
# build.gradle resolves dependencies from CurseForge Maven (cursemaven.com) by
# default: Ex Deorum, Mekanism, JEI and Mek Energistics. This file still pins the
# exact jars so the Nix dev shell can export EXDEORUM_JAR / MEKANISM_JAR / JEI_JAR /
# MEKENERGISTICS_JAR for offline builds; when those variables are set they take
# priority over the Maven coordinates (use -PuseMavenDeps to force Maven).
#
# Ex Deorum and Mekanism are *required* dependencies of this mod; they are
# declared as `type = "required"` in src/main/resources/META-INF/neoforge.mods.toml.
#
# Mek Energistics is a development-only, optional compatibility target: its pattern
# automation API is used to compile the integration (compileOnly, never bundled).
#
# JEI is a development-only convenience: it is declared as an optional
# dependency in neoforge.mods.toml, put on the classpath via JEI_JAR so the
# dev client can show Ex Deorum recipes, and is NOT bundled with the mod.
#
# Their own loader-level requirements, as declared in META-INF/neoforge.mods.toml:
#
#   Ex Deorum 3.12       -> neoforge [21.1,),      minecraft [1.21.1]
#   Mekanism 10.7.19.85  -> neoforge [21.1.194,),  minecraft [1.21.1]
#
# NeoForge 21.1.265 satisfies both ranges.
#
# Note: the Mekanism jar's own [[mods]] entry declares version "10.7.19"; the
# file name adds the build number 85. Dependency ranges therefore have to use
# 10.7.19 (see mekanism_version_range in gradle.properties).
{
  minecraftVersion = "1.21.1";
  neoforgeVersion = "21.1.265";

  mods = [
    {
      id = "exdeorum";
      fileName = "exdeorum-3.12.jar";
      version = "3.12";
      url = "https://cdn.modrinth.com/data/WP0FLyzv/versions/NOZ1Zbbu/exdeorum-3.12.jar";
      # SRI hash used by fetchurl
      hash = "sha512-4Dl+Lzc0pU8DbMEtG9Pf1q+9E0Sb7TNzXlLVdq2b7XGbIFYQNyIETz7t+OqommAwPBGh50HJLySkBKys6APTUA==";
      # Hex digests kept for reference / external tooling
      sha1 = "4e1c95351ac00995eaacf02994a26c0f9ad9acd3";
      sha512 = "e0397e2f3734a54f036cc12d1bd3dfd6afbd13449bed33735e52d576ad9bed719b2056103722044f3eedf8eaa89a60303c11a1e741c92f24a404acace803d350";
      size = 2123067;
    }

    {
      id = "mekanism";
      fileName = "Mekanism-1.21.1-10.7.19.85.jar";
      version = "10.7.19.85";
      url = "https://cdn.modrinth.com/data/Ce6I4WUE/versions/5KzzycBT/Mekanism-1.21.1-10.7.19.85.jar";
      hash = "sha512-ZnRYJTMKmPPkpeo6RK/4sAhw9xXBRO3DjdL2G0JAWJYAtYronvrDtU27WqQwsQWdrBoo/XHKxcyQArvrW6PyKw==";
      sha1 = "b78945c40cfe7640408f3fd1e44da385a8c8b805";
      sha512 = "66745825330a98f3e4a5ea3a44aff8b00870f715c144edc38dd2f61b4240589600b58ae89efac3b54dbb5aa430b1059dac1a28fd71cac5cc9002bbeb5ba3f22b";
      size = 11976009;
    }

    {
      id = "jei";
      fileName = "jei-1.21.1-neoforge-19.54.0.429.jar";
      version = "19.54.0.429";
      url = "https://cdn.modrinth.com/data/u6dRKJwZ/versions/BBfxXwba/jei-1.21.1-neoforge-19.54.0.429.jar";
      hash = "sha512-Th7HCjHgMVjWIQwuwg0oawDdI1skAKd+DPL7tL3e3Sn1ebyst5a7BaLHDaW4FaZJio+trvAkMgkbnNtlQhLvOQ==";
      sha1 = "d3acaea7df222faa05598a00e6d6d439da0400fe";
      sha512 = "4e1ec70a31e03158d6210c2ec20d286b00dd235b2400a77e0cf2fbb4bddedd29f579bcacb796bb05a2c70da5b815a6498a8fadaef02432091b9cdb654212ef39";
      size = 1856734;
    }

    {
      id = "mekenergistics";
      fileName = "mekenergistics-3.0.8.jar";
      version = "3.0.8";
      url = "https://github.com/beipuo/Mek-Energistics/releases/download/3.0.8/mekenergistics-3.0.8.jar";
      hash = "sha256-ktn+zUfAAYgMKD+dwZVteT+XF+58wHayY/Lu0hdBFLE=";
      sha1 = "6055b89f995aff83a74e2465594af048fdf9be7e";
      sha512 = "19ebae18cc863a3b5511fc5842d24d2d37b8f95dc8b883eed0bb350d5eb8ff6e006a9c816d322649c146efa7ed44fbfe86c68d29a90097438108961a987fe7a8";
      size = 2254434;
    }
  ];
}
