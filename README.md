ZooCreeper
==========

Background
----------

This project provides a command-line utility which can be used to back up
ZooKeeper data to a JSON file. It supports the following features:

*   Ignores ephemeral nodes when backing up by default.
*   Able to ignore ZooKeeper paths using '--exclude' regular expressions.
*   Optional compression of the backup file using GZIP.

Build
-----

This project requires Maven 3.0 and Java 6+ to build. To build, run this
command in the top-level directory:

    $ mvn clean package

This will create an executable, shaded jar file name
target/zoocreeper-1.0-SNAPSHOT.jar.

Usage
-----

To see the available options for creating a backup, run:

    $ java -jar target/zoocreeper-1.0-SNAPSHOT.jar --help

To see the available options for restoring a backup, run:

    $ java -cp target/zoocreeper-1.0-SNAPSHOT.jar \
        com.boundary.zoocreeper.Restore --help

The only required option is '-z'/'--zk-connect' which is a standard
ZooKeeper connection string.

Also included is a bash helper script:

    $ zoocreeper dump -z 127.0.0.1 > dumpfile.json
    $ cat dumpfile.json | zoocreeper load -z 127.0.0.1
