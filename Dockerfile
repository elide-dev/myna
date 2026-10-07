# syntax=docker/dockerfile:1
# Build only the requested target: generator (default) or llvm23 (object rewriting).
FROM ubuntu:24.04 AS llvm-build
ARG LLVM_APT_SUITE=llvm-toolchain-noble
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates curl cmake g++ make gnupg \
    && curl -fsSL https://apt.llvm.org/llvm-snapshot.gpg.key -o /usr/share/keyrings/llvm.asc \
    && echo "deb [signed-by=/usr/share/keyrings/llvm.asc] https://apt.llvm.org/noble/ ${LLVM_APT_SUITE} main" > /etc/apt/sources.list.d/llvm.list \
    && apt-get update && apt-get install -y --no-install-recommends llvm-23-dev clang-23 lld-23 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY llvm/ llvm/
RUN cmake -S llvm -B output -DLLVM_DIR=/usr/lib/llvm-23/lib/cmake/llvm -DCMAKE_BUILD_TYPE=Release \
    && cmake --build output --parallel 4 && cmake --install output

FROM ghcr.io/graalvm/native-image-community:25 AS java-build
WORKDIR /build
COPY src/main/java/ src/main/java/
RUN find src/main/java -name '*.java' > sources.txt \
    && javac --release 25 -d classes @sources.txt \
    && jar --create --file myna.jar --main-class dev.elide.myna.cli.Main -C classes . \
    && native-image --no-fallback -O2 -jar myna.jar -o myna

# Full variant: includes the LLVM 23 tools used to read, annotate, and inspect objects.
FROM llvm-build AS llvm23
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
ENV PATH="/usr/lib/llvm-23/bin:${PATH}"
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]

FROM ubuntu:24.04 AS generator
RUN apt-get update && apt-get install -y --no-install-recommends zlib1g \
    && rm -rf /var/lib/apt/lists/*
COPY --from=java-build /build/myna /usr/local/bin/myna
COPY --from=java-build /build/myna.jar /opt/myna/myna.jar
WORKDIR /work
ENTRYPOINT ["/usr/local/bin/myna"]
CMD ["--help"]
