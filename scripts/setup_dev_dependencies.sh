#!/usr/bin/env bash

if ! [ -x "$(command -v mariadb)" ]; then
  sudo apt install mariadb-server
  echo "Running 'mysql_secure_installation', this script expects a root account without password protection"
  # If you choose to set a password for the root user then you will have to add it to the
  # mysql statements. eg. `mysql -uroot -pMYPASSWORD -e "..."`
  sudo mysql_secure_installation
fi

echo "Setting up dev database"
sudo -u root /usr/bin/env bash << 'EOF'
  DATABASE=alovoa
  PASSWORD=alovoa
  USER=alovoa

  mysql -uroot -e "CREATE DATABASE ${DATABASE} /*\!40100 DEFAULT CHARACTER SET utf8 */;"
  mysql -uroot -e "CREATE USER '${USER}'@'localhost' IDENTIFIED BY '${PASSWORD}';"
  mysql -uroot -e "GRANT ALL PRIVILEGES ON ${DATABASE}.* TO '${USER}'@'localhost';"
  mysql -uroot -e "FLUSH PRIVILEGES;"
EOF

echo "Setting up test database"
sudo -u root /usr/bin/env bash << 'EOF'
  DATABASE=alovoa_test
  PASSWORD=alovoa_test
  USER=alovoa_test

  mysql -uroot -e "CREATE DATABASE ${DATABASE} /*\!40100 DEFAULT CHARACTER SET utf8 */;"
  mysql -uroot -e "CREATE USER '${USER}'@'localhost' IDENTIFIED BY '${PASSWORD}';"
  mysql -uroot -e "GRANT ALL PRIVILEGES ON ${DATABASE}.* TO '${USER}'@'localhost';"
  mysql -uroot -e "FLUSH PRIVILEGES;"
EOF
