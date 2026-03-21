# Builds MiniScaled against the real ImmersivePortals 3.0.7 (Forge 1.20.1) and
# runs the Forge GameTest server to validate the integration tests.
#
# Usage:
#   docker build -t mini_scaled_test .
#   docker run --rm mini_scaled_test
#
# The container exits 0 if all required GameTests pass, non-zero otherwise.

FROM eclipse-temurin:17-jdk-jammy

# Install Maven (needed to install the ImmPTL jar to local Maven).
RUN apt-get update && apt-get install -y --no-install-recommends \
        maven \
        curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# ─── Step 1: Download ImmersivePortals 3.0.7 (Forge 1.20.1, all-loaders jar) ──
# Source: Modrinth project "immersive-portals-neoforge" (project ID zf4Szzx2),
# version 3.0.7 (version ID Q6Z7eOLL).
ARG IMMPTL_VERSION=3.0.7
ARG IMMPTL_URL=https://cdn.modrinth.com/data/zf4Szzx2/versions/Q6Z7eOLL/immersive-portals-3.0.7-all.jar
ARG IMMPTL_SHA1=b4a6d71ca7dc07a1da33e7272befabc6ce95057b

RUN curl -fL -o immptl-all.jar "${IMMPTL_URL}" \
    && echo "${IMMPTL_SHA1}  immptl-all.jar" | sha1sum -c -

# ─── Step 2: Install the single "all" jar to local Maven under both coordinates ─
# ImmPTL 3.x ships as one combined jar containing both imm_ptl_core and
# q_misc_util packages.  ForgeGradle's fg.deobf() expects each as its own Maven
# artifact, so we install the same jar under both groupId:artifactId pairs.
RUN mvn --no-transfer-progress install:install-file \
        -Dfile=immptl-all.jar \
        -DgroupId=qouteall \
        -DartifactId=imm_ptl_core \
        -Dversion=${IMMPTL_VERSION} \
        -Dpackaging=jar \
    && mvn --no-transfer-progress install:install-file \
        -Dfile=immptl-all.jar \
        -DgroupId=qouteall \
        -DartifactId=q_misc_util \
        -Dversion=${IMMPTL_VERSION} \
        -Dpackaging=jar

# ─── Step 3: Copy mod sources ────────────────────────────────────────────────
COPY . /build/mod
WORKDIR /build/mod

# ─── Step 4: Build and run GameTestServer with real ImmPTL deps ──────────────
# Give Gradle enough heap; ForgeGradle downloads Minecraft assets on first run.
ENV GRADLE_OPTS="-Xmx2g"

# First, build the jar (validates compilation against real ImmPTL APIs).
RUN ./gradlew --no-daemon build -PuseRealImmersivePortalsDeps=true

# Then run the headless GameTest server.
# Forge GameTestServer exits with code 0 on success, 1 on test failure.
CMD ["./gradlew", "--no-daemon", "runGameTestServer", "-PuseRealImmersivePortalsDeps=true"]
