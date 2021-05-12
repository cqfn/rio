#!/bin/bash

set -e

echo "=== ENCRYPTING SECRET ==="
echo "${TEST_SECRET}" > secret.txt
gpg --import artipie.key
rm -f secret.txt.asc
gpg --encrypt --no-tty --batch --trust-model always --armor -r A523A4E3F543C9523B11A72157EFC152646BA430 secret.txt
cat secret.txt.asc
echo "=== ENCRYPTION DONE ==="

exit 1
