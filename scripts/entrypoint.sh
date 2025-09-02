#!/bin/sh
set -eu

# Wczytaj sekrety z /run/secrets jeśli istnieją
read_secret() {
  name="$1"
  file="/run/secrets/$name"
  if [ -f "$file" ]; then
    val=$(cat "$file")
    export "$name"="$val"
  fi
}

read_secret SECURITY_JWT_SECRET
read_secret SECURITY_ENC_MASTER_KEY
read_secret SECURITY_ENC_ACTIVE_KEY_ID

# Ustaw log4j2 w kontenerze na JSON (classpath)
JAVA_OPTS="-Dlog4j2.configurationFile=classpath:log4j2-docker.xml ${JAVA_OPTS:-}"

exec sh -c "java $JAVA_OPTS -jar app.jar"
