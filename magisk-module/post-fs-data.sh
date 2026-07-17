#!/system/bin/sh
# Runs early (post-fs-data). Prepares the paths Cloudy stages into and drops a readiness marker
# the app checks via RootManager.cloudyModulePresent().
MODDIR=${0%/*}

# Make sure the recovery command dir exists and is writable for the staging step.
mkdir -p /cache/recovery 2>/dev/null
chmod 0771 /cache/recovery 2>/dev/null

# Ensure the staging area under internal storage exists.
mkdir -p /data/media/0/cloudy 2>/dev/null

# Readiness marker.
touch "$MODDIR/cloudy_ready"
