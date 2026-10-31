# syntax=docker/dockerfile:1
# Build only the requested target: generator (default) or llvm23 (object rewriting).
FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852 AS llvm-build
ARG LLVM_APT_SUITE=llvm-toolchain-noble-23
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl cmake g++ make gnupg \
    && curl -fsSL https://apt.llvm.org/llvm-snapshot.gpg.key -o /usr/share/keyrings/llvm.asc \
    && echo "deb [signed-by=/usr/share/keyrings/llvm.asc] https://apt.llvm.org/noble/ ${LLVM_APT_SUITE} main" > /etc/apt/sources.list.d/llvm.list \
    && apt-get update && apt-get install -y --no-install-recommends llvm-23-dev clang-23 lld-23 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY llvm/ llvm/
RUN cmake -S llvm -B output -DLLVM_DIR=/usr/lib/llvm-23/lib/cmake/llvm -DCMAKE_BUILD_TYPE=Release \
    && cmake --build output --parallel 4 && cmake --install output

FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852 AS java-build
ARG TARGETARCH
ARG ELIDE_VERSION=1.6.1
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl xz-utils gcc g++ zlib1g-dev \
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
COPY elide.pkl ./
COPY src/ src/
RUN elide build :jar :native \
    && cp .dev/artifacts/jar/myna/myna.jar myna.jar \
    && cp .dev/artifacts/native-image/myna myna

# Full variant: includes the LLVM 23 tools used to read, annotate, and inspect objects.
FROM llvm-build AS llvm23
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
ENV PATH="/usr/lib/llvm-23/bin:${PATH}"
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]

FROM ubuntu:24.04@sha256:fe81560990fae86dc4743202b2cb4d3df98d0bcf949173cecef15ce1127ef852 AS generator
RUN apt-get update && apt-get install -y --no-install-recommends zlib1g \
    && rm -rf /var/lib/apt/lists/*
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]
