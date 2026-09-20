# Preparar SOLO con una revisión aprobada, nunca con código de una propuesta.
# La base debe incluir SDK 36, JDK compatible, /bin/sh, tar y cp; sin secretos.
ARG SALVE_ANDROID_BASE
FROM ${SALVE_ANDROID_BASE} AS seed
USER root
COPY . /opt/salve/source
RUN mkdir -p /opt/salve/gradle-home \
    && cd /opt/salve/source \
    && GRADLE_USER_HOME=/opt/salve/gradle-home /bin/sh ./gradlew testDebugUnitTest --no-daemon \
    && chmod -R a+rX /opt/salve/gradle-home

FROM ${SALVE_ANDROID_BASE}
COPY --from=seed /opt/salve/gradle-home /opt/salve/gradle-home
USER 65534:65534
