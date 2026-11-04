# syntax=docker/dockerfile:1
# Build only the requested target: generator (default) or llvm23 (object rewriting).
FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852 AS native-toolchain
ARG TARGETARCH
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl cmake make xz-utils python3 zlib1g-dev libzstd-dev libxml2-dev \
    && rm -rf /var/lib/apt/lists/*
RUN case "$TARGETARCH" in \
      amd64) digest=4610627acc96f647776fd4256b3c1fce04c14b2757df692d8923ec299aed0e58; triple=x86_64-unknown-linux-gnu ;; \
      arm64) digest=b6c73becf52e8491c0aad2d1bb5ac6185a7e0c931e1206c62652b1e47fb26785; triple=aarch64-unknown-linux-gnu ;; \
      *) exit 1 ;; esac \
    && curl -fSL "https://github.com/elide-dev/toolchain/releases/download/v2026.11.1/elide-toolchain-2026.11.1-linux-${TARGETARCH}.tar.xz" -o /tmp/toolchain.tar.xz \
    && echo "$digest  /tmp/toolchain.tar.xz" | sha256sum -c - \
    && tar -xJf /tmp/toolchain.tar.xz -C /opt && rm /tmp/toolchain.tar.xz \
    && printf '#!/bin/sh\nexec /opt/elide-toolchain/bin/%s-clang "$@"\n' "$triple" > /opt/myna-cc \
    && printf '#!/bin/sh\nexec /opt/elide-toolchain/bin/%s-clang++ "$@"\n' "$triple" > /opt/myna-cxx \
    && chmod +x /opt/myna-cc /opt/myna-cxx
ENV PATH="/opt/elide-toolchain/bin:${PATH}" \
    CC=/opt/myna-cc CXX=/opt/myna-cxx LLVM_CONFIG=/opt/elide-toolchain/bin/llvm-config \
    LLVM_BIN=/opt/elide-toolchain/bin ELIDE_TOOLCHAIN_HOME=/opt/elide-toolchain

FROM native-toolchain AS llvm-build
COPY LICENSE LICENSE-APACHE LICENSE-MIT /usr/share/licenses/myna/
WORKDIR /build
COPY llvm/ llvm/
RUN cmake -S llvm -B output -DLLVM_DIR=/opt/elide-toolchain/lib/cmake/llvm -DCMAKE_BUILD_TYPE=Release \
    && cmake --build output --parallel 4 && cmake --install output

FROM native-toolchain AS java-build
ARG TARGETARCH
ARG ELIDE_VERSION=1.6.2
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl xz-utils \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/elide \
    && cd /tmp \
    && curl -fsSLO "https://github.com/elide-dev/elide/releases/download/v${ELIDE_VERSION}/elide.linux-${TARGETARCH}.txz" \
    && curl -fsSLO "https://github.com/elide-dev/elide/releases/download/v${ELIDE_VERSION}/elide.linux-${TARGETARCH}.txz.sha256" \
    && sha256sum -c "elide.linux-${TARGETARCH}.txz.sha256" \
    && tar -xJf "elide.linux-${TARGETARCH}.txz" -C /opt/elide \
    && rm elide.linux-*.txz*
ENV PATH="/opt/elide/bin:${PATH}"
WORKDIR /build
COPY tools/graalvm.json tools/graalvm.json
COPY scripts/setup-graalvm.py scripts/setup-graalvm.py
RUN python3 scripts/setup-graalvm.py
ENV GRAALVM_HOME=/build/.dev/toolchains/graalvm JAVA_HOME=/build/.dev/toolchains/graalvm
ENV PATH="/build/.dev/toolchains/graalvm/bin:${PATH}"
COPY scripts/build.sh scripts/symbols.sh scripts/
COPY elide.pkl LICENSE LICENSE-APACHE LICENSE-MIT ./
COPY src/ src/
RUN scripts/build.sh \
    && cp build/dist/myna.jar myna.jar \
    && cp build/dist/myna myna

# Full variant: includes the LLVM 23 tools used to read, annotate, and inspect objects.
FROM llvm-build AS llvm23
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
ENV PATH="/opt/elide-toolchain/bin:${PATH}"
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]

FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852 AS generator
COPY LICENSE LICENSE-APACHE LICENSE-MIT /usr/share/licenses/myna/
RUN apt-get update && apt-get install -y --no-install-recommends zlib1g \
    && rm -rf /var/lib/apt/lists/*
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]
