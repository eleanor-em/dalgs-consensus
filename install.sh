sudo apt update
sudo apt upgrade -y
sudo apt install -y openjdk-11-jdk cl-sql-sqlite3 sqlite sqlitebrowser maven --fix-missing
mvn package