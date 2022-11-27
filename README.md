## SSCAIT Tournament Manager

### Host

Should run on a host OS, together with the tournament's database and web.

Requires Java and a MySQL database with tables defined in database.sql file.

The host listens for TCP connections from the clients.

### Client

There should be two instances of a Windows 7 virtual machines running on a host. Each of them should have an installation of Java, so it can run the Java client instance.

The client running on each VM needs to be able to connect to the host (running on a host machine).
